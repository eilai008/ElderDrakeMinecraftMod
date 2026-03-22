package com.eilai.runeterra.client.hud;

import com.eilai.runeterra.champion.IChampionInventory;
import com.eilai.runeterra.champion.PlayerChampionData;
import com.eilai.runeterra.item.weapon.ChampionWeapon;
import com.eilai.runeterra.item.weapon.VayneWeaponItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = "runeterra", value = Dist.CLIENT)
public class WeaponSlotHud {

    private static final int SIZE   = 24;
    private static final int MARGIN = 6;

    @SubscribeEvent
    public static void onRenderHud(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        PlayerChampionData data = PlayerChampionData.get(mc.player);
        if (data.getChampionId().equals("no_champion")) return;

        ItemStack stack = ItemStack.EMPTY;
        if (mc.player.getInventory() instanceof IChampionInventory ci) {
            stack = ci.runeterra$getWeaponStack();
        }
        if (stack.isEmpty()) return;

        boolean selected   = data.isWeaponSlotSelected();
        boolean onCooldown = VayneWeaponItem.isOnCooldown(mc.player.getUUID());

        GuiGraphics gfx = event.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        // Position: bottom right, vertically centred with the hotbar
        int x = sw - SIZE - MARGIN;
        int y = sh - 22 + (22 - SIZE) / 2;

        // "0" key label ABOVE the slot — always visible so player knows the keybind
        gfx.drawCenteredString(mc.font,
                Component.literal("§70"),
                x + SIZE / 2, y - 9, 0xFFFFFFFF);

        // Background
        gfx.fill(x, y, x + SIZE, y + SIZE,
                selected ? 0xAA2A2A10 : 0xAA0A0A14);

        // Border — gold when selected, dark gold on cooldown, grey otherwise
        int border = onCooldown ? 0xFF666633
                : selected     ? 0xFFFFD700
                :                0xFF555577;
        gfx.fill(x,          y,          x + SIZE, y + 1,      border);
        gfx.fill(x,          y + SIZE-1, x + SIZE, y + SIZE,   border);
        gfx.fill(x,          y,          x + 1,    y + SIZE,   border);
        gfx.fill(x + SIZE-1, y,          x + SIZE, y + SIZE,   border);

        // Item icon
        gfx.renderItem(stack, x + 4, y + 4);

        // Cooldown overlay
        if (onCooldown) {
            gfx.fill(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, 0x99000000);
        }

        // "RMB" hint below when selected
        if (selected) {
            gfx.drawCenteredString(mc.font,
                    Component.literal("§eRMB"),
                    x + SIZE / 2, y + SIZE + 1, 0xFFFFFFFF);
        }

        // Weapon name above the "0" label when selected
        if (selected && stack.getItem() instanceof ChampionWeapon w) {
            String name = "§6" + w.getWeaponDisplayName();
            int nw = mc.font.width(name);
            gfx.fill(x + SIZE/2 - nw/2 - 1, y - 20,
                    x + SIZE/2 + nw/2 + 1,  y - 12, 0xAA000000);
            gfx.drawString(mc.font, Component.literal(name),
                    x + SIZE/2 - nw/2, y - 19, 0xFFFFFFFF);
        }
    }
}