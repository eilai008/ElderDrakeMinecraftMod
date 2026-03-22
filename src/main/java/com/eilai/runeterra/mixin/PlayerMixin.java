package com.eilai.runeterra.mixin;

import com.eilai.runeterra.champion.IChampionInventory;
import com.eilai.runeterra.item.weapon.ChampionWeapon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the champion weapon slot participate in Player.getMainHandItem()
 * when the player has the weapon "selected" via the weapon slot HUD.
 *
 * This allows the weapon to show in the player's hand visually
 * and makes attack damage apply correctly.
 */
@Mixin(Player.class)
public class PlayerMixin {

    /**
     * If the player's selected hotbar slot holds nothing but their
     * champion inventory has a weapon, return the weapon as mainhand item.
     *
     * This is only applied when the player has explicitly "selected"
     * the weapon slot (tracked via PlayerChampionData.isWeaponSelected).
     */
    @Inject(method = "getMainHandItem", at = @At("HEAD"), cancellable = true)
    private void runeterra$getMainHandItem(CallbackInfoReturnable<ItemStack> cir) {
        Player player = (Player)(Object)this;
        if (!(player.getInventory() instanceof IChampionInventory ci)) return;
        if (!ci.runeterra$hasWeapon()) return;

        // Only override if weapon slot is "selected" in champion data
        com.eilai.runeterra.champion.PlayerChampionData data =
                com.eilai.runeterra.champion.PlayerChampionData.get(player);
        if (!data.isWeaponSlotSelected()) return;

        cir.setReturnValue(ci.runeterra$getWeaponStack());
    }
}
