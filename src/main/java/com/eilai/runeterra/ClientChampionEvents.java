package com.eilai.runeterra;

import com.eilai.runeterra.champion.PlayerChampionData;
import com.eilai.runeterra.client.SkinManager;
import com.eilai.runeterra.client.screen.ChampionSelectScreen;
import com.eilai.runeterra.init.ModKeybinds;
import com.eilai.runeterra.network.AbilityPacket;
import com.eilai.runeterra.network.ShootPacket;
import com.eilai.runeterra.network.ToggleWeaponSlotPacket;
import com.eilai.runeterra.network.UpgradeAbilityPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = "runeterra", value = Dist.CLIENT)
public class ClientChampionEvents {

    private static int ticksUntilOpen = -1;

    // Track last known state so we only fire on transition, not held
    private static boolean weaponSlotWasDown = false;

    // ── First join ────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        ticksUntilOpen = 20;
        weaponSlotWasDown = false;
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) SkinManager.applySkin(mc.player.getUUID(), "no_champion");
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (ticksUntilOpen < 0) return;
        if (--ticksUntilOpen > 0) return;
        ticksUntilOpen = -1;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        PlayerChampionData data = PlayerChampionData.get(mc.player);
        if (data.hasSelectedOnce() && !data.getChampionId().equals("no_champion")) {
            SkinManager.applySkin(mc.player.getUUID(), data.getChampionId());
        }
        if (!data.hasSelectedOnce()) {
            mc.execute(() -> mc.setScreen(new ChampionSelectScreen()));
        }
    }

    // ── Key input ─────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        if (event.getAction() != GLFW.GLFW_PRESS) return;

        boolean ctrl = (event.getModifiers() & GLFW.GLFW_MOD_CONTROL) != 0;

        if (ctrl) {
            if      (ModKeybinds.ABILITY_Q.consumeClick()) upgrade(0);
            else if (ModKeybinds.ABILITY_W.consumeClick()) upgrade(1);
            else if (ModKeybinds.ABILITY_E.consumeClick()) upgrade(2);
            else if (ModKeybinds.ABILITY_R.consumeClick()) upgrade(3);
            return;
        }

        // FIX: use the raw key code directly for WEAPON_SLOT toggle
        // consumeClick() can be eaten by vanilla hotbar slot 9 (also bound to 0)
        if (event.getKey() == GLFW.GLFW_KEY_0) {
            // Toggle client side immediately for instant HUD feedback
            PlayerChampionData.get(mc.player).toggleWeaponSlot();
            // Inform server so its copy stays in sync
            ClientPacketDistributor.sendToServer(new ToggleWeaponSlotPacket());
            return;
        }

        if      (ModKeybinds.ABILITY_Q.consumeClick()) send(0);
        else if (ModKeybinds.ABILITY_W.consumeClick()) send(1);
        else if (ModKeybinds.ABILITY_E.consumeClick()) send(2);
        else if (ModKeybinds.ABILITY_R.consumeClick()) send(3);
        else if (ModKeybinds.SPELL_D.consumeClick())   send(4);
        else if (ModKeybinds.SPELL_F.consumeClick())   send(5);
    }

    // ── Mouse input (RMB = shoot) ─────────────────────────────────────────────

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) return;

        PlayerChampionData data = PlayerChampionData.get(mc.player);
        if (data.getChampionId().equals("no_champion")) return;

        if (mc.hitResult != null &&
                mc.hitResult.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            return;
        }

        ClientPacketDistributor.sendToServer(new ShootPacket());
    }

    private static void send(int slot) {
        ClientPacketDistributor.sendToServer(new AbilityPacket(slot));
    }

    private static void upgrade(int slot) {
        ClientPacketDistributor.sendToServer(new UpgradeAbilityPacket(slot));
    }
}