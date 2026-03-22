package com.eilai.runeterra.entity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
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

import java.util.List;

@SuppressWarnings({"resource", "unused"})
public class ElderDrakeEntity extends Monster implements GeoEntity {

    // ── Animation ─────────────────────────────────────────────────────────────
    private static final int    MELEE_ANIM_TICKS = 37;
    private static final int    RANGE_ANIM_TICKS = 45;
    private static final int    RANGE_FIRE_DELAY = RANGE_ANIM_TICKS / 3;

    // ── Combat ────────────────────────────────────────────────────────────────
    private static final int    MELEE_COOLDOWN   = 60;
    private static final int    FIRE_COOLDOWN_P1 = 100;
    private static final int    FIRE_COOLDOWN_P2 = 70;
    private static final int    FIRE_COOLDOWN_P3 = 40;
    private static final double MELEE_RANGE      = 5.0;
    private static final double FIRE_RANGE_P1    = 30.0;
    private static final double FIRE_RANGE_P2    = 40.0;
    private static final double FIRE_RANGE_P3    = 50.0;

    // ── Flight ────────────────────────────────────────────────────────────────
    private static final double MIN_FLY_HEIGHT   = 4.0;
    private static final double CIRCLE_RADIUS    = 16.0;
    private static final double CIRCLE_HEIGHT    = 12.0;
    private static final double CIRCLE_SPEED_P1  = 0.018;
    private static final double CIRCLE_SPEED_P2  = 0.024;
    private static final double CIRCLE_SPEED_P3  = 0.034;
    private static final double FLY_SPEED        = 0.65;  // direct velocity for aerial movement
    private static final double WALK_SPEED        = 1.0;

    // ── Behaviour durations ───────────────────────────────────────────────────
    // With target: alternates ground phase (GROUND_COMBAT_DURATION) and air phase (AIR_COMBAT_DURATION)
    private static final int    GROUND_COMBAT_DURATION = 300; // 15s on ground attacking
    private static final int    AIR_COMBAT_DURATION    = 400; // 20s circling in air
    private static final int    SWOOP_COOLDOWN_TICKS   = 160;
    private static final int    WAYPOINT_TIMEOUT       = 120;

    // ── Wander (no target) ────────────────────────────────────────────────────
    private static final int    WANDER_RADIUS          = 100; // blocks
    private static final double WANDER_FLY_THRESHOLD   = 10.0; // fly if wander dest > this far
    private static final int    WANDER_PICK_INTERVAL   = 200; // pick new wander dest every 10s

    // ── Boss bar ──────────────────────────────────────────────────────────────
    private static final int    BOSS_BAR_RANGE   = 80;
    private static final int    MIN_SPAWN_Y      = 150;
    private static final double MIN_SPAWN_DIST   = 1024.0;
    private static final float  PHASE2_HP        = 0.60f;
    private static final float  PHASE3_HP        = 0.30f;

    // ── Animation names ───────────────────────────────────────────────────────
    @SuppressWarnings("SpellCheckingInspection")
    private static final String ANIM_MELEE  = "animation.elder_drake.attack_meele";
    private static final String ANIM_RANGE  = "animation.elder_drake.attack_range";
    private static final String ANIM_FLY    = "animation.elder_drake.fly";
    private static final String ANIM_IDLE   = "animation.elder_drake.stand";
    private static final String ANIM_WALK   = "animation.elder_drake.walk";
    private static final String CONTROLLER  = "controller";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossBar = new ServerBossEvent(
            Component.translatable("entity.runeterra.elder_drake"),
            BossEvent.BossBarColor.GREEN,
            BossEvent.BossBarOverlay.NOTCHED_10);

    // ── State machine ─────────────────────────────────────────────────────────
    private enum DrakeState {
        // Combat states (have target)
        GROUND_COMBAT,   // on ground, walking toward player, attacking
        TAKING_OFF,      // rising to circle height
        CIRCLING,        // orbiting player at height, firing
        SWOOPING,        // diving at player
        LANDING,         // descending to ground

        // Wander states (no target)
        WANDER_WALK,     // walking to a random nearby point
        WANDER_FLY,      // flying to a random far point
        WANDER_IDLE      // standing still briefly between wander destinations
    }

    private DrakeState drakeState  = DrakeState.WANDER_IDLE;
    private int        stateTicks  = 0;
    private int        swoopCooldown = SWOOP_COOLDOWN_TICKS;
    private double     circleAngle;
    private Vec3       waypointPos = null;  // current movement destination
    private int        wanderTimer         = 0;
    private Vec3       wanderFlyStart      = null;  // where the parabola arc began
    private double     wanderFlyHorizDist  = 1.0;   // total horiz distance of current arc
    private int        wanderIdleDuration  = 100; // randomised each time we go idle (5-15s)
    private int        consecutiveWalks    = 0;   // forces a fly after 5 walks in a row

    // ── Combat ────────────────────────────────────────────────────────────────
    private boolean isAttacking     = false;
    private int     attackAnimTimer = 0;
    private int     meleeCooldown   = 0;
    private int     fireCooldown    = 0;
    private int           meleeDamageTick   = 0;
    private LivingEntity  meleeDamageTarget = null;
    private int          rangeFireTick   = 0;
    private LivingEntity rangeFireTarget = null;
    private int          rangeFireCount  = 0;

    // ── Animation (synced to client via data watcher) ────────────────────────
    private static final EntityDataAccessor<Boolean> DATA_IS_FLYING =
            SynchedEntityData.defineId(ElderDrakeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_WALKING =
            SynchedEntityData.defineId(ElderDrakeEntity.class, EntityDataSerializers.BOOLEAN);

    public ElderDrakeEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.circleAngle = this.random.nextDouble() * Math.PI * 2;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_IS_FLYING,  false);
        builder.define(DATA_IS_WALKING, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,          500.0)
                .add(Attributes.ATTACK_DAMAGE,        20.0)
                .add(Attributes.MOVEMENT_SPEED,       0.55)
                .add(Attributes.FLYING_SPEED,         0.55)  // higher flight speed
                .add(Attributes.ARMOR,                15.0)
                .add(Attributes.FOLLOW_RANGE,         80.0)
                .add(Attributes.KNOCKBACK_RESISTANCE,  1.0)
                .add(Attributes.ATTACK_KNOCKBACK,      3.0);
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override protected void checkFallDamage(double y, boolean onGround,
                                             @NotNull BlockState state, @NotNull BlockPos pos) {}

    @Override
    protected void registerGoals() {
        // Only look goals — all movement is manual
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 32.0f));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ── Phase helpers ─────────────────────────────────────────────────────────
    private int getPhase() {
        float r = this.getHealth() / this.getMaxHealth();
        if (r <= PHASE3_HP) return 3;
        if (r <= PHASE2_HP) return 2;
        return 1;
    }

    private int getFireCooldown() {
        return switch (getPhase()) { case 3 -> FIRE_COOLDOWN_P3; case 2 -> FIRE_COOLDOWN_P2; default -> FIRE_COOLDOWN_P1; };
    }

    private double getFireRange() {
        return switch (getPhase()) { case 3 -> FIRE_RANGE_P3; case 2 -> FIRE_RANGE_P2; default -> FIRE_RANGE_P1; };
    }

    private double getCircleSpeed() {
        return switch (getPhase()) { case 3 -> CIRCLE_SPEED_P3; case 2 -> CIRCLE_SPEED_P2; default -> CIRCLE_SPEED_P1; };
    }

    // ── Boss bar ──────────────────────────────────────────────────────────────
    @Override public void startSeenByPlayer(@NotNull ServerPlayer p) { super.startSeenByPlayer(p); bossBar.addPlayer(p); }
    @Override public void stopSeenByPlayer(@NotNull ServerPlayer p)  { super.stopSeenByPlayer(p);  bossBar.removePlayer(p); }

    @Override public void die(@NotNull DamageSource cause) {
        super.die(cause);
        bossBar.setVisible(false);
        bossBar.getPlayers().stream().toList().forEach(bossBar::removePlayer);
    }

    @Override public void remove(@NotNull RemovalReason r) { super.remove(r); bossBar.setVisible(false); }

    private void updateBossBar() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        bossBar.setProgress(this.getHealth() / this.getMaxHealth());
        bossBar.setColor(switch (getPhase()) {
            case 3 -> BossEvent.BossBarColor.RED;
            case 2 -> BossEvent.BossBarColor.YELLOW;
            default -> BossEvent.BossBarColor.GREEN;
        });
        sl.getPlayers(p -> p.distanceTo(this) <= BOSS_BAR_RANGE).forEach(bossBar::addPlayer);
        bossBar.getPlayers().stream().toList().forEach(p -> {
            if (p.distanceTo(this) > BOSS_BAR_RANGE) bossBar.removePlayer(p);
        });
    }

    // ── Main tick ─────────────────────────────────────────────────────────────
    @Override
    public void tick() {
        super.tick();

        // Sync flying flag to client. Walking is set explicitly at the nav call sites.
        if (!this.level().isClientSide()) {
            boolean flying = drakeState == DrakeState.WANDER_FLY
                    || drakeState == DrakeState.TAKING_OFF
                    || drakeState == DrakeState.CIRCLING
                    || drakeState == DrakeState.SWOOPING;
            this.entityData.set(DATA_IS_FLYING, flying);
        }

        updateBossBar();

        if (attackAnimTimer > 0) {
            attackAnimTimer--;
            if (attackAnimTimer == 0) isAttacking = false;
        }

        if (!this.level().isClientSide()) {
            tickCombatTimers();
            tickStateMachine();
        }
    }

    private void tickCombatTimers() {
        if (meleeCooldown > 0) meleeCooldown--;
        if (fireCooldown  > 0) fireCooldown--;

        if (meleeDamageTick > 0) {
            meleeDamageTick--;
            if (meleeDamageTick == 0 && meleeDamageTarget != null
                    && meleeDamageTarget.isAlive()
                    && this.level() instanceof ServerLevel sl) {
                meleeDamageTarget.hurtServer(sl, this.damageSources().mobAttack(this),
                        (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
                meleeDamageTarget = null;
            }
        }

        if (rangeFireTick > 0) {
            rangeFireTick--;
            if (rangeFireTick == 0 && rangeFireTarget != null && rangeFireTarget.isAlive()) {
                spawnFireballs(rangeFireTarget, rangeFireCount);
                rangeFireTarget = null;
                rangeFireCount  = 0;
            }
        }
    }

    // ── State machine ─────────────────────────────────────────────────────────
    private void tickStateMachine() {
        LivingEntity target = this.getTarget();
        stateTicks++;

        boolean hasTarget = target != null && target.isAlive();

        if (!hasTarget) {
            // Lost/no target — cancel any in-progress combat state, wander freely
            if (isCombatState(drakeState)) {
                clearAttackState();
                // Descend if airborne, then wander
                if (this.entityData.get(DATA_IS_FLYING)) {
                    transitionTo(DrakeState.LANDING);
                } else {
                    transitionTo(DrakeState.WANDER_IDLE);
                }
            }
            tickWander();
            return;
        }

        // Has target — switch away from wander states into combat
        if (isWanderState(drakeState)) {
            this.getNavigation().stop();
            transitionTo(DrakeState.GROUND_COMBAT);
        }

        switch (drakeState) {
            case GROUND_COMBAT -> tickGroundCombat(target);
            case TAKING_OFF    -> tickTakingOff(target);
            case CIRCLING      -> tickCircling(target);
            case SWOOPING      -> tickSwooping(target);
            case LANDING       -> tickLanding(target);
            default            -> transitionTo(DrakeState.GROUND_COMBAT);
        }
    }

    private boolean isCombatState(DrakeState s) {
        return s == DrakeState.GROUND_COMBAT || s == DrakeState.TAKING_OFF
                || s == DrakeState.CIRCLING   || s == DrakeState.SWOOPING;
    }

    private boolean isWanderState(DrakeState s) {
        return s == DrakeState.WANDER_WALK || s == DrakeState.WANDER_FLY
                || s == DrakeState.WANDER_IDLE;
    }

    private void transitionTo(DrakeState next) {
        if (drakeState == next) return;
        drakeState  = next;
        stateTicks  = 0;
        waypointPos = null;
        wanderFlyStart = null; // reset parabola so next flight re-initialises
        // Clear walk anim on any state change — the new state will re-enable it if needed
        this.entityData.set(DATA_IS_WALKING, false);
        if (next == DrakeState.WANDER_IDLE) {
            // Stay idle for a random 5–15 seconds
            wanderIdleDuration = 140 + this.random.nextInt(161); // 140–300 ticks (7–15s)
        }
    }

    private void clearAttackState() {
        isAttacking = false; attackAnimTimer = 0;
        meleeDamageTick = 0; meleeDamageTarget = null;
        rangeFireTick = 0;   rangeFireTarget = null; rangeFireCount = 0;
    }

    // ── WANDER (no target) ────────────────────────────────────────────────────
    private void tickWander() {
        wanderTimer++;

        // LANDING during wander — just descend, then switch to WANDER_IDLE
        if (drakeState == DrakeState.LANDING) {
            tickLanding(null);
            return;
        }

        // Only pick a new destination when:
        //  - actively walking/flying and stuck or have no waypoint
        //  - idling and the idle timer has elapsed
        // NEVER pick just because waypointPos is null during WANDER_IDLE — transitionTo()
        // always clears waypointPos, so that would skip the idle pause entirely.
        boolean isMovingWander = drakeState == DrakeState.WANDER_WALK || drakeState == DrakeState.WANDER_FLY;
        boolean needsDest = (isMovingWander && (waypointPos == null || stateTicks > WANDER_PICK_INTERVAL))
                || (drakeState == DrakeState.WANDER_IDLE && stateTicks > wanderIdleDuration);

        if (needsDest) {
            pickWanderDestination();
            return;
        }

        switch (drakeState) {
            case WANDER_WALK -> tickWanderWalk();
            case WANDER_FLY  -> tickWanderFly();
            case WANDER_IDLE -> tickWanderIdle(); // wait, but land if airborne
            default          -> transitionTo(DrakeState.WANDER_IDLE);
        }
    }

    private void pickWanderDestination() {
        double angle = this.random.nextDouble() * Math.PI * 2;

        // 70% chance walk (short), 30% chance fly (far) — but force fly after 5 consecutive walks
        boolean forceFly = consecutiveWalks >= 5;
        boolean chooseFly = forceFly || this.random.nextDouble() > 0.70;

        double radius;
        if (chooseFly) {
            // Far destination: 10–100 blocks
            radius = WANDER_FLY_THRESHOLD + 1 + this.random.nextDouble() * (WANDER_RADIUS - WANDER_FLY_THRESHOLD - 1);
            consecutiveWalks = 0; // reset counter
        } else {
            // Short destination: 2–10 blocks
            radius = 2 + this.random.nextDouble() * (WANDER_FLY_THRESHOLD - 2);
            consecutiveWalks++;
        }

        double destX = this.getX() + Math.cos(angle) * radius;
        double destZ = this.getZ() + Math.sin(angle) * radius;
        double destY = getGroundY(BlockPos.containing(destX, this.getY(), destZ));

        // Avoid water — if the landing spot is water, stay idle and try again next cycle
        BlockPos destGround = BlockPos.containing(destX, destY - 1, destZ);
        if (this.level().getBlockState(destGround).getFluidState().isSource()) {
            transitionTo(DrakeState.WANDER_IDLE);
            return;
        }

        if (chooseFly) {
            waypointPos = new Vec3(destX, destY + 8.0, destZ);
            transitionTo(DrakeState.WANDER_FLY);
        } else {
            waypointPos = new Vec3(destX, destY, destZ);
            transitionTo(DrakeState.WANDER_WALK);
        }
    }

    private void tickWanderWalk() {
        if (waypointPos == null) {
            this.entityData.set(DATA_IS_WALKING, false);
            transitionTo(DrakeState.WANDER_IDLE);
            return;
        }

        double horizDist = Math.sqrt(
                Math.pow(this.getX() - waypointPos.x, 2) +
                        Math.pow(this.getZ() - waypointPos.z, 2));

        if (horizDist < 2.0) {
            // Arrived — stop nav and animation immediately
            this.getNavigation().stop();
            this.entityData.set(DATA_IS_WALKING, false);
            transitionTo(DrakeState.WANDER_IDLE);
            return;
        }

        if (this.entityData.get(DATA_IS_FLYING)) { applyDownwardPush(); return; }

        this.getNavigation().moveTo(waypointPos.x, waypointPos.y, waypointPos.z, WALK_SPEED);
        this.entityData.set(DATA_IS_WALKING, true);
        clampToGround();
    }

    private void tickWanderIdle() {
        this.entityData.set(DATA_IS_WALKING, false);
        if (!this.onGround()) {
            applyDownwardPush();
        } else {
            Vec3 cur = this.getDeltaMovement();
            this.setDeltaMovement(cur.x * 0.3, cur.y, cur.z * 0.3);
        }
        this.getNavigation().stop();
    }

    private void tickWanderFly() {
        if (waypointPos == null) { transitionTo(DrakeState.WANDER_IDLE); return; }

        // Initialise parabola on the first tick of this flight
        if (wanderFlyStart == null) {
            wanderFlyStart = this.position();
            double dx = waypointPos.x - wanderFlyStart.x;
            double dz = waypointPos.z - wanderFlyStart.z;
            wanderFlyHorizDist = Math.max(1.0, Math.sqrt(dx * dx + dz * dz));
        }

        if (stateTicks > WAYPOINT_TIMEOUT * 3) {
            transitionTo(DrakeState.LANDING);
            return;
        }

        // Horizontal progress: 0.0 at start, 1.0 at destination
        double dxNow = this.getX() - wanderFlyStart.x;
        double dzNow = this.getZ() - wanderFlyStart.z;
        double horizTravelled = Math.sqrt(dxNow * dxNow + dzNow * dzNow);
        double progress = Math.min(1.0, horizTravelled / wanderFlyHorizDist);

        // Parabola: sin(progress * PI) peaks at 0.5 (midpoint of journey).
        // Arc height scales with distance so short hops have a gentle arc
        // and long flights have a tall one (capped at 20 blocks).
        double arcHeight = Math.min(20.0, wanderFlyHorizDist * 0.25);
        double parabolaY = wanderFlyStart.y + Math.sin(progress * Math.PI) * arcHeight;

        // When past the apex (progress > 0.5) descend toward ground-level destination
        double targetY = progress < 0.5 ? parabolaY
                : parabolaY + (waypointPos.y - parabolaY) * ((progress - 0.5) * 2.0);

        // Horizontal: fly straight toward destination XZ
        Vec3 toWaypoint = waypointPos.subtract(this.position());
        double horizDist = Math.sqrt(toWaypoint.x * toWaypoint.x + toWaypoint.z * toWaypoint.z);

        if (horizDist < 3.0) {
            wanderFlyStart = null;
            transitionTo(DrakeState.LANDING);
            return;
        }

        // Obstacle avoidance — probe ahead and nudge upward if blocked
        Vec3 horizDir = new Vec3(toWaypoint.x, 0, toWaypoint.z).normalize();
        Vec3 probe = this.position().add(horizDir.scale(3.0));
        BlockPos probePos = BlockPos.containing(probe.x, probe.y, probe.z);
        boolean blocked = !this.level().getBlockState(probePos).isAir()
                || !this.level().getBlockState(probePos.above()).isAir()
                || !this.level().getBlockState(BlockPos.containing(probe.x, probe.y + 1, probe.z)).isAir();
        if (blocked) targetY = Math.max(targetY, this.getY() + 3.0);

        double dyToTarget = targetY - this.getY();
        double speed = Math.min(FLY_SPEED, horizDist * 0.08);
        Vec3 desired = new Vec3(horizDir.x * speed, dyToTarget * 0.15, horizDir.z * speed);
        Vec3 cur = this.getDeltaMovement();
        this.setDeltaMovement(
                cur.x * 0.4 + desired.x * 0.6,
                cur.y * 0.4 + desired.y * 0.6,
                cur.z * 0.4 + desired.z * 0.6);

        faceMovementDirection();
        this.getNavigation().stop();
    }

    // ── GROUND_COMBAT ─────────────────────────────────────────────────────────
    private void tickGroundCombat(LivingEntity target) {
        double dist = this.distanceTo(target);

        if (this.entityData.get(DATA_IS_FLYING)) { applyDownwardPush(); return; }

        if (!isAttacking) {
            if (dist <= MELEE_RANGE && meleeCooldown == 0) {
                this.entityData.set(DATA_IS_WALKING, false);
                this.getNavigation().stop();
                doMeleeAttack(target);
            } else if (dist <= getFireRange() && fireCooldown == 0) {
                this.entityData.set(DATA_IS_WALKING, false);
                this.getNavigation().stop();
                doRangeAttack(target, 1);
            } else {
                this.getNavigation().moveTo(target, WALK_SPEED);
                this.entityData.set(DATA_IS_WALKING, true);
                clampToGround();
            }
        } else {
            // Attack in progress — stop walking
            this.entityData.set(DATA_IS_WALKING, false);
        }

        // After GROUND_COMBAT_DURATION, take off and circle
        if (stateTicks > GROUND_COMBAT_DURATION) {
            transitionTo(DrakeState.TAKING_OFF);
        }
    }

    // ── TAKING_OFF ────────────────────────────────────────────────────────────
    private void tickTakingOff(LivingEntity target) {
        this.getNavigation().stop();
        double targetY = (target != null ? target.getY() : this.getY()) + CIRCLE_HEIGHT;
        double dy = targetY - this.getY();

        if (dy > 0.5) {
            double orbitX = (target != null ? target.getX() : this.getX()) + Math.cos(circleAngle) * CIRCLE_RADIUS;
            double orbitZ = (target != null ? target.getZ() : this.getZ()) + Math.sin(circleAngle) * CIRCLE_RADIUS;
            Vec3 toStart = new Vec3(orbitX - this.getX(), dy, orbitZ - this.getZ()).normalize().scale(0.6);
            Vec3 cur = this.getDeltaMovement();
            this.setDeltaMovement(
                    cur.x * 0.5 + toStart.x * 0.5,
                    Math.min(cur.y + 0.10, 0.8),
                    cur.z * 0.5 + toStart.z * 0.5);
            faceMovementDirection();
        } else {
            transitionTo(DrakeState.CIRCLING);
        }
    }

    // ── CIRCLING ──────────────────────────────────────────────────────────────
    private void tickCircling(LivingEntity target) {
        circleAngle += getCircleSpeed();

        double orbitX = target.getX() + Math.cos(circleAngle) * CIRCLE_RADIUS;
        double orbitY = target.getY() + CIRCLE_HEIGHT;
        double orbitZ = target.getZ() + Math.sin(circleAngle) * CIRCLE_RADIUS;

        Vec3 toOrbit = new Vec3(orbitX - this.getX(), orbitY - this.getY(), orbitZ - this.getZ());
        double gap   = toOrbit.length();
        double speed = Math.min(FLY_SPEED, gap * 0.10);
        Vec3 desired = toOrbit.normalize().scale(speed);
        Vec3 cur     = this.getDeltaMovement();
        this.setDeltaMovement(
                cur.x * 0.4 + desired.x * 0.6,
                cur.y * 0.4 + desired.y * 0.6,
                cur.z * 0.4 + desired.z * 0.6);

        enforceMinHeight();
        faceMovementDirection(); // face the orbit flight path

        // Fire while circling
        double dist = this.distanceTo(target);
        if (!isAttacking && fireCooldown == 0 && dist <= getFireRange()) {
            int shots = (getPhase() == 3) ? 3 : (getPhase() == 2) ? 2 : 1;
            doRangeAttack(target, shots);
        }

        // Occasional swoop
        if (swoopCooldown > 0) swoopCooldown--;
        if (swoopCooldown == 0 && dist < 35) {
            transitionTo(DrakeState.SWOOPING);
            swoopCooldown = SWOOP_COOLDOWN_TICKS;
        }

        // After AIR_COMBAT_DURATION, land and fight on ground (phase 3 stays up)
        if (getPhase() < 3 && stateTicks > AIR_COMBAT_DURATION) {
            transitionTo(DrakeState.LANDING);
        }
    }

    // ── SWOOPING ──────────────────────────────────────────────────────────────
    private void tickSwooping(LivingEntity target) {
        double dist = this.distanceTo(target);
        Vec3 toTarget = target.position().subtract(this.position()).normalize().scale(1.6);
        this.setDeltaMovement(toTarget.x, toTarget.y * 0.7, toTarget.z);
        faceMovementDirection();
        // intentionally no enforceMinHeight — swoop dives at the player

        if (!isAttacking && dist <= MELEE_RANGE && meleeCooldown == 0) {
            doMeleeAttack(target);
        }
        if (stateTicks > 60 || dist < 3) {
            transitionTo(DrakeState.CIRCLING);
        }
    }

    // ── LANDING ───────────────────────────────────────────────────────────────
    // target may be null (landing after losing target)
    private void tickLanding(@Nullable LivingEntity target) {
        if (waypointPos == null) {
            double gx = target != null ? this.getX() + (target.getX() - this.getX()) * 0.4 : this.getX();
            double gz = target != null ? this.getZ() + (target.getZ() - this.getZ()) * 0.4 : this.getZ();
            double gy = getGroundY(BlockPos.containing(gx, this.getY(), gz));
            waypointPos = new Vec3(gx, gy, gz);
        }

        Vec3 toLand = waypointPos.subtract(this.position());
        double dist = toLand.length();

        if (dist > 1.5) {
            Vec3 desired = toLand.normalize().scale(Math.min(0.55, dist * 0.10));
            Vec3 cur = this.getDeltaMovement();
            this.setDeltaMovement(
                    cur.x * 0.5 + desired.x * 0.5,
                    cur.y * 0.5 + desired.y * 0.5,
                    cur.z * 0.5 + desired.z * 0.5);
        } else {
            // Landed — go to appropriate next state
            if (target != null && target.isAlive()) {
                transitionTo(DrakeState.GROUND_COMBAT);
            } else {
                transitionTo(DrakeState.WANDER_IDLE);
            }
        }

        if (stateTicks > WAYPOINT_TIMEOUT) {
            transitionTo(target != null ? DrakeState.GROUND_COMBAT : DrakeState.WANDER_IDLE);
        }
    }

    // ── Flight helpers ────────────────────────────────────────────────────────
    // Makes the drake face the direction it is currently moving.
    // yaw is computed from horizontal velocity; pitch from vertical.
    private void faceMovementDirection() {
        Vec3 vel = this.getDeltaMovement();
        double hLen = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        if (hLen < 0.01) return; // not moving — don't snap
        float targetYaw   = (float)(Math.toDegrees(Math.atan2(-vel.x, vel.z)));
        float targetPitch = (float)(Math.toDegrees(-Math.atan2(vel.y, hLen)));
        // Smooth interpolation so it doesn't snap instantly
        this.setYRot(lerpAngle(this.getYRot(), targetYaw,  0.15f));
        this.setXRot(lerpAngle(this.getXRot(), targetPitch, 0.10f));
        this.yRotO = this.getYRot();
    }

    private static float lerpAngle(float current, float target, float t) {
        float diff = target - current;
        // Wrap to [-180, 180]
        while (diff >  180) diff -= 360;
        while (diff < -180) diff += 360;
        return current + diff * t;
    }

    private void enforceMinHeight() {
        if (drakeState == DrakeState.LANDING || drakeState == DrakeState.GROUND_COMBAT
                || drakeState == DrakeState.WANDER_WALK) return;
        double groundY = getGroundY();
        if (this.getY() < groundY + MIN_FLY_HEIGHT) {
            Vec3 cur = this.getDeltaMovement();
            this.setDeltaMovement(cur.x, Math.max(cur.y, 0.28), cur.z);
        }
    }

    // Called every tick during ground movement — zeros Y velocity completely so
    // FlyingMoveControl cannot lift the entity off the ground.
    // Vanilla gravity is added by the physics step AFTER this, so the entity
    // still gets pulled down and stays flush with the ground naturally.
    private void clampToGround() {
        Vec3 cur = this.getDeltaMovement();
        this.setDeltaMovement(cur.x, 0.0, cur.z);
    }

    private void applyDownwardPush() {
        Vec3 cur = this.getDeltaMovement();
        this.setDeltaMovement(cur.x * 0.7, -0.28, cur.z * 0.7);
    }

    private double getGroundY() { return getGroundY(this.blockPosition()); }

    private double getGroundY(BlockPos from) {
        BlockPos pos = from;
        for (int i = 0; i < 64; i++) {
            if (!this.level().getBlockState(pos).isAir()) return pos.getY() + 1.0;
            pos = pos.below();
        }
        return this.level().getMinY();
    }

    // ── Attacks ───────────────────────────────────────────────────────────────
    private void doMeleeAttack(LivingEntity target) {
        isAttacking = true; attackAnimTimer = MELEE_ANIM_TICKS;
        meleeCooldown = MELEE_COOLDOWN;
        meleeDamageTick = MELEE_ANIM_TICKS / 2; meleeDamageTarget = target;
        this.triggerAnim(CONTROLLER, ANIM_MELEE);
    }

    private void doRangeAttack(LivingEntity target, int count) {
        isAttacking = true; attackAnimTimer = RANGE_ANIM_TICKS;
        fireCooldown = getFireCooldown();
        rangeFireTick = RANGE_FIRE_DELAY; rangeFireTarget = target; rangeFireCount = count;
        this.triggerAnim(CONTROLLER, ANIM_RANGE);
    }

    private void spawnFireballs(LivingEntity target, int count) {
        for (int i = 0; i < count; i++) {
            double spread = (count == 1) ? 0 : (i - (count - 1) / 2.0) * 0.18;
            Vec3 dir = target.position().subtract(this.position()).normalize().add(spread, 0.05, spread);
            this.level().addFreshEntity(new ElderDrakeFireball(this.level(), this, dir.x, dir.y, dir.z));
        }
    }

    // ── Spawn rules ───────────────────────────────────────────────────────────
    @SuppressWarnings("DataFlowIssue")
    public static boolean checkElderDrakeSpawnRules(
            EntityType<ElderDrakeEntity> type, ServerLevelAccessor level,
            EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        if (pos.getY() < MIN_SPAWN_Y) return false;
        if (!level.getLevelData().isThundering()) return false;
        if (!level.canSeeSky(pos)) return false;
        List<ElderDrakeEntity> nearby = level.getEntitiesOfClass(ElderDrakeEntity.class,
                new AABB(pos.getX() - MIN_SPAWN_DIST, pos.getY() - 128, pos.getZ() - MIN_SPAWN_DIST,
                        pos.getX() + MIN_SPAWN_DIST, pos.getY() + 128, pos.getZ() + MIN_SPAWN_DIST));
        if (!nearby.isEmpty()) return false;
        return true;
    }

    // ── NBT ───────────────────────────────────────────────────────────────────
    @Override public void readAdditionalSaveData(@NotNull ValueInput in)  { super.readAdditionalSaveData(in); if (this.hasCustomName()) bossBar.setName(this.getDisplayName()); }
    @Override public void addAdditionalSaveData(@NotNull ValueOutput out) { super.addAdditionalSaveData(out); }
    @Override public void setCustomName(@Nullable Component name)         { super.setCustomName(name); bossBar.setName(this.getDisplayName()); }

    // ── Animation controllers ─────────────────────────────────────────────────
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(CONTROLLER + "_move", 0, state -> {
            if (this.entityData.get(DATA_IS_FLYING))  return state.setAndContinue(RawAnimation.begin().thenLoop(ANIM_FLY));
            if (this.entityData.get(DATA_IS_WALKING)) return state.setAndContinue(RawAnimation.begin().thenLoop(ANIM_WALK));
            return state.setAndContinue(RawAnimation.begin().thenLoop(ANIM_IDLE));
        }));
        AnimationController<ElderDrakeEntity> atk = new AnimationController<>(CONTROLLER, 0, state -> PlayState.STOP);
        atk.triggerableAnim(ANIM_MELEE, RawAnimation.begin().thenPlay(ANIM_MELEE));
        atk.triggerableAnim(ANIM_RANGE, RawAnimation.begin().thenPlay(ANIM_RANGE));
        controllers.add(atk);
    }


    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    public boolean isDrakeFlying()    { return this.entityData.get(DATA_IS_FLYING); }
    public boolean isDrakeAttacking() { return isAttacking; }
}