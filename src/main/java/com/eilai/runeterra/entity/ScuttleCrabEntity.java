package com.eilai.runeterra.entity;

import com.eilai.runeterra.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ScuttleCrabEntity extends Animal implements GeoEntity {

    // ── Animation names ───────────────────────────────────────────────────────
    private static final String ANIM_IDLE   = "animation.scuttle_crab.idle";
    private static final String ANIM_WALK   = "animation.scuttle_crab.walk";
    private static final String ANIM_RUN    = "animation.scuttle_crab.run";
    private static final String ANIM_DASH   = "animation.scuttle_crab.dash";
    private static final String ANIM_EAT    = "animation.scuttle_crab.eat";
    private static final String ANIM_ATTACK = "animation.scuttle_crab.attack";

    private static final String TRIG_DASH   = "dash";
    private static final String TRIG_EAT    = "eat";
    private static final String TRIG_ATTACK = "attack";

    private static final String CONTROLLER  = "move_controller";

    // ── Synced data ───────────────────────────────────────────────────────────
    private static final EntityDataAccessor<Boolean> DATA_SADDLE =
            SynchedEntityData.defineId(ScuttleCrabEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_WALKING =
            SynchedEntityData.defineId(ScuttleCrabEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_RUNNING =
            SynchedEntityData.defineId(ScuttleCrabEntity.class, EntityDataSerializers.BOOLEAN);

    // ── Server-side state ─────────────────────────────────────────────────────
    private int dashCooldown = 0;
    private boolean isDashing = false;
    private int dashTimer   = 0;
    private int eatTimer    = 200;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ScuttleCrabEntity(EntityType<? extends ScuttleCrabEntity> type, Level level) {
        super(type, level);

    }

    // ── Attributes ────────────────────────────────────────────────────────────

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH,      30.0)
                .add(Attributes.MOVEMENT_SPEED,  0.28)
                .add(Attributes.ATTACK_DAMAGE,   4.0)
                .add(Attributes.FOLLOW_RANGE,    16.0);
    }

    // ── Synced data init ──────────────────────────────────────────────────────

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SADDLE,     false);
        builder.define(DATA_IS_WALKING, false);
        builder.define(DATA_IS_RUNNING, false);
    }

    // ── AI goals ──────────────────────────────────────────────────────────────

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.8));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.2,
                stack -> stack.is(Items.TROPICAL_FISH)
                        || stack.is(Items.COD)
                        || stack.is(Items.SALMON), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            tickServer();
        }
    }

    private void tickServer() {
        if (dashCooldown > 0) dashCooldown--;

        if (isDashing) {
            if (--dashTimer <= 0) isDashing = false;
        }

        // Eat animation — triggered randomly when idle on grass/dirt
        if (!this.isVehicle() && this.onGround() && !isDashing) {
            if (--eatTimer <= 0) {
                eatTimer = 150 + this.random.nextInt(250);
                BlockPos below = this.blockPosition().below();
                if (this.level().getBlockState(below).is(Blocks.GRASS_BLOCK) ||
                        this.level().getBlockState(below).is(Blocks.DIRT)) {
                    this.triggerAnim(CONTROLLER, TRIG_EAT);
                }
            }
        }

        // Sync animation flags
        double hSpeed = this.getDeltaMovement().horizontalDistance();
        boolean ridden = this.isVehicle() && this.isSaddled();
        this.entityData.set(DATA_IS_RUNNING, ridden  && hSpeed > 0.12);
        this.entityData.set(DATA_IS_WALKING, !ridden && hSpeed > 0.04);
    }

    // ── Saddle ────────────────────────────────────────────────────────────────

    public boolean isSaddled() { return this.entityData.get(DATA_SADDLE); }

    private void setSaddled(boolean value) { this.entityData.set(DATA_SADDLE, value); }

    @Override
    public @NotNull InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (stack.is(Items.SADDLE) && !this.isSaddled()) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            setSaddled(true);
            this.playSound(SoundEvents.HORSE_SADDLE.value());
            return InteractionResult.SUCCESS;
        }

        if (this.isSaddled() && !player.isSecondaryUseActive() && !stack.is(Items.SADDLE)) {
            if (!this.level().isClientSide()) {
                player.startRiding(this);
            }
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    // ── Riding: punch to dash ─────────────────────────────────────────────────

    @Override
    public boolean hurtServer(@NotNull ServerLevel level, @NotNull DamageSource source, float amount) {
        // If the rider left-clicks the crab while moving → dash
        if (source.getEntity() instanceof Player rider
                && rider == this.getFirstPassenger()
                && dashCooldown <= 0
                && !isDashing) {
            double hSpeed = this.getDeltaMovement().horizontalDistance();
            if (hSpeed > 0.05) {
                performDash();
                return false;
            }
        }
        return super.hurtServer(level, source, amount);
    }

    private void performDash() {
        float yawRad = (float) Math.toRadians(this.getYRot());
        double dx = -Math.sin(yawRad) * 1.8;
        double dz =  Math.cos(yawRad) * 1.8;
        this.setDeltaMovement(dx, 0.25, dz);
        isDashing    = true;
        dashTimer    = 12;
        dashCooldown = 80;
        this.triggerAnim(CONTROLLER, TRIG_DASH);
    }

    // ── Rider controls ────────────────────────────────────────────────────────


    @Override
    protected @NotNull Vec3 getRiddenInput(@NotNull Player player, @NotNull Vec3 travelVector) {
        if (!this.isSaddled()) return Vec3.ZERO;
        return new Vec3(player.xxa * 0.5f, 0.0, player.zza);
    }

    @Override
    protected float getRiddenSpeed(@NotNull Player player) {
        if (!this.isSaddled()) return 0f;
        return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 2.5f;
    }

    @Override
    public @NotNull Vec3 getPassengerRidingPosition(@NotNull Entity passenger) {
        return this.position().add(0.0, this.getBbHeight() * 0.82, 0.0);
    }

    // ── Food / breeding ───────────────────────────────────────────────────────

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.TROPICAL_FISH)
                || stack.is(Items.COD)
                || stack.is(Items.SALMON);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel level, @NotNull AgeableMob other) {
        return ModEntities.SCUTTLE_CRAB.get().create(level, EntitySpawnReason.BREEDING);
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(@NotNull ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("Saddle", isSaddled());
    }

    @Override
    public void readAdditionalSaveData(@NotNull ValueInput in) {
        super.readAdditionalSaveData(in);
        setSaddled(in.getBooleanOr("Saddle", false));
    }

    // ── GeckoLib ──────────────────────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<ScuttleCrabEntity> moveCtrl = new AnimationController<>(CONTROLLER, 3, state -> {
            if (entityData.get(DATA_IS_RUNNING)) return state.setAndContinue(RawAnimation.begin().thenLoop(ANIM_RUN));
            if (entityData.get(DATA_IS_WALKING)) return state.setAndContinue(RawAnimation.begin().thenLoop(ANIM_WALK));
            return state.setAndContinue(RawAnimation.begin().thenLoop(ANIM_IDLE));
        });
        moveCtrl.triggerableAnim(TRIG_DASH,   RawAnimation.begin().thenPlay(ANIM_DASH));
        moveCtrl.triggerableAnim(TRIG_EAT,    RawAnimation.begin().thenPlay(ANIM_EAT));
        moveCtrl.triggerableAnim(TRIG_ATTACK, RawAnimation.begin().thenPlay(ANIM_ATTACK));
        controllers.add(moveCtrl);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}