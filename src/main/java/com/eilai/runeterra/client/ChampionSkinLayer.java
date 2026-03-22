package com.eilai.runeterra.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Render layer that draws champion skin over the player.
 *
 * UUID strategy: find the player entity whose render state name matches,
 * falling back to iterating all loaded players.
 */
public class ChampionSkinLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    public ChampionSkinLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent,
                             EntityModelSet entityModelSet) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack,
                       SubmitNodeCollector collector,
                       int packedLight,
                       AvatarRenderState renderState,
                       float yRot,
                       float xRot) {

        UUID playerId = getUUIDFromState(renderState);
        if (playerId == null) return;

        Identifier skin = SkinManager.getOverride(playerId);
        if (skin == null) return;

        PlayerModel model = this.getParentModel();
        model.setupAnim(renderState);
        coloredCutoutModelCopyLayerRender(
                model, skin, poseStack, collector, packedLight, renderState, -1, 0);
    }

    private static UUID getUUIDFromState(AvatarRenderState renderState) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;

        // Strategy 1: check if this is the local player
        if (mc.player != null && renderState.id == mc.player.getId()) {
            return mc.player.getUUID();
        }

        // Strategy 2: find by entity ID among all loaded players
        for (Player p : mc.level.players()) {
            if (p.getId() == renderState.id) {
                return p.getUUID();
            }
        }

        // Strategy 3: fall back to name tag match (works when name tag is visible)
        try {
            if (mc.getConnection() != null && renderState.nameTag != null) {
                String name = renderState.nameTag.getString();
                if (!name.isEmpty()) {
                    return mc.getConnection().getOnlinePlayers().stream()
                            .filter(p -> p.getProfile().name().equals(name))
                            .map(p -> p.getProfile().id())
                            .findFirst()
                            .orElse(null);
                }
            }
        } catch (Exception ignored) {}

        return null;
    }
}