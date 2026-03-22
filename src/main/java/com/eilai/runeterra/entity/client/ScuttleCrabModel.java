package com.eilai.runeterra.entity.client;

import com.eilai.runeterra.entity.ScuttleCrabEntity;
import net.minecraft.resources.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class ScuttleCrabModel extends GeoModel<ScuttleCrabEntity> {

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath("runeterra", "scuttle_crab");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath("runeterra", "textures/entity/scuttle_crab.png");
    }

    @Override
    public Identifier getAnimationResource(ScuttleCrabEntity entity) {
        return Identifier.fromNamespaceAndPath("runeterra", "scuttle_crab");
    }
}
