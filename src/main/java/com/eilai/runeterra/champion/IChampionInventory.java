package com.eilai.runeterra.champion;

import net.minecraft.world.item.ItemStack;

/**
 * Interface mixed into net.minecraft.world.entity.player.Inventory
 * by InventoryMixin. Provides access to the champion weapon slot.
 *
 * Usage:
 *   if (player.getInventory() instanceof IChampionInventory ci) {
 *       ItemStack weapon = ci.runeterra$getWeaponStack();
 *   }
 *
 * The runeterra$ prefix is the Mixin convention to avoid name collisions.
 */
public interface IChampionInventory {

    ItemStack runeterra$getWeaponStack();

    void runeterra$setWeaponStack(ItemStack stack);

    boolean runeterra$hasWeapon();
}
