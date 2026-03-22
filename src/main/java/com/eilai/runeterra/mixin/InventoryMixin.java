package com.eilai.runeterra.mixin;

import com.eilai.runeterra.champion.IChampionInventory;
import com.eilai.runeterra.item.weapon.ChampionWeapon;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public class InventoryMixin implements IChampionInventory {

    private static final int WEAPON_SLOT_SENTINEL = 99;

    @Unique
    private ItemStack runeterra$weaponStack = ItemStack.EMPTY;

    @Override
    public ItemStack runeterra$getWeaponStack() {
        return runeterra$weaponStack;
    }

    @Override
    public void runeterra$setWeaponStack(ItemStack stack) {
        this.runeterra$weaponStack = stack == null ? ItemStack.EMPTY : stack;
    }

    @Override
    public boolean runeterra$hasWeapon() {
        return !runeterra$weaponStack.isEmpty()
                && runeterra$weaponStack.getItem() instanceof ChampionWeapon;
    }

    @Inject(method = "save", at = @At("RETURN"), remap = false)
    private void runeterra$save(ValueOutput.TypedOutputList<ItemStackWithSlot> output,
                                CallbackInfo ci) {
        if (!runeterra$weaponStack.isEmpty()) {
            output.add(new ItemStackWithSlot(WEAPON_SLOT_SENTINEL, runeterra$weaponStack));
        }
    }

    @Inject(method = "load", at = @At("RETURN"), remap = false)
    private void runeterra$load(ValueInput.TypedInputList<ItemStackWithSlot> input,
                                CallbackInfo ci) {
        for (ItemStackWithSlot entry : input) {
            if (entry.slot() == WEAPON_SLOT_SENTINEL) {
                runeterra$weaponStack = entry.stack();
                return;
            }
        }
        runeterra$weaponStack = ItemStack.EMPTY;
    }

    @Inject(method = "dropAll", at = @At("RETURN"), remap = false)
    private void runeterra$clearWeaponOnDeath(CallbackInfo ci) {
        runeterra$weaponStack = ItemStack.EMPTY;
    }
}