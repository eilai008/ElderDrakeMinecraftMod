package com.eilai.runeterra.mixin;

import com.eilai.runeterra.champion.IChampionInventory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the champion weapon slot participate in getMainHandItem()
 * when the player has the weapon "selected" via the weapon slot HUD.
 *
 * NOTE: getMainHandItem() is defined on LivingEntity, not Player,
 * so we must target LivingEntity. We guard against non-Player entities
 * at runtime so other LivingEntities are unaffected.
 */
@Mixin(LivingEntity.class)
public class PlayerMixin {

    @Inject(method = "getMainHandItem", at = @At("HEAD"), cancellable = true, remap = false)
    private void runeterra$getMainHandItem(CallbackInfoReturnable<ItemStack> cir) {
        // Only apply to players
        if (!((Object)this instanceof Player player)) return;

        if (!(player.getInventory() instanceof IChampionInventory ci)) return;
        if (!ci.runeterra$hasWeapon()) return;

        com.eilai.runeterra.champion.PlayerChampionData data =
                com.eilai.runeterra.champion.PlayerChampionData.get(player);
        if (!data.isWeaponSlotSelected()) return;

        cir.setReturnValue(ci.runeterra$getWeaponStack());
    }
}