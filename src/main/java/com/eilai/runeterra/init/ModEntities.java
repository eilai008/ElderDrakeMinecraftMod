package com.eilai.runeterra.init;

import com.eilai.runeterra.entity.ElderDrakeEntity;
import com.eilai.runeterra.entity.ElderDrakeFireball;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
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
                    // Hitbox: 2.5 wide x 2.0 tall (in blocks)
                    // At 8x visual scale this matches the model body well
                    builder -> builder.sized(2.5f, 2.0f).clientTrackingRange(12));

    public static final Supplier<EntityType<ElderDrakeFireball>> ELDER_DRAKE_FIREBALL =
            ENTITY_TYPES.registerEntityType("elder_drake_fireball", ElderDrakeFireball::new, MobCategory.MISC,
                    builder -> builder.sized(1.0f, 1.0f));

    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
                ELDER_DRAKE.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ElderDrakeEntity::checkElderDrakeSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.OR
        );
    }
}