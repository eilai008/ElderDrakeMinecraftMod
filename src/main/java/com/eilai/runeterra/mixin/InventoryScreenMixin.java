package com.eilai.runeterra.mixin;

import com.eilai.runeterra.champion.IChampionInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {

    @Inject(method = "render", at = @At("RETURN"), remap = false)
    private void runeterra$renderWeaponSlot(GuiGraphics gfx, int mouseX, int mouseY,
                                            float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (!(mc.player.getInventory() instanceof IChampionInventory ci2)) return;
        ItemStack weapon = ci2.runeterra$getWeaponStack();
        if (weapon.isEmpty()) return;

        InventoryScreen screen = (InventoryScreen)(Object)this;
        int guiLeft = (screen.width - 176) / 2;
        int guiTop  = (screen.height - 166) / 2;

        int slotX = guiLeft + 155;
        int slotY = guiTop  + 75;

        gfx.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF8B6914);
        gfx.fill(slotX,     slotY,     slotX + 16, slotY + 16, 0xFF1A1A2E);

        gfx.renderItem(weapon, slotX, slotY);
        gfx.renderItemDecorations(mc.font, weapon, slotX, slotY);

        gfx.drawString(mc.font,
                net.minecraft.network.chat.Component.literal("§6W"),
                slotX + 5, slotY - 9, 0xFFFFFFFF, false);

        if (mouseX >= slotX && mouseX < slotX + 16
                && mouseY >= slotY && mouseY < slotY + 16) {
            gfx.fill(slotX, slotY, slotX + 16, slotY + 16, 0x80FFFFFF);
        }
    }
}