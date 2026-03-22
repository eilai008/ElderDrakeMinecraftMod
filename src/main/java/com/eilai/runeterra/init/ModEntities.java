package com.eilai.runeterra.init;

import com.eilai.runeterra.entity.ElderDrakeEntity;
import com.eilai.runeterra.entity.ElderDrakeFireball;
import com.eilai.runeterra.entity.ScuttleCrabEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntities {

    public static final DeferredRegister.Entities ENTITY_TYPES =
            DeferredRegister.createEntities("runeterra");

    public static final Supplier<EntityType<ElderDrakeEntity>> ELDER_DRAKE =
            ENTITY_TYPES.registerEntityType("elder_drake", ElderDrakeEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(2.5f, 2.0f).clientTrackingRange(12));

    public static final Supplier<EntityType<ElderDrakeFireball>> ELDER_DRAKE_FIREBALL =
            ENTITY_TYPES.registerEntityType("elder_drake_fireball", ElderDrakeFireball::new, MobCategory.MISC,
                    builder -> builder.sized(1.0f, 1.0f));

    public static final Supplier<EntityType<ScuttleCrabEntity>> SCUTTLE_CRAB =
            ENTITY_TYPES.registerEntityType("scuttle_crab", ScuttleCrabEntity::new, MobCategory.CREATURE,
                    builder -> builder.sized(1.2f, 0.8f).clientTrackingRange(10));

    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
                ELDER_DRAKE.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ElderDrakeEntity::checkElderDrakeSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.OR
        );
        event.register(
                SCUTTLE_CRAB.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.OR
        );
    }
}