package com.eilai.runeterra.events;

import com.eilai.runeterra.champion.PlayerChampionData;
import com.eilai.runeterra.item.weapon.VayneWeaponItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.eilai.runeterra.network.SyncChampionDataPacket;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Intercepts right-click (use item / interact) for Vayne players.
 *
 * When Vayne right-clicks with an empty hand or any item in mainhand,
 * we fire the weapon arrow regardless of what's actually held.
 * The weapon is HUD-only so there's nothing to "use" — we just fire.
 *
 * Uses PlayerInteractEvent.RightClickEmpty for right-click with empty hand,
 * and PlayerInteractEvent.RightClickItem as fallback.
 */
@EventBusSubscriber(modid = "runeterra")
public class VayneRightClickHandler {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        handleFire(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        // Don't intercept if they're right-clicking with a specific mod item
        // (e.g. Champion Crystal, Spellbook) — let those through
        net.minecraft.world.item.ItemStack held = player.getMainHandItem();
        if (!held.isEmpty()
                && (held.getItem() instanceof com.eilai.runeterra.item.ChampionCrystalItem
                    || held.getItem() instanceof com.eilai.runeterra.item.SpellbookItem)) {
            return;
        }
        handleFire(player);
    }

    private static void handleFire(ServerPlayer player) {
        PlayerChampionData data = PlayerChampionData.get(player);
        if (!data.getChampionId().equals("vayne")) return;

        boolean fired = VayneWeaponItem.tryFire(player);
        if (fired) {
            // Sync cooldown to client so HUD updates immediately
            PacketDistributor.sendToPlayer(player, SyncChampionDataPacket.from(player));
        }
    }
}
