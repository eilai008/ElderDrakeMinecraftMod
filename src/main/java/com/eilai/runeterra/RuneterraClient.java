package com.eilai.runeterra;

import com.eilai.runeterra.entity.client.ElderDrakeFireballRenderer;
import com.eilai.runeterra.entity.client.ElderDrakeRenderer;
import com.eilai.runeterra.entity.client.ScuttleCrabRenderer;
import com.eilai.runeterra.init.ModEntities;
import com.eilai.runeterra.client.ChampionSkinLayer;
import com.eilai.runeterra.init.ModKeybinds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Runeterra.MODID, dist = Dist.CLIENT)
// This version of NeoForge auto-routes to the correct bus:
// events implementing IModBusEvent go to the mod bus automatically.
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
        EntityRenderers.register(ModEntities.ELDER_DRAKE_FIREBALL.get(), ElderDrakeFireballRenderer::new);
        EntityRenderers.register(ModEntities.SCUTTLE_CRAB.get(), ScuttleCrabRenderer::new);
    }

    @SubscribeEvent
    static void onAddPlayerLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerModelType type : event.getSkins()) {
            AvatarRenderer<?> playerRenderer = event.getPlayerRenderer(type);
            if (playerRenderer != null) {
                playerRenderer.addLayer(new ChampionSkinLayer(playerRenderer, event.getEntityModels()));
            }
        }
    }

    @SubscribeEvent
    static void onRegisterKeybinds(RegisterKeyMappingsEvent event) {
        event.register(ModKeybinds.ABILITY_Q);
        event.register(ModKeybinds.ABILITY_W);
        event.register(ModKeybinds.ABILITY_E);
        event.register(ModKeybinds.ABILITY_R);
        event.register(ModKeybinds.SPELL_D);
        event.register(ModKeybinds.SPELL_F);
        event.register(ModKeybinds.SHOOT);
        event.register(ModKeybinds.WEAPON_SLOT);
    }
}