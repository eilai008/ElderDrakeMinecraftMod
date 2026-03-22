package com.eilai.runeterra.item.weapon;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Vayne's weapon — "The Bolt"
 *
 * This item is never placed in the real inventory.
 * It is HUD-only. Firing is triggered by right-click
 * intercepted in VayneRightClickHandler.
 *
 * Attack speed 2.4 → cooldown = 20/2.4 ≈ 8 ticks
 */
public class VayneWeaponItem extends ChampionWeapon {

    // Ticks remaining until next shot per player
    private static final Map<UUID, Integer> COOLDOWNS = new HashMap<>();

    public VayneWeaponItem(Item.Properties properties) {
        super("vayne", "The Bolt", 5.0, 2.4, 10.0, properties);
    }

    // ── Fire ──────────────────────────────────────────────────────────────────

    /**
     * Called from VayneRightClickHandler on right-click.
     * Returns false if on cooldown.
     */
    public static boolean tryFire(ServerPlayer player) {
        if (COOLDOWNS.getOrDefault(player.getUUID(), 0) > 0) return false;

        if (!(player.level() instanceof ServerLevel sl)) return false;

        // Build arrow — 1.21.11 constructor: Arrow(Level, double x, double y, double z,
        //   ItemStack arrowStack, ItemStack weaponStack)
        Arrow arrow = new Arrow(sl,
                player.getX(),
                player.getEyeY() - 0.1,
                player.getZ(),
                Items.ARROW.getDefaultInstance(),
                null);

        // Direction from look angle
        Vec3 look = player.getLookAngle();
        arrow.setDeltaMovement(look.x * 3.0, look.y * 3.0, look.z * 3.0);
        arrow.setOwner(player);
        arrow.setBaseDamage(5.0);
        arrow.setCritArrow(false);
        arrow.pickup = Arrow.Pickup.DISALLOWED;

        sl.addFreshEntity(arrow);
        player.playSound(SoundEvents.ARROW_SHOOT, 1.0f, 1.0f);

        // 8 ticks ≈ 2.4 shots/sec
        COOLDOWNS.put(player.getUUID(), 8);
        return true;
    }

    // ── Tick / cleanup ────────────────────────────────────────────────────────

    public static void tickCooldowns() {
        COOLDOWNS.replaceAll((id, cd) -> Math.max(0, cd - 1));
    }

    public static boolean isOnCooldown(UUID id) {
        return COOLDOWNS.getOrDefault(id, 0) > 0;
    }

    public static void clearPlayer(UUID id) {
        COOLDOWNS.remove(id);
    }
}