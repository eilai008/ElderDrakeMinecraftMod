package com.eilai.runeterra.client.hud;

import com.eilai.runeterra.champion.ChampionWeaponSlot;
import com.eilai.runeterra.champion.PlayerChampionData;
import com.eilai.runeterra.item.weapon.ChampionWeapon;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Renders the champion weapon slot as a custom HUD element.
 *
 * Layout: a single slot rendered above and slightly left of the hotbar,
 * aligned with slot 0. It has:
 *  - A gold border when selected (slot 0 is active)
 *  - A grey locked border when not selected
 *  - A lock icon overlay for "no_champion" (empty placeholder)
 *  - The weapon item rendered inside
 *  - "0" key hint label below the slot
 *  - Weapon name tooltip above the slot when selected
 */
@EventBusSubscriber(modid = "runeterra", value = Dist.CLIENT)
public class WeaponSlotHud {

    // Slot dimensions
    private static final int SLOT_SIZE    = 22;
    private static final int BORDER       = 2;

    // Colors
    private static final int COLOR_BORDER_SELECTED   = 0xFFFFD700; // gold
    private static final int COLOR_BORDER_NORMAL      = 0xFF888888; // grey
    private static final int COLOR_BORDER_LOCKED      = 0xFF444444; // dark grey
    private static final int COLOR_BG                 = 0xFF1A1A2E;
    private static final int COLOR_BG_SELECTED        = 0xFF2A2A1E;

    @SubscribeEvent
    public static void onRenderHud(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        Player player = mc.player;
        GuiGraphics gfx = event.getGuiGraphics();

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        // The vanilla hotbar starts at:
        int hotbarX = (screenW - 182) / 2;
        int hotbarY = screenH - 22;

        // Our weapon slot sits directly above slot 0 of the hotbar
        // Slot 0 center in vanilla hotbar = hotbarX + 9
        int slotX = hotbarX + 1; // align left edge with vanilla slot 0
        int slotY = hotbarY - SLOT_SIZE - 6; // 6px gap above hotbar

        boolean selected = player.getInventory().selected == ChampionWeaponSlot.WEAPON_SLOT;
        PlayerChampionData data = PlayerChampionData.get(player);
        String champId = data.getChampionId();
        boolean noChamp = champId.equals("no_champion");

        ItemStack weaponStack = player.getInventory().getItem(ChampionWeaponSlot.WEAPON_SLOT);
        boolean hasWeapon = !weaponStack.isEmpty()
                && weaponStack.getItem() instanceof ChampionWeapon;

        // ── Slot background ────────────────────────────────────────────────
        int bgColor     = selected ? COLOR_BG_SELECTED : COLOR_BG;
        int borderColor = noChamp  ? COLOR_BORDER_LOCKED
                        : selected ? COLOR_BORDER_SELECTED
                        :            COLOR_BORDER_NORMAL;

        // Fill background
        gfx.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, bgColor);

        // Draw border (1px outline)
        gfx.fill(slotX,                slotY,                slotX + SLOT_SIZE, slotY + 1,           borderColor); // top
        gfx.fill(slotX,                slotY + SLOT_SIZE - 1,slotX + SLOT_SIZE, slotY + SLOT_SIZE,   borderColor); // bottom
        gfx.fill(slotX,                slotY,                slotX + 1,         slotY + SLOT_SIZE,   borderColor); // left
        gfx.fill(slotX + SLOT_SIZE - 1,slotY,                slotX + SLOT_SIZE, slotY + SLOT_SIZE,   borderColor); // right

        // ── Render weapon item or lock icon ────────────────────────────────
        if (hasWeapon) {
            gfx.renderItem(weaponStack, slotX + BORDER, slotY + BORDER);
            gfx.renderItemDecorations(mc.font, weaponStack, slotX + BORDER, slotY + BORDER);
        } else {
            // Draw a lock symbol for no_champion or missing weapon
            gfx.drawCenteredString(mc.font,
                    Component.literal("§7🔒"),
                    slotX + SLOT_SIZE / 2,
                    slotY + SLOT_SIZE / 2 - 4,
                    0xFFFFFF);
        }

        // ── "0" key label below the slot ──────────────────────────────────
        gfx.drawCenteredString(mc.font,
                Component.literal(selected ? "§e0" : "§70"),
                slotX + SLOT_SIZE / 2,
                slotY + SLOT_SIZE + 1,
                0xFFFFFF);

        // ── Weapon name above slot when selected ───────────────────────────
        if (selected && hasWeapon && weaponStack.getItem() instanceof ChampionWeapon weapon) {
            String name = "§6" + weapon.getWeaponDisplayName();
            int nameW = mc.font.width(name);
            gfx.drawString(mc.font,
                    Component.literal(name),
                    slotX + SLOT_SIZE / 2 - nameW / 2,
                    slotY - 10,
                    0xFFFFFF);
        }

        // ── "CHAMPION WEAPON" label — shown briefly on equip (optional) ────
        // This is rendered as a small label above the slot always, greyed out
        gfx.drawCenteredString(mc.font,
                Component.literal("§8Champion"),
                slotX + SLOT_SIZE / 2,
                slotY - 18,
                0xFFFFFF);
    }
}
