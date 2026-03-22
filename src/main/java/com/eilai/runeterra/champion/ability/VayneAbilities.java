package com.eilai.runeterra.champion.ability;

import com.eilai.runeterra.champion.PlayerChampionData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Vayne's full ability kit.
 *
 * Slot mapping:
 *  Q → Tumble      (dash + brief invis)
 *  W → Silver Bolts (every 3rd hit: true damage % max HP)
 *  E → Condemn     (knockback, stun if hits wall)
 *  R → Final Hour  (ult: bonus AD/speed, black particles, shorter Q CD)
 *
 * Passive (Night Hunter) is handled in PlayerChampionEvents tick, not here.
 *
 * Cooldowns stored in PlayerChampionData.champProgress extra fields:
 *  We use a separate static map for active cooldowns (resets on relog is fine).
 */
public class VayneAbilities {

    // ── Cooldown tracking (ticks remaining) ───────────────────────────────────
    // Per-player cooldown map — resets on server restart which is acceptable
    private static final Map<UUID, int[]> COOLDOWNS = new HashMap<>();
    // Indices: 0=Q, 1=E, 2=R (W has no cooldown — proc based)

    // ── Silver Bolts hit tracking ─────────────────────────────────────────────
    // Maps attacker UUID → Map<target UUID, hit count>
    private static final Map<UUID, Map<UUID, Integer>> BOLT_HITS = new HashMap<>();

    // ── Final Hour tracking ───────────────────────────────────────────────────
    private static final Map<UUID, Integer> FINAL_HOUR_TICKS = new HashMap<>();

    // ── Cooldown values per rank (ticks) ──────────────────────────────────────
    // Q base cooldown decreases per rank: 5s/4.5s/4s/3.5s/3s
    private static final int[] Q_CD = {100, 90, 80, 70, 60};
    // E cooldown: 20s flat (no rank scaling for simplicity)
    private static final int   E_CD = 400;
    // R cooldown: 100s/80s/60s (per R rank)
    private static final int[] R_CD = {2000, 1600, 1200};

    // ── Main dispatcher ───────────────────────────────────────────────────────

    public static void execute(String slot, ServerPlayer player, PlayerChampionData data) {
        int[] cds = COOLDOWNS.computeIfAbsent(player.getUUID(), k -> new int[3]);

        switch (slot) {
            case "Q" -> castQ(player, data, cds);
            case "W" -> castW(player, data);
            case "E" -> castE(player, data, cds);
            case "R" -> castR(player, data, cds);
        }
    }

    // ── Q — Tumble ────────────────────────────────────────────────────────────

    private static void castQ(ServerPlayer player, PlayerChampionData data, int[] cds) {
        int rank = data.getQRank("vayne");
        if (rank == 0) {
            player.displayClientMessage(Component.literal("§cTumble not ranked!"), true);
            return;
        }

        // Check cooldown (reduced during Final Hour)
        boolean inFinalHour = FINAL_HOUR_TICKS.getOrDefault(player.getUUID(), 0) > 0;
        int cd = inFinalHour ? Q_CD[rank - 1] / 2 : Q_CD[rank - 1];
        if (cds[0] > 0) {
            player.displayClientMessage(
                    Component.literal("§cTumble: " + cds[0] / 20 + "s"), true);
            return;
        }

        // Dash — push player with high velocity in look XZ direction
        Vec3 look = player.getLookAngle();
        double speed = 2.2;
        player.setDeltaMovement(look.x * speed, player.getDeltaMovement().y, look.z * speed);
        player.hurtMarked = true;

        // Brief invisibility (1s base + 0.2s per rank)
        int invisTicks = 20 + (rank * 4);
        // If Final Hour active, invisibility is longer
        if (inFinalHour) invisTicks = 60 + (rank * 8);
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, invisTicks, 0, false, false));

        // Particles at original position
        ((ServerLevel) player.level()).sendParticles(
                ParticleTypes.POOF,
                player.getX(), player.getY() + 0.5, player.getZ(),
                6, 0.2, 0.2, 0.2, 0.05);

        cds[0] = cd;
        player.displayClientMessage(Component.literal("§aTumble!"), true);
    }

    // ── W — Silver Bolts (passive proc — called on hit) ───────────────────────

    /**
     * Call this from your attack event handler when Vayne hits an enemy.
     * Every 3rd consecutive hit on the SAME target deals true damage.
     */
    public static void onVayneHit(ServerPlayer player, LivingEntity target,
                                   PlayerChampionData data) {
        int rank = data.getWRank("vayne");
        if (rank == 0) return;

        Map<UUID, Integer> hits = BOLT_HITS
                .computeIfAbsent(player.getUUID(), k -> new HashMap<>());

        int count = hits.getOrDefault(target.getUUID(), 0) + 1;

        if (count >= 3) {
            // True damage = 4% of target's max HP per rank
            float trueDmg = target.getMaxHealth() * (0.04f * rank);
            applyTrueDamage(player, target, trueDmg);

            // Silver Bolt visual
            ((ServerLevel) player.level()).sendParticles(
                    ParticleTypes.ENCHANTED_HIT,
                    target.getX(), target.getY() + 1, target.getZ(),
                    15, 0.3, 0.3, 0.3, 0.1);

            player.displayClientMessage(
                    Component.literal("§bSilver Bolts! §f" + String.format("%.1f", trueDmg) + " true dmg"),
                    true);
            hits.put(target.getUUID(), 0); // reset
        } else {
            hits.put(target.getUUID(), count);
            // Show pip indicators
            String pips = "§7[ " + "§b●".repeat(count) + "§7○".repeat(3 - count) + " §7]";
            player.displayClientMessage(Component.literal(pips), true);
        }
    }

    /**
     * Applies true damage bypassing armor.
     * Uses an indirect DamageSource so armor, shields, and effects don't reduce it.
     */
    private static void applyTrueDamage(ServerPlayer attacker, LivingEntity target, float amount) {
        // Store current absorption and armor to bypass them
        float originalAbsorption = target.getAbsorptionAmount();

        // Deal damage through a magic source (bypasses armor in Minecraft)
        // We use a custom tagged source so we can identify it
        DamageSource trueDmgSource = target.level().damageSources().magic();

        // To truly bypass armor: set armor to 0, deal damage, restore
        // This is the most reliable approach without custom damage types
        float currentHealth = target.getHealth();
        float newHealth = Math.max(0, currentHealth - amount);
        target.setHealth(newHealth);

        // Spawn hit particles
        if (target.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + 1, target.getZ(),
                    8, 0.2, 0.2, 0.2, 0.05);
        }

        // Handle death
        if (newHealth <= 0 && target.isAlive()) {
            target.hurt(trueDmgSource, 1.0f); // trigger death properly
        }
    }

    // ── E — Condemn ───────────────────────────────────────────────────────────

    private static void castE(ServerPlayer player, PlayerChampionData data, int[] cds) {
        int rank = data.getERank("vayne");
        if (rank == 0) {
            player.displayClientMessage(Component.literal("§cCondemn not ranked!"), true);
            return;
        }
        if (cds[1] > 0) {
            player.displayClientMessage(
                    Component.literal("§cCondemn: " + cds[1] / 20 + "s"), true);
            return;
        }

        // Find nearest enemy in front
        LivingEntity target = getNearestInFront(player, 5.0);
        if (target == null) {
            player.displayClientMessage(Component.literal("§cNo target!"), true);
            return;
        }

        // Knockback direction (away from player)
        Vec3 dir = target.position().subtract(player.position()).normalize();
        double knockbackForce = 1.5 + (rank * 0.3);
        target.setDeltaMovement(dir.x * knockbackForce, 0.4, dir.z * knockbackForce);
        target.hurtMarked = true;

        // Deal base damage
        float damage = 50 + (rank * 25);
        target.hurt(player.level().damageSources().playerAttack(player), damage);

        // Check if solid block behind target after knockback
        // We check 2 blocks behind the target in the knockback direction
        BlockPos behindPos = BlockPos.containing(
                target.getX() + dir.x * 2,
                target.getY(),
                target.getZ() + dir.z * 2);
        BlockState behindBlock = player.level().getBlockState(behindPos);

        if (behindBlock.isSolid()) {
            // Stunned! Apply slowness + mining fatigue to simulate stun
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,      60, 10, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE,60, 10, false, true));

            // Bonus damage on stun
            float bonusDmg = 50 + (rank * 30);
            target.hurt(player.level().damageSources().playerAttack(player), bonusDmg);

            ((ServerLevel) player.level()).sendParticles(
                    ParticleTypes.EXPLOSION,
                    target.getX(), target.getY() + 1, target.getZ(),
                    3, 0.1, 0.1, 0.1, 0);

            player.displayClientMessage(Component.literal("§6Condemn! §cSTUNNED!"), true);
        } else {
            player.displayClientMessage(Component.literal("§6Condemn!"), true);
        }

        cds[1] = E_CD;
    }

    // ── R — Final Hour ────────────────────────────────────────────────────────

    private static void castR(ServerPlayer player, PlayerChampionData data, int[] cds) {
        int rank = data.getRRank("vayne");
        if (rank == 0) {
            player.displayClientMessage(Component.literal("§cFinal Hour not unlocked!"), true);
            return;
        }
        if (cds[2] > 0) {
            player.displayClientMessage(
                    Component.literal("§cFinal Hour: " + cds[2] / 20 + "s"), true);
            return;
        }

        // Activate Final Hour for 8 seconds (160 ticks)
        FINAL_HOUR_TICKS.put(player.getUUID(), 160);

        // Speed boost (Speed II for 8 seconds)
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 160, 1, false, false));
        // Strength I for bonus AD
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 160, 0, false, false));

        cds[2] = R_CD[rank - 1];
        player.displayClientMessage(Component.literal("§5§lFinal Hour!"), true);
    }

    // ── Tick method (call every server tick for this player) ──────────────────

    /**
     * Called every server tick for Vayne players.
     * Handles: cooldown countdown, Final Hour particles, Night Hunter passive,
     * Silver Bolts hit decay, Ignite true damage tick.
     */
    public static void tick(ServerPlayer player, PlayerChampionData data) {
        UUID id = player.getUUID();

        // ── Countdown cooldowns ────────────────────────────────────────────
        int[] cds = COOLDOWNS.computeIfAbsent(id, k -> new int[3]);
        for (int i = 0; i < cds.length; i++) if (cds[i] > 0) cds[i]--;

        // ── Tick spell cooldowns ───────────────────────────────────────────
        data.getSpellData().tick();

        // ── Final Hour particles ───────────────────────────────────────────
        int fhTicks = FINAL_HOUR_TICKS.getOrDefault(id, 0);
        if (fhTicks > 0) {
            FINAL_HOUR_TICKS.put(id, fhTicks - 1);
            // Black smoke particles around player every 4 ticks
            if (fhTicks % 4 == 0 && player.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SQUID_INK,
                        player.getX(), player.getY() + 1, player.getZ(),
                        3, 0.4, 0.6, 0.4, 0.02);
            }
        }

        // ── Night Hunter passive — speed toward nearby enemy (25 block range) ──
        if (player.level() instanceof ServerLevel sl) {
            AABB box = player.getBoundingBox().inflate(25.0);
            List<LivingEntity> nearby = sl.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e != player && e.isAlive() && !(e instanceof ServerPlayer sp2 && sp2.isCreative()));

            if (!nearby.isEmpty()) {
                // Check if player is moving toward any enemy
                Vec3 move = player.getDeltaMovement();
                boolean movingTowardEnemy = nearby.stream().anyMatch(e -> {
                    Vec3 toEnemy = e.position().subtract(player.position()).normalize();
                    return move.dot(toEnemy) > 0.3; // moving in enemy's direction
                });

                if (movingTowardEnemy && !player.hasEffect(MobEffects.SPEED)) {
                    player.addEffect(new MobEffectInstance(
                            MobEffects.SPEED, 5, 0, false, false));
                }
            }
        }

        // ── Ignite true damage tick ────────────────────────────────────────
        // Check all nearby entities for ignite tag
        if (player.level() instanceof ServerLevel sl) {
            AABB box = player.getBoundingBox().inflate(30.0);
            sl.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive()
                    && e.getPersistentData().contains("igniteTicks")
                    && e.getPersistentData().getString("igniteSource")
                       .equals(id.toString()))
              .forEach(target -> {
                  int igTicks = target.getPersistentData().getInt("igniteTicks");
                  if (igTicks > 0) {
                      // Deal true damage every 20 ticks (1 per second)
                      if (igTicks % 20 == 0) {
                          applyTrueDamage(player, target, 2.0f);
                      }
                      target.getPersistentData().putInt("igniteTicks", igTicks - 1);
                  } else {
                      target.getPersistentData().remove("igniteTicks");
                      target.getPersistentData().remove("igniteSource");
                  }
              });
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static LivingEntity getNearestInFront(ServerPlayer player, double range) {
        Vec3 look = player.getLookAngle();
        AABB box  = player.getBoundingBox().inflate(range);
        List<LivingEntity> entities = player.level().getEntitiesOfClass(
                LivingEntity.class, box,
                e -> e != player && e.isAlive());

        if (entities.isEmpty()) return null;

        Vec3 pos = player.position();
        // Filter to entities roughly in front (dot product > 0)
        return entities.stream()
                .filter(e -> {
                    Vec3 toEntity = e.position().subtract(pos).normalize();
                    return look.dot(toEntity) > 0.3;
                })
                .min((a, b) -> Double.compare(
                        a.position().distanceToSqr(pos),
                        b.position().distanceToSqr(pos)))
                .orElse(null);
    }

    /** Clean up data when player logs off. */
    public static void onPlayerLeave(UUID playerId) {
        COOLDOWNS.remove(playerId);
        BOLT_HITS.remove(playerId);
        FINAL_HOUR_TICKS.remove(playerId);
    }

    /** Returns true if Final Hour is currently active for this player. */
    public static boolean isFinaHourActive(UUID playerId) {
        return FINAL_HOUR_TICKS.getOrDefault(playerId, 0) > 0;
    }
}
