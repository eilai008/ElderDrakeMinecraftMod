package com.eilai.runeterra.mixin;

import com.eilai.runeterra.champion.IChampionInventory;
import com.eilai.runeterra.item.weapon.ChampionWeapon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds a single champion weapon slot to the player's inventory.
 *
 * The slot is stored separately from vanilla slots and serialized
 * under the "ChampionWeapon" NBT key so it persists across sessions.
 */
@Mixin(Inventory.class)
public class InventoryMixin implements IChampionInventory {

    @Unique
    private ItemStack runeterra$weaponStack = ItemStack.EMPTY;

    // ── IChampionInventory implementation ─────────────────────────────────────

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

    // ── Serialize weapon slot alongside vanilla inventory ─────────────────────

    @Inject(method = "save", at = @At("RETURN"))
    private void runeterra$save(ListTag listTag, CallbackInfoReturnable<ListTag> cir) {
        if (!runeterra$weaponStack.isEmpty()) {
            CompoundTag tag = new CompoundTag();
            tag.putByte("Slot", (byte) 99); // sentinel slot number
            runeterra$weaponStack.save(tag);
            listTag.add(tag);
        }
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void runeterra$load(ListTag listTag, CallbackInfo ci) {
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag tag = listTag.getCompound(i);
            if (tag.getByte("Slot") == (byte) 99) {
                runeterra$weaponStack = ItemStack.parseOptional(
                        ((Inventory)(Object)this).player.registryAccess(), tag);
                break;
            }
        }
    }

    // ── Prevent dropping weapon if player dies / loses it ─────────────────────

    @Inject(method = "dropAll", at = @At("RETURN"))
    private void runeterra$clearWeaponOnDeath(CallbackInfo ci) {
        // Don't drop weapon — it's a permanent champion item
        runeterra$weaponStack = ItemStack.EMPTY;
    }
}
