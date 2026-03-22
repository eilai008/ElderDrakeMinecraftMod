package com.eilai.runeterra;

import com.eilai.runeterra.champion.MobXPConfig;
import com.eilai.runeterra.champion.PlayerChampionData;
import com.eilai.runeterra.init.ModNetwork;
import com.eilai.runeterra.init.ModEffects;
import com.eilai.runeterra.init.ModEntities;
import com.eilai.runeterra.init.ModItems;
import com.eilai.runeterra.entity.ElderDrakeEntity;
import com.eilai.runeterra.entity.ScuttleCrabEntity;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@Mod(Runeterra.MODID)
public class Runeterra {

    public static final String MODID = "runeterra";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public Runeterra(IEventBus modEventBus, ModContainer modContainer) {
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModItems.register(modEventBus);
        ModEffects.MOB_EFFECTS.register(modEventBus);

        // Champion system
        PlayerChampionData.ATTACHMENT_TYPES.register(modEventBus);
        MobXPConfig.load(); // Load mob XP config from file

        // Register network packets on the mod event bus
        modEventBus.addListener(ModNetwork::onRegisterPayloads);

        modEventBus.addListener(this::registerAttributes);
        modEventBus.addListener(ModEntities::registerSpawnPlacements);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ELDER_DRAKE.get(), ElderDrakeEntity.createAttributes().build());
        event.put(ModEntities.SCUTTLE_CRAB.get(), ScuttleCrabEntity.createAttributes().build());
    }
}