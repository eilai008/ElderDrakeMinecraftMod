package com.eilai.runeterra;

import com.eilai.runeterra.init.ModEntities;
import com.eilai.runeterra.init.ModItems;
import com.eilai.runeterra.entity.ElderDrakeEntity;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@Mod(Runeterra.MODID)
public class Runeterra {

    public static final String MODID = "runeterra";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Runeterra(IEventBus modEventBus, ModContainer modContainer) {
        // Register our entities
        ModEntities.ENTITY_TYPES.register(modEventBus);

        // Register items (spawn egg + creative tab)
        ModItems.register(modEventBus);

        // Register attribute event (defines HP, speed etc)
        modEventBus.addListener(this::registerAttributes);

        // Register custom spawn placement rules (thunder, high mountains, no duplicates)
        modEventBus.addListener(ModEntities::registerSpawnPlacements);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ELDER_DRAKE.get(), ElderDrakeEntity.createAttributes().build());
    }
}