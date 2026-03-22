package com.eilai.runeterra.init;

import com.eilai.runeterra.item.weapon.ChampionWeapon;
import com.eilai.runeterra.item.weapon.GarenSwordItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Central registry mapping champion ids to their weapon items.
 *
 * HOW TO ADD A NEW CHAMPION WEAPON:
 *  1. Create your weapon class extending ChampionWeapon
 *  2. Register it in ModItems (see bottom of this file for the pattern)
 *  3. Add an entry in the WEAPONS map below pointing to your ModItems supplier
 *
 * All weapons are registered as proper mod items via ModItems so they
 * show up in creative tabs, have proper item ids, and work with datapacks.
 */
public final class WeaponRegistry {

    private WeaponRegistry() {}

    /**
     * Map of championId → Supplier<ChampionWeapon>.
     * Using Supplier so items are lazily resolved after registration.
     *
     * Add every champion weapon here.
     */
    private static final Map<String, Supplier<? extends ChampionWeapon>> WEAPONS = new HashMap<>();

    static {
        // ── Register weapons here as you implement them ───────────────────────
        // Pattern: WEAPONS.put("champion_id", ModItems.CHAMPION_WEAPON::get);
        //
        // Example (uncomment when GarenSwordItem is registered in ModItems):
        // WEAPONS.put("garen", ModItems.GAREN_WEAPON::get);

        // Placeholder — Garen registered for demonstration
        // Remove this line and use the ModItems version once wired up:
        WEAPONS.put("garen", GarenSwordItem::new);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns the weapon item for a given champion id, if one exists.
     */
    public static Optional<ChampionWeapon> getWeapon(String championId) {
        Supplier<? extends ChampionWeapon> supplier = WEAPONS.get(championId);
        return supplier != null ? Optional.of(supplier.get()) : Optional.empty();
    }

    /**
     * Creates a fresh ItemStack for the given champion's weapon.
     * Returns an empty stack if the champion has no weapon registered yet.
     */
    public static ItemStack createWeaponStack(String championId) {
        return getWeapon(championId)
                .map(ChampionWeapon::createStack)
                .orElse(ItemStack.EMPTY);
    }

    /**
     * Returns true if a weapon is registered for this champion.
     */
    public static boolean hasWeapon(String championId) {
        return WEAPONS.containsKey(championId);
    }

    /**
     * Registers a weapon supplier. Call this from ModItems after
     * DeferredRegister fires, so the items are fully initialized.
     *
     * Usage in ModItems:
     *   public static final Supplier<GarenSwordItem> GAREN_WEAPON =
     *       ITEMS.register("garen_weapon", GarenSwordItem::new);
     *
     *   // Then in your mod init:
     *   WeaponRegistry.register("garen", ModItems.GAREN_WEAPON::get);
     */
    public static void register(String championId, Supplier<? extends ChampionWeapon> supplier) {
        WEAPONS.put(championId, supplier);
    }
}
