package com.eilai.runeterra.item.weapon;

import net.minecraft.world.item.Item;

/**
 * Garen's weapon — "Judgment" (his giant greatsword).
 *
 * Stats:
 *  - Attack Damage : 8.0  (high damage, tanky bruiser)
 *  - Attack Speed  : 1.4  (slow, heavy swings)
 *  - Attack Range  : 4.0  (slightly longer reach than vanilla)
 *
 * Template: copy this class for every champion weapon.
 * Steps to create a new weapon:
 *  1. Copy this file, rename to <ChampName>WeaponItem.java
 *  2. Change championId, weaponDisplayName, and the three stat values
 *  3. Register it in WeaponRegistry
 *  4. Add your custom 3D model JSON to:
 *       assets/runeterra/models/item/weapon/<champion_id>.json
 *  5. Add your texture PNG to:
 *       assets/runeterra/textures/item/weapon/<champion_id>.png
 *  6. Add the item model override in:
 *       assets/runeterra/items/<champion_id>_weapon.json
 */
public class GarenSwordItem extends ChampionWeapon {

    public GarenSwordItem() {
        super(
            "garen",                    // championId — must match ChampionRegistry id
            "Judgment",                 // weapon display name
            8.0,                        // attackDamage
            1.4,                        // attackSpeed (attacks per second)
            4.0,                        // attackRange (blocks)
            new Properties()
        );
    }
}
