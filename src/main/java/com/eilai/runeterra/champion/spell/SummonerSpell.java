package com.eilai.runeterra.champion.spell;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * All available summoner spells.
 * Each spell defines its cooldown (in ticks) and its execute() logic.
 *
 * Keybind mapping:
 *   D slot → F key in Minecraft
 *   F slot → C key in Minecraft
 */
public enum SummonerSpell {

    FLASH("Flash", "Teleport a short distance in the direction you are moving.", 300 * 20) {
        @Override
        public void execute(ServerPlayer player) {
            Vec3 look  = player.getLookAngle();
            double dist = 8.0;
            Vec3 dest  = player.position().add(look.x * dist, 0, look.z * dist);

            // Check destination is safe (not inside a block)
            if (player.level().noCollision(player,
                    player.getBoundingBox().move(look.x * dist, 0, look.z * dist))) {
                player.teleportTo(dest.x, dest.y, dest.z);
                ((ServerLevel) player.level()).sendParticles(
                        ParticleTypes.PORTAL,
                        dest.x, dest.y + 1, dest.z,
                        20, 0.3, 0.5, 0.3, 0.1);
            } else {
                player.displayClientMessage(
                        Component.literal("§cFlash blocked!"), true);
            }
        }
    },

    IGNITE("Ignite", "Set a nearby enemy on fire, dealing true damage over 5 seconds.", 210 * 20) {
        @Override
        public void execute(ServerPlayer player) {
            LivingEntity target = getLookAtTarget(player, 7.0);
            if (target == null) {
                player.displayClientMessage(
                        Component.literal("§cNo target in range!"), true);
                return;
            }
            // Set on fire for 5 seconds
            target.setRemainingFireTicks(100); // 5 seconds = 100 ticks
            // Apply true damage over time via a custom damage tick
            // We use a simple approach: deal 5 instances of 2 damage bypassing armor
            // spread across the burning duration via a tag on the entity
            target.getPersistentData().putInt("igniteTicks", 100); // 5 seconds
            target.getPersistentData().putString("igniteSource", player.getUUID().toString());

            player.displayClientMessage(
                    Component.literal("§c🔥 Ignite!"), true);
        }
    },

    GHOST("Ghost", "Gain a massive speed boost for 10 seconds.", 210 * 20) {
        @Override
        public void execute(ServerPlayer player) {
            // Speed III for 10 seconds
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 2, false, true));
            player.displayClientMessage(
                    Component.literal("§a💨 Ghost!"), true);
        }
    },

    HEAL("Heal", "Restore 40% of your max health.", 240 * 20) {
        @Override
        public void execute(ServerPlayer player) {
            float maxHp  = player.getMaxHealth();
            float amount = maxHp * 0.40f;
            player.heal(amount);
            ((ServerLevel) player.level()).sendParticles(
                    ParticleTypes.HEART,
                    player.getX(), player.getY() + 1, player.getZ(),
                    8, 0.3, 0.3, 0.3, 0.05);
            player.displayClientMessage(
                    Component.literal("§a❤ Heal!"), true);
        }
    },

    BARRIER("Barrier", "Gain a temporary absorption shield for 2 seconds.", 180 * 20) {
        @Override
        public void execute(ServerPlayer player) {
            // Absorption IV for 2 seconds
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 40, 3, false, true));
            player.displayClientMessage(
                    Component.literal("§b🛡 Barrier!"), true);
        }
    },

    EXHAUST("Exhaust", "Slow a nearby enemy and reduce their damage for 3 seconds.", 210 * 20) {
        @Override
        public void execute(ServerPlayer player) {
            LivingEntity target = getLookAtTarget(player, 7.0);
            if (target == null) {
                player.displayClientMessage(
                        Component.literal("§cNo target in range!"), true);
                return;
            }
            // Slowness III + Weakness II for 3 seconds
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,  60, 2, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,  60, 1, false, true));
            player.displayClientMessage(
                    Component.literal("§9💤 Exhaust!"), true);
        }
    };

    // ── Fields ────────────────────────────────────────────────────────────────

    private final String displayName;
    private final String description;
    private final int    cooldownTicks;

    SummonerSpell(String displayName, String description, int cooldownTicks) {
        this.displayName   = displayName;
        this.description   = description;
        this.cooldownTicks = cooldownTicks;
    }

    public String getDisplayName()  { return displayName; }
    public String getDescription()  { return description; }
    public int    getCooldownTicks(){ return cooldownTicks; }

    /** Override in each enum constant to implement the spell. */
    public abstract void execute(ServerPlayer player);

    // ── Helper ────────────────────────────────────────────────────────────────

    protected static LivingEntity getLookAtTarget(ServerPlayer player, double range) {
        net.minecraft.world.phys.Vec3 eyePos  = player.getEyePosition();
        net.minecraft.world.phys.Vec3 lookVec = player.getLookAngle();
        net.minecraft.world.phys.Vec3 endPos  = eyePos.add(
                lookVec.x * range, lookVec.y * range, lookVec.z * range);

        net.minecraft.world.phys.AABB box = player.getBoundingBox().inflate(range);
        java.util.List<LivingEntity> candidates = player.level().getEntitiesOfClass(
                LivingEntity.class, box, e -> e != player && e.isAlive()
                        && !(e instanceof ServerPlayer sp && sp.isCreative()));

        LivingEntity best = null;
        double bestDist   = Double.MAX_VALUE;
        for (LivingEntity e : candidates) {
            var hit = e.getBoundingBox().clip(eyePos, endPos);
            if (hit.isPresent()) {
                double d = eyePos.distanceToSqr(hit.get());
                if (d < bestDist) { bestDist = d; best = e; }
            }
        }
        return best;
    }

    protected static LivingEntity getNearestEnemy(ServerPlayer player, double range) {
        AABB box = player.getBoundingBox().inflate(range);
        List<LivingEntity> entities = player.level().getEntitiesOfClass(
                LivingEntity.class, box,
                e -> e != player && e.isAlive() && !(e instanceof ServerPlayer sp && sp.isCreative()));
        if (entities.isEmpty()) return null;
        Vec3 pos = player.position();
        entities.sort((a, b) -> Double.compare(
                a.position().distanceToSqr(pos),
                b.position().distanceToSqr(pos)));
        return entities.get(0);
    }
}