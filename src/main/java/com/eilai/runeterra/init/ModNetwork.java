package com.eilai.runeterra.init;

import com.eilai.runeterra.network.AbilityPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers all network packets for the mod.
 */
@EventBusSubscriber(modid = "runeterra", bus = EventBusSubscriber.Bus.MOD)
public class ModNetwork {

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // Client → Server: player pressed an ability key
        registrar.playToServer(
                AbilityPacket.TYPE,
                AbilityPacket.STREAM_CODEC,
                AbilityPacket::handle);
    }
}
