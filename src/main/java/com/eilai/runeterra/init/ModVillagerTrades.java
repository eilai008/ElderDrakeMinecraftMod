package com.eilai.runeterra.init;

import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

@EventBusSubscriber(modid = "runeterra")
public class ModVillagerTrades {

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {

        if (event.getType() != VillagerProfession.LIBRARIAN) return;

        event.getTrades().get(3).add((trader, random, level) ->
                new MerchantOffer(
                        new ItemCost(Items.EMERALD, 12),
                        new ItemStack(ModItems.SPELLBOOK.get()),
                        3,
                        10,
                        0.05f
                )
        );
    }
}