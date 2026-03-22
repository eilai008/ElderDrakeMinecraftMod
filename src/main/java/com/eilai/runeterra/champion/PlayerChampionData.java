package com.eilai.runeterra.champion;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Per-player data saved via NeoForge PlayerData attachments.
 *
 * Stores:
 *  - chosen champion id
 *  - whether the player has seen the first-time select screen
 *  - last combat timestamp (game time ticks)
 *  - last champion switch timestamp (real epoch ms)
 *  - per-champion: level (1-18), XP, and ability ranks [Q,W,E,R] (0-5/3)
 */
public class PlayerChampionData {

    // ── NeoForge Attachment registration ──────────────────────────────────────

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "runeterra");

    public static final Supplier<AttachmentType<PlayerChampionData>> CHAMPION_DATA =
            ATTACHMENT_TYPES.register("champion_data",
                    () -> AttachmentType.serializable(PlayerChampionData::new)
                            .build());

    // ── Constants ─────────────────────────────────────────────────────────────

    /** 5 minutes in ticks (20 ticks/sec × 60 sec × 5) */
    public static final long OUT_OF_COMBAT_TICKS = 20L * 60 * 5;

    /** 1 hour in milliseconds */
    public static final long SWITCH_COOLDOWN_MS  = 1000L * 60 * 60;

    // ── Fields ────────────────────────────────────────────────────────────────

    private String  championId        = "no_champion";
    private boolean hasSelectedOnce   = false;
    private long    lastCombatTick    = -1L;   // server game time
    private long    lastSwitchMs      = 0L;    // System.currentTimeMillis()

    /**
     * Per-champion progress.
     * Key: champion id
     * Value: int[5] → [level, xp, qRank, wRank, eRank, rRank]
     *                   idx 0   1    2      3      4      5
     */
    private final Map<String, int[]> champProgress = new HashMap<>();

    // ── Constructor ───────────────────────────────────────────────────────────

    public PlayerChampionData() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String  getChampionId()      { return championId; }
    public boolean hasSelectedOnce()    { return hasSelectedOnce; }
    public long    getLastCombatTick()  { return lastCombatTick; }
    public long    getLastSwitchMs()    { return lastSwitchMs; }

    public void markFirstSelection()    { this.hasSelectedOnce = true; }
    public void setLastCombatTick(long tick) { this.lastCombatTick = tick; }

    // ── Champion selection ────────────────────────────────────────────────────

    /**
     * Attempts to switch champions. Returns a result code:
     *   0 = success
     *   1 = still in combat (within last 5 min)
     *   2 = switch on cooldown (within last hour)
     *
     * First-time selection bypasses both checks.
     */
    public int trySetChampion(String newId, long currentGameTick) {
        if (!hasSelectedOnce) {
            forceSetChampion(newId);
            return 0;
        }

        long now = System.currentTimeMillis();

        // Combat check
        if (lastCombatTick >= 0 && (currentGameTick - lastCombatTick) < OUT_OF_COMBAT_TICKS) {
            return 1;
        }

        // Cooldown check
        if ((now - lastSwitchMs) < SWITCH_COOLDOWN_MS) {
            return 2;
        }

        forceSetChampion(newId);
        this.lastSwitchMs = now;
        return 0;
    }

    /** Sets champion without any checks — used internally and for admin commands. */
    public void forceSetChampion(String newId) {
        this.championId = newId;
        // Ensure progress entry exists for this champion
        champProgress.computeIfAbsent(newId, k -> new int[]{1, 0, 0, 0, 0, 0});
        markFirstSelection();
    }

    // ── Per-champion progress ─────────────────────────────────────────────────

    private int[] getProgress(String id) {
        return champProgress.computeIfAbsent(id, k -> new int[]{1, 0, 0, 0, 0, 0});
    }

    public int getLevel(String champId)  { return getProgress(champId)[0]; }
    public int getXP(String champId)     { return getProgress(champId)[1]; }
    public int getQRank(String champId)  { return getProgress(champId)[2]; }
    public int getWRank(String champId)  { return getProgress(champId)[3]; }
    public int getERank(String champId)  { return getProgress(champId)[4]; }
    public int getRRank(String champId)  { return getProgress(champId)[5]; }

    public int getCurrentLevel()  { return getLevel(championId); }
    public int getCurrentXP()     { return getXP(championId); }

    /** Adds XP and levels up if threshold reached. Returns true if levelled up. */
    public boolean addXP(int amount) {
        int[] p = getProgress(championId);
        if (p[0] >= 18) return false; // already max level

        p[1] += amount;
        int threshold = LeagueXPHelper.xpForLevel(p[0]);

        if (p[1] >= threshold) {
            p[1] -= threshold;
            p[0] = Math.min(p[0] + 1, 18);
            return true;
        }
        return false;
    }

    /**
     * Attempts to rank up an ability.
     * Returns false if not allowed by League rules or level cap.
     * abilityIndex: 0=Q, 1=W, 2=E, 3=R
     */
    public boolean rankUpAbility(int abilityIndex) {
        int[] p = getProgress(championId);
        int level = p[0];
        int qRank = p[2], wRank = p[3], eRank = p[4], rRank = p[5];

        return switch (abilityIndex) {
            case 0 -> { // Q — max 5
                if (qRank >= 5) yield false;
                if (!LeagueXPHelper.canRankBasicAbility(level, qRank, wRank, eRank)) yield false;
                p[2]++;
                yield true;
            }
            case 1 -> { // W — max 5
                if (wRank >= 5) yield false;
                if (!LeagueXPHelper.canRankBasicAbility(level, qRank, wRank, eRank)) yield false;
                p[3]++;
                yield true;
            }
            case 2 -> { // E — max 5
                if (eRank >= 5) yield false;
                if (!LeagueXPHelper.canRankBasicAbility(level, qRank, wRank, eRank)) yield false;
                p[4]++;
                yield true;
            }
            case 3 -> { // R — max 3, only at 6/11/16
                if (rRank >= 3) yield false;
                if (!LeagueXPHelper.canRankUltimate(level, rRank)) yield false;
                p[5]++;
                yield true;
            }
            default -> false;
        };
    }

    /** Returns how many skill points are available to spend at current level. */
    public int availableSkillPoints() {
        int[] p = getProgress(championId);
        int totalSpent = p[2] + p[3] + p[4] + p[5];
        // In League you get 1 point per level, starting at level 1 you can put your first point.
        return (p[0]) - totalSpent;
    }

    // ── NBT Serialization ─────────────────────────────────────────────────────

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("ChampionId",     championId);
        tag.putBoolean("HasSelected",   hasSelectedOnce);
        tag.putLong("LastCombatTick",   lastCombatTick);
        tag.putLong("LastSwitchMs",     lastSwitchMs);

        CompoundTag progressTag = new CompoundTag();
        for (Map.Entry<String, int[]> entry : champProgress.entrySet()) {
            CompoundTag c = new CompoundTag();
            int[] d = entry.getValue();
            c.putInt("level",  d[0]);
            c.putInt("xp",     d[1]);
            c.putInt("qRank",  d[2]);
            c.putInt("wRank",  d[3]);
            c.putInt("eRank",  d[4]);
            c.putInt("rRank",  d[5]);
            progressTag.put(entry.getKey(), c);
        }
        tag.put("Progress", progressTag);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        championId      = tag.getString("ChampionId").orElse("no_champion");
        hasSelectedOnce = tag.getBoolean("HasSelected").orElse(false);
        lastCombatTick  = tag.getLong("LastCombatTick").orElse(-1L);
        lastSwitchMs    = tag.getLong("LastSwitchMs").orElse(0L);

        champProgress.clear();
        CompoundTag progressTag = tag.getCompound("Progress").orElseGet(CompoundTag::new);
        for (String key : progressTag.keySet()) {
            CompoundTag c = progressTag.getCompound(key).orElseGet(CompoundTag::new);
            champProgress.put(key, new int[]{
                    c.getInt("level").orElse(1),
                    c.getInt("xp").orElse(0),
                    c.getInt("qRank").orElse(0),
                    c.getInt("wRank").orElse(0),
                    c.getInt("eRank").orElse(0),
                    c.getInt("rRank").orElse(0)
            });
        }
    }

    // ── Static helper: get data from player ──────────────────────────────────

    public static PlayerChampionData get(Player player) {
        return player.getData(CHAMPION_DATA.get());
    }
}
