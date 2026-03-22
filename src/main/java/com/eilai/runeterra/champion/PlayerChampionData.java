package com.eilai.runeterra.champion;

import com.eilai.runeterra.champion.spell.PlayerSpellData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Per-player champion data stored via NeoForge data attachments.
 *
 * ValueIOSerializable correct API (from NeoForge docs):
 *  - Method names are serialize(ValueOutput) and deserialize(ValueInput)
 *  - Nested objects: output.child("key") / input.childOrEmpty("key")
 *  - Reads: input.getStringOr / getBooleanOr / getLongOr / getIntOr
 *  - Writes: output.putString / putBoolean / putLong / putInt
 *  - Lists of children: not directly supported — use indexed child keys instead
 */
public class PlayerChampionData implements ValueIOSerializable {

    // ── Attachment registration ────────────────────────────────────────────────

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "runeterra");

    public static final Supplier<AttachmentType<PlayerChampionData>> CHAMPION_DATA =
            ATTACHMENT_TYPES.register("champion_data",
                    () -> AttachmentType.serializable(PlayerChampionData::new).build());

    // ── Constants ─────────────────────────────────────────────────────────────

    public static final long OUT_OF_COMBAT_TICKS = 20L * 60 * 5;
    public static final long SWITCH_COOLDOWN_MS  = 1000L * 60 * 60;

    // ── Fields ────────────────────────────────────────────────────────────────

    private String  championId      = "no_champion";
    private boolean hasSelectedOnce = false;
    private long    lastCombatTick  = -1L;
    private long    lastSwitchMs    = 0L;

    /** int[6] = [level, xp, qRank, wRank, eRank, rRank] */
    private final Map<String, int[]> champProgress = new HashMap<>();

    private final PlayerSpellData spellData = new PlayerSpellData();

    public PlayerChampionData() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String  getChampionId()           { return championId; }
    public boolean hasSelectedOnce()         { return hasSelectedOnce; }
    public long    getLastCombatTick()       { return lastCombatTick; }
    public long    getLastSwitchMs()         { return lastSwitchMs; }
    public void    markFirstSelection()      { this.hasSelectedOnce = true; }
    public void    setLastCombatTick(long t) { this.lastCombatTick = t; }

    // Weapon slot selection (toggled by pressing 0)
    private boolean weaponSlotSelected = false;
    public boolean isWeaponSlotSelected()    { return weaponSlotSelected; }
    public void toggleWeaponSlot()           { weaponSlotSelected = !weaponSlotSelected; }
    public void setWeaponSlotSelected(boolean v) { weaponSlotSelected = v; }
    public PlayerSpellData getSpellData()       { return spellData; }

    // ── Champion selection ────────────────────────────────────────────────────

    /** 0 = success, 1 = in combat, 2 = on cooldown */
    public int trySetChampion(String newId, long currentGameTick) {
        if (!hasSelectedOnce) { forceSetChampion(newId); return 0; }
        long now = System.currentTimeMillis();
        if (lastCombatTick >= 0 && (currentGameTick - lastCombatTick) < OUT_OF_COMBAT_TICKS) return 1;
        if ((now - lastSwitchMs) < SWITCH_COOLDOWN_MS) return 2;
        forceSetChampion(newId);
        this.lastSwitchMs = now;
        return 0;
    }

    public void forceSetChampion(String newId) {
        this.championId = newId;
        champProgress.computeIfAbsent(newId, k -> new int[]{1, 0, 0, 0, 0, 0});
        markFirstSelection();
    }

    // ── Per-champion progress ─────────────────────────────────────────────────

    private int[] getProgress(String id) {
        return champProgress.computeIfAbsent(id, k -> new int[]{1, 0, 0, 0, 0, 0});
    }

    public int getLevel(String id) { return getProgress(id)[0]; }
    public int getXP(String id)    { return getProgress(id)[1]; }
    public int getQRank(String id) { return getProgress(id)[2]; }
    public int getWRank(String id) { return getProgress(id)[3]; }
    public int getERank(String id) { return getProgress(id)[4]; }
    public int getRRank(String id) { return getProgress(id)[5]; }

    public int getCurrentLevel() { return getLevel(championId); }

    /**
     * Copies all data from another PlayerChampionData instance.
     * Called after death to preserve champion progress on the new player entity.
     */
    public void copyFrom(PlayerChampionData other) {
        this.championId      = other.championId;
        this.hasSelectedOnce = other.hasSelectedOnce;
        this.lastCombatTick  = other.lastCombatTick;
        this.lastSwitchMs    = other.lastSwitchMs;
        this.champProgress.clear();
        other.champProgress.forEach((k, v) ->
                this.champProgress.put(k, v.clone()));
        // Copy spell selections (reset cooldowns on death)
        this.spellData.setSpellD(other.spellData.getSpellD());
        this.spellData.setSpellF(other.spellData.getSpellF());
    }

    /**
     * Called on the CLIENT when a SyncChampionDataPacket arrives.
     * Directly writes server-authoritative values into the progress map.
     */
    public void syncFromServer(String id, int level, int xp,
                               int qRank, int wRank, int eRank, int rRank) {
        int[] p = champProgress.computeIfAbsent(id, k -> new int[]{1, 0, 0, 0, 0, 0});
        p[0] = level;
        p[1] = xp;
        p[2] = qRank;
        p[3] = wRank;
        p[4] = eRank;
        p[5] = rRank;
    }

    /**
     * Adds XP and handles multiple level-ups in one call.
     * Returns the number of levels gained (0 if none).
     */
    public int addXP(int amount) {
        int[] p = getProgress(championId);
        if (p[0] >= 18) return 0;
        p[1] += amount;
        int levelsGained = 0;
        // Loop so a large XP gain can trigger multiple level-ups
        while (p[0] < 18) {
            int threshold = LeagueXPHelper.xpForLevel(p[0]);
            if (p[1] >= threshold) {
                p[1] -= threshold;
                p[0]++;
                levelsGained++;
            } else {
                break;
            }
        }
        return levelsGained;
    }

    public boolean rankUpAbility(int abilityIndex) {
        int[] p = getProgress(championId);
        int level = p[0], q = p[2], w = p[3], e = p[4], r = p[5];
        return switch (abilityIndex) {
            case 0 -> { if (q >= 5 || !LeagueXPHelper.canRankBasicAbility(level, q, w, e)) yield false; p[2]++; yield true; }
            case 1 -> { if (w >= 5 || !LeagueXPHelper.canRankBasicAbility(level, q, w, e)) yield false; p[3]++; yield true; }
            case 2 -> { if (e >= 5 || !LeagueXPHelper.canRankBasicAbility(level, q, w, e)) yield false; p[4]++; yield true; }
            case 3 -> { if (r >= 3 || !LeagueXPHelper.canRankUltimate(level, r))           yield false; p[5]++; yield true; }
            default -> false;
        };
    }

    public int availableSkillPoints() {
        int[] p = getProgress(championId);
        return p[0] - (p[2] + p[3] + p[4] + p[5]);
    }

    // ── ValueIOSerializable ───────────────────────────────────────────────────
    // Correct method names: serialize(ValueOutput) and deserialize(ValueInput)
    // Nested objects via output.child("key") / input.childOrEmpty("key")
    // Lists: use indexed keys (e.g. "prog_0", "prog_1", "progCount")

    @Override
    public void serialize(ValueOutput out) {
        out.putString("ChampionId",   championId);
        out.putBoolean("HasSelected", hasSelectedOnce);
        out.putLong("LastCombatTick", lastCombatTick);
        out.putLong("LastSwitchMs",   lastSwitchMs);

        // Store progress map as indexed children
        int i = 0;
        for (Map.Entry<String, int[]> entry : champProgress.entrySet()) {
            int[] d = entry.getValue();
            ValueOutput c = out.child("prog_" + i);
            c.putString("id",    entry.getKey());
            c.putInt("level",    d[0]);
            c.putInt("xp",       d[1]);
            c.putInt("qRank",    d[2]);
            c.putInt("wRank",    d[3]);
            c.putInt("eRank",    d[4]);
            c.putInt("rRank",    d[5]);
            i++;
        }
        out.putInt("progCount", i);
    }

    @Override
    public void deserialize(ValueInput in) {
        championId      = in.getStringOr("ChampionId",   "no_champion");
        hasSelectedOnce = in.getBooleanOr("HasSelected", false);
        lastCombatTick  = in.getLongOr("LastCombatTick", -1L);
        lastSwitchMs    = in.getLongOr("LastSwitchMs",   0L);

        champProgress.clear();
        int count = in.getIntOr("progCount", 0);
        for (int i = 0; i < count; i++) {
            ValueInput c = in.childOrEmpty("prog_" + i);
            String key = c.getStringOr("id", "");
            if (!key.isEmpty()) {
                champProgress.put(key, new int[]{
                        c.getIntOr("level", 1),
                        c.getIntOr("xp",    0),
                        c.getIntOr("qRank", 0),
                        c.getIntOr("wRank", 0),
                        c.getIntOr("eRank", 0),
                        c.getIntOr("rRank", 0)
                });
            }
        }
        spellData.deserialize(in.childOrEmpty("Spells"));
    }

    // ── Static helper ─────────────────────────────────────────────────────────

    public static PlayerChampionData get(Player player) {
        return player.getData(CHAMPION_DATA.get());
    }
}