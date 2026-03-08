package com.eilai.runeterra.init;

import com.eilai.runeterra.Runeterra;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(Runeterra.MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Runeterra.MODID);

    public static final DeferredItem<SpawnEggItem> ELDER_DRAKE_SPAWN_EGG = ITEMS.registerItem(
            "elder_drake_spawn_egg",
            properties -> new SpawnEggItem(
                    properties.spawnEgg(ModEntities.ELDER_DRAKE.get())
            )
    );

    public static final Supplier<CreativeModeTab> RUNETERRA_TAB = CREATIVE_TABS.register(
            "runeterra_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.runeterra"))
                    .icon(() -> ELDER_DRAKE_SPAWN_EGG.get().getDefaultInstance())
                    .displayItems((params, output) -> output.accept(ELDER_DRAKE_SPAWN_EGG.get()))
                    .build()
    );

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}