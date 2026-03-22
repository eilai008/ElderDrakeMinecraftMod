package com.eilai.runeterra.champion;

/**
 * Immutable data descriptor for a single champion.
 *
 * Ability slots map to Minecraft keys as follows:
 *   Passive → passive ability
 *   Q (E key in Minecraft)
 *   W (R key in Minecraft)
 *   E (T key in Minecraft)
 *   R (F key in Minecraft) — Ultimate
 *   D (C key in Minecraft) — Summoner slot 1
 *   F               — Summoner slot 2
 *
 * League skill ranks: Q/W/E max 5, R max 3 (unlockable at lvl 6/11/16)
 */
public record ChampionDefinition(
        /** Internal unique id, e.g. "ahri", "jinx", "no_champion" */
        String id,

        /** Display name shown in champion select */
        String displayName,

        /** Short lore line shown under the name */
        String title,

        /** Status — AVAILABLE or UNDER_CONSTRUCTION */
        ChampionStatus status,

        /**
         * Path to the champion splash texture, relative to textures/champion/splash/
         * e.g. "ahri.png" → textures/champion/splash/ahri.png
         */
        String splashTexture,

        /** Passive description */
        String passive,

        /** Q ability name + description  (key: E in Minecraft) */
        String abilityQ,

        /** W ability name + description  (key: R in Minecraft) */
        String abilityW,

        /** E ability name + description  (key: T in Minecraft) */
        String abilityE,

        /** R ability name + description  (key: F in Minecraft — ultimate) */
        String abilityR,

        /** D summoner slot description   (key: C in Minecraft) */
        String abilityD,

        /** F summoner slot description   (second summoner key) */
        String abilityF,

        /**
         * The weapon item registry name for this champion, e.g. "garen_weapon".
         * Used by WeaponRegistry to look up the correct weapon.
         * Null / empty means no weapon defined yet.
         */
        String weaponItemId
) {

    // ── Convenience helpers ───────────────────────────────────────────────────

    public boolean isAvailable()         { return status == ChampionStatus.AVAILABLE; }
    public boolean isUnderConstruction() { return status == ChampionStatus.UNDER_CONSTRUCTION; }
    public boolean hasWeapon()           { return weaponItemId != null && !weaponItemId.isEmpty(); }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder(String id, String displayName, String title) {
        return new Builder(id, displayName, title);
    }

    public static final class Builder {
        private final String id, displayName, title;
        private ChampionStatus status = ChampionStatus.UNDER_CONSTRUCTION;
        private String splashTexture  = "placeholder.png";
        private String passive = "?", abilityQ = "?", abilityW = "?",
                       abilityE = "?", abilityR = "?", abilityD = "?", abilityF = "?";
        private String weaponItemId   = "";

        private Builder(String id, String displayName, String title) {
            this.id = id; this.displayName = displayName; this.title = title;
        }

        public Builder available()              { this.status = ChampionStatus.AVAILABLE;  return this; }
        public Builder splash(String tex)       { this.splashTexture = tex;                return this; }
        public Builder passive(String d)        { this.passive = d;                        return this; }
        public Builder q(String d)              { this.abilityQ = d;                       return this; }
        public Builder w(String d)              { this.abilityW = d;                       return this; }
        public Builder e(String d)              { this.abilityE = d;                       return this; }
        public Builder r(String d)              { this.abilityR = d;                       return this; }
        public Builder d(String d)              { this.abilityD = d;                       return this; }
        public Builder f(String d)              { this.abilityF = d;                       return this; }
        public Builder weapon(String itemId)    { this.weaponItemId = itemId;              return this; }

        public ChampionDefinition build() {
            return new ChampionDefinition(id, displayName, title, status, splashTexture,
                    passive, abilityQ, abilityW, abilityE, abilityR, abilityD, abilityF,
                    weaponItemId);
        }
    }
}
