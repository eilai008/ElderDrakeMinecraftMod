package com.eilai.runeterra.champion.ability;

import com.eilai.runeterra.champion.PlayerChampionData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Vayne's full ability kit.
 *
 * Slot mapping (Q/W/E/R = ability names, keys are different):
 *  Q key → Tumble
 *  E key → Silver Bolts (passive proc — no cast, W just shows pip info)
 *  R key → Condemn
 *  T key → Final Hour (ultimate)
 *
 * Passive (Night Hunter) ticks every server tick via tick().
 * Silver Bolts (W) procs on hit via onVayneHit() — not a castable ability.
 */
public class VayneAbilities {

    // ── Cooldown tracking (ticks remaining) ───────────────────────────────────
    // int[3]: 0=Q(Tumble), 1=E(Condemn), 2=R(Final Hour)
    private static final Map<UUID, int[]> COOLDOWNS       = new HashMap<>();
    private static final Map<UUID, Map<UUID, Integer>> BOLT_HITS   = new HashMap<>();
    private static final Map<UUID, Integer> FINAL_HOUR_TICKS        = new HashMap<>();

    // ── Cooldown values per rank (ticks) ──────────────────────────────────────
    private static final int[] Q_CD = {100, 90, 80, 70, 60}; // Tumble
    private static final int   E_CD = 400;                    // Condemn — flat
    private static final int[] R_CD = {2000, 1600, 1200};    // Final Hour

    // ── Main dispatcher ───────────────────────────────────────────────────────

    public static void execute(String slot, ServerPlayer player, PlayerChampionData data) {
        int[] cds = COOLDOWNS.computeIfAbsent(player.getUUID(), k -> new int[3]);
        switch (slot) {
            case "Q" -> castQ(player, data, cds);
            case "W" -> showBoltPips(player, data);   // W = Silver Bolts info (passive)
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
        boolean inFH = FINAL_HOUR_TICKS.getOrDefault(player.getUUID(), 0) > 0;
        int cd = inFH ? Q_CD[rank - 1] / 2 : Q_CD[rank - 1];

        if (cds[0] > 0) {
            player.displayClientMessage(
                    Component.literal("§cTumble: " + (cds[0] / 20) + "s"), true);
            return;
        }

        // Dash — high XZ velocity in look direction
        Vec3 look  = player.getLookAngle();
        double spd = 2.2;
        player.setDeltaMovement(look.x * spd, player.getDeltaMovement().y, look.z * spd);
        player.hurtMarked = true;

        // Invisibility: scales with rank, doubled during Final Hour
        int invisTicks = 20 + (rank * 4);
        if (inFH) invisTicks = 60 + (rank * 8);
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, invisTicks, 0, false, false));

        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.POOF,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    6, 0.2, 0.2, 0.2, 0.05);
        }

        cds[0] = cd;
        player.displayClientMessage(Component.literal("§aTumble!"), true);
    }

    // ── W — Silver Bolts (passive, shows pip info when W pressed) ────────────

    private static void showBoltPips(ServerPlayer player, PlayerChampionData data) {
        int rank = data.getWRank("vayne");
        if (rank == 0) {
            player.displayClientMessage(
                    Component.literal("§cSilver Bolts not ranked!"), true);
            return;
        }
        Map<UUID, Integer> hits = BOLT_HITS
                .computeIfAbsent(player.getUUID(), k -> new HashMap<>());
        // Show current bolt stacks on nearest target
        player.displayClientMessage(
                Component.literal("§bSilver Bolts §7— Rank §f" + rank
                        + " §7| True dmg: §f" + String.format("%.0f%%", rank * 4.0) + " §7max HP"), true);
    }

    // ── W proc — called on hit ────────────────────────────────────────────────

    /**
     * Call this from PlayerChampionEvents when Vayne hits an enemy.
     * Every 3rd consecutive hit on the same target deals true damage.
     */
    public static void onVayneHit(ServerPlayer player, LivingEntity target,
                                  PlayerChampionData data) {
        int rank = data.getWRank("vayne");
        if (rank == 0) return;

        Map<UUID, Integer> hits = BOLT_HITS
                .computeIfAbsent(player.getUUID(), k -> new HashMap<>());
        int count = hits.getOrDefault(target.getUUID(), 0) + 1;

        if (count >= 3) {
            float trueDmg = target.getMaxHealth() * (0.04f * rank);
            applyTrueDamage(player, target, trueDmg);

            if (player.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT,
                        target.getX(), target.getY() + 1, target.getZ(),
                        15, 0.3, 0.3, 0.3, 0.1);
            }

            player.displayClientMessage(
                    Component.literal("§bSilver Bolts! §f"
                            + String.format("%.1f", trueDmg) + " §7true dmg"), true);
            hits.put(target.getUUID(), 0);
        } else {
            hits.put(target.getUUID(), count);
            String pips = "§7[ " + "§b●".repeat(count)
                    + "§7○".repeat(3 - count) + " §7]";
            player.displayClientMessage(Component.literal(pips), true);
        }
    }

    // ── True damage (bypasses armor by setting HP directly) ───────────────────

    private static void applyTrueDamage(ServerPlayer attacker,
                                        LivingEntity target, float amount) {
        float newHp = Math.max(0f, target.getHealth() - amount);
        target.setHealth(newHp);

        if (target.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + 1, target.getZ(),
                    8, 0.2, 0.2, 0.2, 0.05);
        }

        if (newHp <= 0f && target.isAlive()) {
            target.hurt(target.level().damageSources().magic(), 1.0f);
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
                    Component.literal("§cCondemn: " + (cds[1] / 20) + "s"), true);
            return;
        }

        LivingEntity target = getLookAtTarget(player, 7.0);
        if (target == null) {
            player.displayClientMessage(Component.literal("§cNo target in range!"), true);
            return;
        }

        Vec3 dir = target.position().subtract(player.position()).normalize();
        double force = 1.5 + (rank * 0.3);
        target.setDeltaMovement(dir.x * force, 0.4, dir.z * force);
        target.hurtMarked = true;

        float damage = 6 + (rank * 2f);
        target.hurt(player.level().damageSources().playerAttack(player), damage);

        // Check for wall stun (solid block 2 units behind target in knockback dir)
        BlockPos behindPos = BlockPos.containing(
                target.getX() + dir.x * 2,
                target.getY(),
                target.getZ() + dir.z * 2);
        BlockState behindBlock = player.level().getBlockState(behindPos);

        if (behindBlock.isSolid()) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,      60, 10, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE,60, 10, false, true));
            target.hurt(player.level().damageSources().playerAttack(player), 8 + (rank * 2f));

            if (player.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.EXPLOSION,
                        target.getX(), target.getY() + 1, target.getZ(),
                        3, 0.1, 0.1, 0.1, 0);
            }
            player.displayClientMessage(Component.literal("§6Condemn! §c§lSTUNNED!"), true);
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
                    Component.literal("§cFinal Hour: " + (cds[2] / 20) + "s"), true);
            return;
        }

        FINAL_HOUR_TICKS.put(player.getUUID(), 160); // 8 seconds
        player.addEffect(new MobEffectInstance(MobEffects.SPEED,    160, 1, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 160, 0, false, false));
        cds[2] = R_CD[rank - 1];

        player.displayClientMessage(Component.literal("§5§lFinal Hour!"), true);
    }

    // ── Server tick ───────────────────────────────────────────────────────────

    public static void tick(ServerPlayer player, PlayerChampionData data) {
        UUID id = player.getUUID();

        // Cooldowns
        int[] cds = COOLDOWNS.computeIfAbsent(id, k -> new int[3]);
        for (int i = 0; i < cds.length; i++) if (cds[i] > 0) cds[i]--;

        // Spell cooldowns
        data.getSpellData().tick();

        // Final Hour black particles
        int fh = FINAL_HOUR_TICKS.getOrDefault(id, 0);
        if (fh > 0) {
            FINAL_HOUR_TICKS.put(id, fh - 1);
            if (fh % 4 == 0 && player.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SQUID_INK,
                        player.getX(), player.getY() + 1, player.getZ(),
                        3, 0.4, 0.6, 0.4, 0.02);
            }
        }

        // Night Hunter passive — speed boost when moving toward enemy (25 block range)
        if (player.level() instanceof ServerLevel sl) {
            AABB box = player.getBoundingBox().inflate(25.0);
            List<LivingEntity> nearby = sl.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e != player && e.isAlive()
                            && !(e instanceof ServerPlayer sp2 && sp2.isCreative()));

            if (!nearby.isEmpty()) {
                // FIX 1: use look angle instead of deltaMovement — deltaMovement is
                // near-zero on the ground due to friction, so the passive never fired.
                Vec3 look = player.getLookAngle();
                boolean facingEnemy = nearby.stream().anyMatch(e -> {
                    Vec3 toEnemy = e.position().subtract(player.position()).normalize();
                    return look.dot(toEnemy) > 0.3;
                });
                // FIX 2: always apply/refresh — don't gate on !hasEffect(SPEED)
                // so Final Hour and the passive don't conflict, and the buff
                // stays active continuously while the condition is met.
                if (facingEnemy) {
                    player.addEffect(new MobEffectInstance(
                            MobEffects.SPEED, 10, 0, false, false));
                }
            }
        }

        // Ignite true damage tick
        // 1.21.11: CompoundTag.getInt() returns Optional<Integer> — use orElse(0)
        if (player.level() instanceof ServerLevel sl) {
            AABB box = player.getBoundingBox().inflate(30.0);
            sl.getEntitiesOfClass(LivingEntity.class, box,
                            e -> e.isAlive()
                                    && e.getPersistentData().contains("igniteTicks")
                                    && id.toString().equals(
                                    e.getPersistentData().getString("igniteSource").orElse("")))
                    .forEach(target -> {
                        int igTicks = target.getPersistentData()
                                .getInt("igniteTicks").orElse(0);
                        if (igTicks > 0) {
                            if (igTicks % 20 == 0) applyTrueDamage(player, target, 2.0f);
                            target.getPersistentData().putInt("igniteTicks", igTicks - 1);
                        } else {
                            target.getPersistentData().remove("igniteTicks");
                            target.getPersistentData().remove("igniteSource");
                        }
                    });
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns the entity the player is directly looking at within range.
     * Uses ray-casting along the look vector.
     */
    private static LivingEntity getLookAtTarget(ServerPlayer player, double range) {
        Vec3 eyePos  = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos  = eyePos.add(lookVec.x * range, lookVec.y * range, lookVec.z * range);

        AABB searchBox = player.getBoundingBox().inflate(range);
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(
                LivingEntity.class, searchBox,
                e -> e != player && e.isAlive());

        LivingEntity best  = null;
        double bestDist    = Double.MAX_VALUE;
        for (LivingEntity e : candidates) {
            // Check if entity's bounding box intersects the look ray
            var hit = e.getBoundingBox().clip(eyePos, endPos);
            if (hit.isPresent()) {
                double d = eyePos.distanceToSqr(hit.get());
                if (d < bestDist) { bestDist = d; best = e; }
            }
        }
        return best;
    }

    private static LivingEntity getNearestInFront(ServerPlayer player, double range) {
        Vec3 look = player.getLookAngle();
        AABB box  = player.getBoundingBox().inflate(range);
        return player.level().getEntitiesOfClass(LivingEntity.class, box,
                        e -> e != player && e.isAlive())
                .stream()
                .filter(e -> {
                    Vec3 toE = e.position().subtract(player.position()).normalize();
                    return look.dot(toE) > 0.3;
                })
                .min((a, b) -> Double.compare(
                        a.position().distanceToSqr(player.position()),
                        b.position().distanceToSqr(player.position())))
                .orElse(null);
    }

    public static void onPlayerLeave(UUID id) {
        COOLDOWNS.remove(id);
        BOLT_HITS.remove(id);
        FINAL_HOUR_TICKS.remove(id);
    }

    public static boolean isFinalHourActive(UUID id) {
        return FINAL_HOUR_TICKS.getOrDefault(id, 0) > 0;
    }
}