package com.eilai.runeterra.champion;

import com.eilai.runeterra.init.WeaponRegistry;
import com.eilai.runeterra.item.weapon.ChampionWeapon;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Manages the champion weapon stored in the custom mixin slot.
 * Uses IChampionInventory (injected into Inventory by InventoryMixin).
 */
@EventBusSubscriber(modid = "runeterra")
public class ChampionWeaponSlot {

    // ── Tick: ensure weapon is always in the mixin slot ───────────────────────

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.getInventory() instanceof IChampionInventory ci)) return;

        PlayerChampionData data = PlayerChampionData.get(player);
        String champId = data.getChampionId();

        if (!WeaponRegistry.hasWeapon(champId)) {
            // No weapon for this champion — clear slot
            ci.runeterra$setWeaponStack(ItemStack.EMPTY);
            return;
        }

        ItemStack current = ci.runeterra$getWeaponStack();
        if (!isCorrectWeapon(current, champId)) {
            ci.runeterra$setWeaponStack(WeaponRegistry.createWeaponStack(champId));
        }
    }

    // ── Champion changed ──────────────────────────────────────────────────────

    public static void onChampionChanged(ServerPlayer player, String newChampionId) {
        if (!(player.getInventory() instanceof IChampionInventory ci)) return;
        if (WeaponRegistry.hasWeapon(newChampionId)) {
            ci.runeterra$setWeaponStack(WeaponRegistry.createWeaponStack(newChampionId));
        } else {
            ci.runeterra$setWeaponStack(ItemStack.EMPTY);
        }
    }

    public static void validateSlot(ServerPlayer player) {
        onChampionChanged(player, PlayerChampionData.get(player).getChampionId());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static boolean isCorrectWeapon(ItemStack stack, String champId) {
        if (stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof ChampionWeapon w)) return false;
        return w.getChampionId().equals(champId);
    }

    public static boolean isHoldingChampionWeapon(Player player) {
        return player.getInventory() instanceof IChampionInventory ci
                && ci.runeterra$hasWeapon()
                && PlayerChampionData.get(player).isWeaponSlotSelected();
    }

    public static ItemStack getWeaponStack(Player player) {
        if (player.getInventory() instanceof IChampionInventory ci) {
            return ci.runeterra$getWeaponStack();
        }
        return ItemStack.EMPTY;
    }
}