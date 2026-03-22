package com.eilai.runeterra.init;

import com.eilai.runeterra.item.weapon.ChampionWeapon;
import com.eilai.runeterra.item.weapon.GarenSwordItem;
import com.eilai.runeterra.item.weapon.VayneWeaponItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Central registry mapping champion ids to their weapon items.
 *
 * NOTE: Weapon constructors require Item.Properties, so they cannot be
 * used as bare method references (Supplier). Use lambdas instead:
 *   () -> new MyWeaponItem(new Item.Properties())
 */
public final class WeaponRegistry {

    private WeaponRegistry() {}

    private static final Map<String, Supplier<? extends ChampionWeapon>> WEAPONS = new HashMap<>();

    static {
        WEAPONS.put("garen", GarenSwordItem::new);
        WEAPONS.put("vayne", () -> new VayneWeaponItem(new Item.Properties()));
    }

    public static Optional<ChampionWeapon> getWeapon(String championId) {
        Supplier<? extends ChampionWeapon> supplier = WEAPONS.get(championId);
        return supplier != null ? Optional.of(supplier.get()) : Optional.empty();
    }

    public static ItemStack createWeaponStack(String championId) {
        return getWeapon(championId)
                .map(ChampionWeapon::createStack)
                .orElse(ItemStack.EMPTY);
    }

    public static boolean hasWeapon(String championId) {
        return WEAPONS.containsKey(championId);
    }

    public static void register(String championId, Supplier<? extends ChampionWeapon> supplier) {
        WEAPONS.put(championId, supplier);
    }
}