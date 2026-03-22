package com.eilai.runeterra.mixin;

import com.eilai.runeterra.champion.IChampionInventory;
import com.eilai.runeterra.champion.PlayerChampionData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders the champion weapon slot in the inventory screen.
 * Draws a custom slot to the right of the offhand slot.
 */
@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {

    @Inject(method = "render", at = @At("RETURN"))
    private void runeterra$renderWeaponSlot(GuiGraphics gfx, int mouseX, int mouseY,
                                             float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (!(mc.player.getInventory() instanceof IChampionInventory ci2)) return;
        ItemStack weapon = ci2.runeterra$getWeaponStack();
        if (weapon.isEmpty()) return;

        // Position: to the right of the standard inventory layout
        // Vanilla inventory GUI is centered — offset weapon slot to a fixed position
        InventoryScreen screen = (InventoryScreen)(Object)this;
        int guiLeft = (screen.width - 176) / 2;
        int guiTop  = (screen.height - 166) / 2;

        // Place weapon slot below the offhand area (roughly x+155, y+75)
        int slotX = guiLeft + 155;
        int slotY = guiTop  + 75;

        // Slot background
        gfx.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF8B6914);
        gfx.fill(slotX,     slotY,     slotX + 16, slotY + 16, 0xFF1A1A2E);

        // Render item
        gfx.renderItem(weapon, slotX, slotY);
        gfx.renderItemDecorations(mc.font, weapon, slotX, slotY);

        // Label
        gfx.drawString(mc.font,
                net.minecraft.network.chat.Component.literal("§6W"),
                slotX + 5, slotY - 9, 0xFFFFFFFF, false);

        // Highlight on hover
        if (mouseX >= slotX && mouseX < slotX + 16
                && mouseY >= slotY && mouseY < slotY + 16) {
            gfx.fill(slotX, slotY, slotX + 16, slotY + 16, 0x80FFFFFF);
        }
    }
}
