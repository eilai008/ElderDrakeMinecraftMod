package com.eilai.runeterra.events;

import com.eilai.runeterra.Runeterra;
import com.eilai.runeterra.init.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = Runeterra.MODID)
public class ModEvents {

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (entity.level().isClientSide()) return;
        if (!entity.hasEffect(ModEffects.TRUE_DAMAGE)) return;

        if (entity.tickCount % 20 == 0) {
            if (entity.level() instanceof ServerLevel serverLevel) {
                entity.hurtServer(serverLevel, entity.damageSources().magic(), 2.0f);
            }
        }
    }
}