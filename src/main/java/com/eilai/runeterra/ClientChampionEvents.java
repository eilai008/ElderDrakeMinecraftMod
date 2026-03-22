package com.eilai.runeterra.client;

import com.eilai.runeterra.champion.PlayerChampionData;
import com.eilai.runeterra.client.screen.ChampionSelectScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Client-only champion events.
 *
 * Screen opening fix:
 *  LoggingIn fires too early — player data attachments aren't loaded yet.
 *  Instead we set a flag on login, then wait 20 ticks (1 second) in the
 *  level tick before opening the screen. By then everything is loaded.
 */
@EventBusSubscriber(modid = "runeterra", value = Dist.CLIENT)
public class ClientChampionEvents {

    private static int ticksUntilOpen = -1;

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        // Schedule screen check for 20 ticks after login
        ticksUntilOpen = 20;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (ticksUntilOpen < 0) return;
        ticksUntilOpen--;
        if (ticksUntilOpen > 0) return;

        // Reset counter
        ticksUntilOpen = -1;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Now data is loaded — check if player needs to select a champion
        PlayerChampionData data = PlayerChampionData.get(mc.player);
        if (!data.hasSelectedOnce()) {
            mc.execute(() -> mc.setScreen(new ChampionSelectScreen()));
        }
    }
}