package com.eilai.runeterra.champion;

import com.eilai.runeterra.champion.MobXPConfig;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.*;

/**
 * Handles all League-style XP and ability unlock logic.
 *
 * XP curve approximates League of Legends' per-level thresholds.
 * Mob XP values are tuned so a typical play session levels naturally.
 */
public final class LeagueXPHelper {

    private LeagueXPHelper() {}

    // ── XP thresholds (XP needed to reach NEXT level) ────────────────────────
    // Index = current level (1-17). Level 18 is max, no further XP needed.
    private static final int[] XP_TO_NEXT = {
            /*1→2*/  280,
            /*2→3*/  380,
            /*3→4*/  480,
            /*4→5*/  580,
            /*5→6*/  700,
            /*6→7*/  820,
            /*7→8*/  940,
            /*8→9*/  1060,
            /*9→10*/ 1180,
            /*10→11*/1300,
            /*11→12*/1420,
            /*12→13*/1540,
            /*13→14*/1660,
            /*14→15*/1780,
            /*15→16*/1900,
            /*16→17*/2020,
            /*17→18*/2140
    };

    /**
     * Returns XP required to level up from the given level.
     * e.g. xpForLevel(1) = 280 (XP needed to go from 1 to 2)
     */
    public static int xpForLevel(int level) {
        if (level < 1 || level >= 18) return Integer.MAX_VALUE;
        return XP_TO_NEXT[level - 1];
    }

    // ── Mob XP rewards ────────────────────────────────────────────────────────

    /**
     * Returns the League XP reward for killing a given entity.
     * Values are loaded from config/runeterra/mob_xp.json —
     * edit that file to change XP rewards without recompiling.
     */
    public static int getMobXP(LivingEntity entity) {
        return MobXPConfig.getXP(entity);
    }

    // ── Old hardcoded values kept here for reference only ─────────────────────
    @SuppressWarnings("unused")
    private static int getMobXPLegacy(LivingEntity entity) {
        EntityType<?> type = entity.getType();

        // Passive mobs — small reward
        if (entity instanceof Animal)         return 15;

        // Common hostile mobs
        if (type == EntityType.ZOMBIE)        return 40;
        if (type == EntityType.SKELETON)      return 40;
        if (type == EntityType.CREEPER)       return 55;
        if (type == EntityType.SPIDER)        return 35;
        if (type == EntityType.CAVE_SPIDER)   return 35;
        if (type == EntityType.ENDERMAN)      return 80;
        if (type == EntityType.WITCH)         return 65;
        if (type == EntityType.DROWNED)       return 40;
        if (type == EntityType.HUSK)          return 40;
        if (type == EntityType.STRAY)         return 40;
        if (type == EntityType.PHANTOM)       return 50;
        if (type == EntityType.SLIME)         return 20;
        if (type == EntityType.MAGMA_CUBE)    return 25;

        // Nether mobs
        if (type == EntityType.BLAZE)         return 90;
        if (type == EntityType.GHAST)         return 95;
        if (type == EntityType.PIGLIN)        return 50;
        if (type == EntityType.PIGLIN_BRUTE)  return 100;
        if (type == EntityType.ZOMBIFIED_PIGLIN) return 55;
        if (type == EntityType.WITHER_SKELETON)  return 100;

        // End mobs
        if (type == EntityType.SHULKER)       return 80;

        // Mini-bosses / rare
        if (type == EntityType.ELDER_GUARDIAN) return 300;
        if (type == EntityType.RAVAGER)        return 250;
        if (type == EntityType.EVOKER)         return 200;
        if (type == EntityType.VINDICATOR)     return 80;

        // Bosses
        if (type == EntityType.WITHER)        return 1500;
        if (type == EntityType.ENDER_DRAGON)  return 3000;

        // Default fallback for any other LivingEntity
        return 20;
    }

    // ── Ability unlock rules (League of Legends) ─────────────────────────────

    /**
     * Can a basic ability (Q/W/E) be ranked up?
     *
     * League rules:
     *  - Total ranks in Q+W+E cannot exceed (level - rRank)
     *    (because each level gives exactly 1 skill point)
     *  - Each of Q/W/E is capped at 5
     *  - You must have an available skill point (checked in PlayerChampionData)
     *
     * This method checks only structural validity, not point availability.
     */
    public static boolean canRankBasicAbility(int level, int qRank, int wRank, int eRank) {
        // Must be at least level 1 to put a first point
        return level >= 1;
        // Point availability is enforced separately via availableSkillPoints()
    }

    /**
     * Can the ultimate (R) be ranked up?
     *
     * League rules: R can only be ranked at levels 6, 11, and 16.
     * rRank 0→1 requires level 6
     * rRank 1→2 requires level 11
     * rRank 2→3 requires level 16
     */
    public static boolean canRankUltimate(int level, int currentRRank) {
        return switch (currentRRank) {
            case 0 -> level >= 6;
            case 1 -> level >= 11;
            case 2 -> level >= 16;
            default -> false;
        };
    }

    /**
     * Returns how many unspent skill points a player has at the given level
     * with the given current ranks.
     */
    public static int availablePoints(int level, int qRank, int wRank, int eRank, int rRank) {
        return level - (qRank + wRank + eRank + rRank);
    }

    /**
     * Returns the cooldown reduction multiplier for a given ability rank (1-5).
     * Rough approximation — individual champion abilities will override this.
     */
    public static float cdMultiplier(int rank) {
        return 1.0f - (rank - 1) * 0.08f; // 8% CDR per rank
    }

    /**
     * Returns the damage multiplier for a given ability rank (1-5).
     */
    public static float dmgMultiplier(int rank) {
        return 1.0f + (rank - 1) * 0.20f; // 20% more damage per rank
    }
}