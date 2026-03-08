package com.eilai.runeterra.entity.client;

import com.eilai.runeterra.entity.ElderDrakeEntity;
import net.minecraft.resources.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class ElderDrakeModel extends GeoModel<ElderDrakeEntity> {

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath("runeterra", "elder_drake");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        // Explicit full path since the texture is in a subdirectory
        return Identifier.fromNamespaceAndPath("runeterra", "textures/entity/elder_drake.png");
    }

    @Override
    public Identifier getAnimationResource(ElderDrakeEntity entity) {
        return Identifier.fromNamespaceAndPath("runeterra", "elder_drake");
    }
}