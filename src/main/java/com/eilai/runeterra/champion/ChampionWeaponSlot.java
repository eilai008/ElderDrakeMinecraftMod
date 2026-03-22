package com.eilai.runeterra.champion;

import com.eilai.runeterra.init.WeaponRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Manages the champion weapon slot system.
 *
 * Rules:
 *  - Slot 0 (first hotbar slot) is the champion weapon slot
 *  - The weapon is locked — it cannot be moved, replaced, or dropped
 *  - Every server tick, the slot is validated and restored if tampered with
 *  - On champion switch, the weapon is swapped automatically
 *  - "No Champion" players get an empty locked placeholder (air, guarded)
 *
 * The slot is rendered by WeaponSlotHud on the client side.
 */
@EventBusSubscriber(modid = "runeterra")
public class ChampionWeaponSlot {

    /** The hotbar index used for the champion weapon. */
    public static final int WEAPON_SLOT = 0;

    // ── Tick validation ───────────────────────────────────────────────────────

    /**
     * Every tick, ensure slot 0 contains the correct champion weapon.
     * This catches any attempt to move or replace the item.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        PlayerChampionData data = PlayerChampionData.get(player);
        String champId = data.getChampionId();

        Inventory inv = player.getInventory();
        ItemStack current = inv.getItem(WEAPON_SLOT);

        // Check if the current item is the correct champion weapon
        if (!isCorrectWeapon(current, champId)) {
            // Put the correct weapon back
            ItemStack correct = WeaponRegistry.createWeaponStack(champId);
            inv.setItem(WEAPON_SLOT, correct);

            // If the player somehow has the weapon elsewhere in their inventory,
            // remove the duplicate
            if (!correct.isEmpty()) {
                removeDuplicatesFrom(inv, champId);
            }
        }
    }

    // ── Called when champion is switched ─────────────────────────────────────

    /**
     * Call this whenever a player's champion changes.
     * Swaps out the old weapon and puts in the new one.
     */
    public static void onChampionChanged(ServerPlayer player, String newChampionId) {
        Inventory inv = player.getInventory();

        // Clear slot 0
        inv.setItem(WEAPON_SLOT, ItemStack.EMPTY);

        // Put new weapon in (or leave empty for no_champion)
        ItemStack newWeapon = WeaponRegistry.createWeaponStack(newChampionId);
        inv.setItem(WEAPON_SLOT, newWeapon);

        // Remove any duplicates from rest of inventory
        if (!newWeapon.isEmpty()) {
            removeDuplicatesFrom(inv, newChampionId);
        }
    }

    // ── Prevent moving via container events ───────────────────────────────────

    /**
     * Intercepts container interactions to block moving the weapon out of slot 0.
     * Note: the tick-based restore above is the main safety net.
     * This provides an additional layer for container screens.
     */
    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // Re-validate on container open to catch any desync
        validateSlot(player);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Checks whether the given stack is the correct weapon for the champion.
     */
    private static boolean isCorrectWeapon(ItemStack stack, String championId) {
        if (championId.equals("no_champion")) {
            // No champion = slot should be empty (air)
            return stack.isEmpty();
        }

        if (stack.isEmpty()) return false;

        if (!(stack.getItem() instanceof com.eilai.runeterra.item.weapon.ChampionWeapon weapon)) {
            return false;
        }

        return weapon.getChampionId().equals(championId);
    }

    /**
     * Removes any copies of the champion weapon from slots 1-35
     * (to prevent duplication if someone somehow got a second copy).
     */
    private static void removeDuplicatesFrom(Inventory inv, String championId) {
        for (int i = 1; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof com.eilai.runeterra.item.weapon.ChampionWeapon w
                    && w.getChampionId().equals(championId)) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    /**
     * Forces slot 0 to contain the correct weapon for the player's current champion.
     */
    public static void validateSlot(ServerPlayer player) {
        PlayerChampionData data = PlayerChampionData.get(player);
        String champId = data.getChampionId();
        Inventory inv = player.getInventory();

        if (!isCorrectWeapon(inv.getItem(WEAPON_SLOT), champId)) {
            inv.setItem(WEAPON_SLOT, WeaponRegistry.createWeaponStack(champId));
        }
    }

    /**
     * Returns true if the given player is currently holding their champion weapon
     * (i.e. slot 0 is selected).
     */
    public static boolean isHoldingChampionWeapon(Player player) {
        return player.getInventory().selected == WEAPON_SLOT;
    }
}
