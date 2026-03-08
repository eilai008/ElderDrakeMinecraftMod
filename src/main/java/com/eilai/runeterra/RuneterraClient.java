package com.eilai.runeterra;

import com.eilai.runeterra.entity.client.ElderDrakeFireballRenderer;
import com.eilai.runeterra.entity.client.ElderDrakeRenderer;
import com.eilai.runeterra.init.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Runeterra.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Runeterra.MODID, value = Dist.CLIENT)
public class RuneterraClient {

    public RuneterraClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        Runeterra.LOGGER.info("HELLO FROM CLIENT SETUP");
        Runeterra.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

        EntityRenderers.register(ModEntities.ELDER_DRAKE.get(), ElderDrakeRenderer::new);

        // NoopRenderer-based: the fireball is invisible but fully functional.
        // Visual appearance comes from particles spawned in ElderDrakeFireball#tick().
        EntityRenderers.register(ModEntities.ELDER_DRAKE_FIREBALL.get(), ElderDrakeFireballRenderer::new);
    }
}