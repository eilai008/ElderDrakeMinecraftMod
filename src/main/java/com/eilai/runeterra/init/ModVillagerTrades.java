package com.eilai.runeterra.init;

import com.eilai.runeterra.item.SpellbookItem;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

/**
 * Adds the Spellbook to Librarian villager trades.
 *
 * Trade: 12 Emeralds → 1 Spellbook of the Rift (level 3 trade)
 */
@EventBusSubscriber(modid = "runeterra")
public class ModVillagerTrades {

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != VillagerProfession.LIBRARIAN) return;

        // Level 3 trade (index 2 = apprentice, 3 = journeyman, 4 = expert, 5 = master)
        event.getTrades().get(3).add((trader, random) ->
                new net.minecraft.world.entity.npc.MerchantOffer(
                        new ItemStack(Items.EMERALD, 12),   // cost
                        SpellbookItem.createSpellbookStack(ModItems.SPELLBOOK.get()), // result
                        3,    // max uses
                        10,   // villager XP
                        0.05f // price multiplier
                ));
    }
}
