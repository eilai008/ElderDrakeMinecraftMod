package com.eilai.runeterra.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.UUID;

/**
 * Intercepts player rendering to apply champion skin textures.
 *
 * Uses RenderPlayerEvent.Pre to cancel the default render,
 * then re-renders the player with the champion skin texture.
 *
 * This works for the local player in first-person and third-person view,
 * and also for other players on the same client (LAN/singleplayer).
 *
 * 1.21.11 correct approach:
 *  - No PlayerSkin class
 *  - Skin is an Identifier (renamed from ResourceLocation)
 *  - PlayerRenderer.getTextureLocation() returns the skin Identifier
 *  - We override by rendering with RenderType.entityTranslucent(skinId)
 */
@EventBusSubscriber(modid = "runeterra", value = Dist.CLIENT)
public class ChampionSkinRenderer {

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        UUID playerId = event.getEntity().getUUID();
        Identifier skinOverride = SkinManager.getOverride(playerId);
        if (skinOverride == null) return;

        // Cancel the default render
        event.setCanceled(true);

        // Re-render the player with our champion skin
        PlayerRenderer renderer = event.getRenderer();
        var poseStack       = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        int packedLight     = event.getPackedLight();
        var player          = event.getEntity();
        float partialTick   = event.getPartialTick();

        // Push pose, render with champion skin texture
        poseStack.pushPose();

        // Render each model layer with the override texture
        PlayerModel<net.minecraft.client.player.AbstractClientPlayer> model = renderer.getModel();

        // Render main body with champion skin
        var consumer = bufferSource.getBuffer(
                RenderType.entityTranslucent(skinOverride));

        model.renderToBuffer(poseStack, consumer, packedLight,
                net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,
                0xFFFFFFFF);

        poseStack.popPose();
    }
}