package com.eilai.runeterra.champion.spell;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Stores a player's two chosen summoner spells (D and F slots)
 * and their individual cooldown timers.
 *
 * This is embedded inside PlayerChampionData — not a separate attachment.
 * Call serialize/deserialize from PlayerChampionData's write/read methods.
 *
 * Cooldowns are stored in ticks remaining (countdown to 0 = ready).
 */
public class PlayerSpellData {

    // Default spells on first join
    public static final SummonerSpell DEFAULT_D = SummonerSpell.FLASH;
    public static final SummonerSpell DEFAULT_F = SummonerSpell.IGNITE;

    private SummonerSpell spellD = DEFAULT_D;
    private SummonerSpell spellF = DEFAULT_F;

    private int cooldownD = 0; // ticks remaining
    private int cooldownF = 0;

    public PlayerSpellData() {}

    // ── Getters ───────────────────────────────────────────────────────────────

    public SummonerSpell getSpellD()   { return spellD; }
    public SummonerSpell getSpellF()   { return spellF; }
    public int           getCooldownD(){ return cooldownD; }
    public int           getCooldownF(){ return cooldownF; }

    public boolean isSpellDReady()  { return cooldownD <= 0; }
    public boolean isSpellFReady()  { return cooldownF <= 0; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setSpellD(SummonerSpell spell) { this.spellD = spell; this.cooldownD = 0; }
    public void setSpellF(SummonerSpell spell) { this.spellF = spell; this.cooldownF = 0; }

    // ── Use spells ────────────────────────────────────────────────────────────

    /** Attempt to use D spell. Returns false if on cooldown. */
    public boolean useSpellD() {
        if (cooldownD > 0) return false;
        cooldownD = spellD.getCooldownTicks();
        return true;
    }

    /** Attempt to use F spell. Returns false if on cooldown. */
    public boolean useSpellF() {
        if (cooldownF > 0) return false;
        cooldownF = spellF.getCooldownTicks();
        return true;
    }

    // ── Tick cooldowns ────────────────────────────────────────────────────────

    /** Call every server tick to count down cooldowns. */
    public void tick() {
        if (cooldownD > 0) cooldownD--;
        if (cooldownF > 0) cooldownF--;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public void serialize(ValueOutput out) {
        out.putString("SpellD",    spellD.name());
        out.putString("SpellF",    spellF.name());
        out.putInt("CooldownD",    cooldownD);
        out.putInt("CooldownF",    cooldownF);
    }

    public void deserialize(ValueInput in) {
        String dName = in.getStringOr("SpellD", DEFAULT_D.name());
        String fName = in.getStringOr("SpellF", DEFAULT_F.name());
        try { spellD = SummonerSpell.valueOf(dName); } catch (Exception e) { spellD = DEFAULT_D; }
        try { spellF = SummonerSpell.valueOf(fName); } catch (Exception e) { spellF = DEFAULT_F; }
        cooldownD = in.getIntOr("CooldownD", 0);
        cooldownF = in.getIntOr("CooldownF", 0);
    }
}
