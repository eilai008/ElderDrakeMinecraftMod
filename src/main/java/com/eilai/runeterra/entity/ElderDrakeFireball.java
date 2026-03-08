package com.eilai.runeterra.entity;

import com.eilai.runeterra.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class ElderDrakeFireball extends AbstractHurtingProjectile {

    private static final int MAX_LIFETIME = 100; // 5 seconds
    private static final float EXPLOSION_RADIUS = 3.5f; // similar to TNT (4.0f)
    private int lifeTicks = 0;

    public ElderDrakeFireball(EntityType<? extends AbstractHurtingProjectile> type, Level level) {
        super(type, level);
    }

    public ElderDrakeFireball(Level level, LivingEntity shooter,
                              double xPower, double yPower, double zPower) {
        super(ModEntities.ELDER_DRAKE_FIREBALL.get(), shooter,
                new Vec3(xPower, yPower, zPower), level);
    }

    @Override
    public void tick() {
        super.tick();

        lifeTicks++;
        if (lifeTicks >= MAX_LIFETIME) {
            explode();
            return;
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            // Flame trail
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    this.getX(), this.getY(), this.getZ(),
                    8, 0.3, 0.3, 0.3, 0.05);
            // Smoke trail
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    this.getX(), this.getY(), this.getZ(),
                    3, 0.2, 0.2, 0.2, 0.01);
            // Dragon breath glow effect
            serverLevel.sendParticles(ParticleTypes.DRIPPING_LAVA,
                    this.getX(), this.getY(), this.getZ(),
                    2, 0.1, 0.1, 0.1, 0.02);
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        explode();
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult result) {
        super.onHitBlock(result);
        explode();
    }

    private void explode() {
        if (this.isRemoved()) return;

        Level level = this.level();
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            // TNT-style explosion that destroys blocks
            // Explosion.BlockInteraction.DESTROY_WITH_DECAY destroys most blocks
            // (same as TNT), DESTROY destroys everything, NONE does no block damage
            serverLevel.explode(
                    this,                                    // source entity
                    this.damageSources().explosion(this, this.getOwner() instanceof LivingEntity le ? le : this),
                    null,                                    // explosion damager (null = use default)
                    this.getX(), this.getY(), this.getZ(),   // position
                    EXPLOSION_RADIUS,                        // radius
                    true,                                    // fire — sets fire in the area like TNT
                    Level.ExplosionInteraction.TNT           // block interaction — destroys blocks like TNT
            );
        }

        this.discard();
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}