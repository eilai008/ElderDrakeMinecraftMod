package com.eilai.runeterra.entity.client;

import com.eilai.runeterra.entity.ScuttleCrabEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class ScuttleCrabRenderer<R extends LivingEntityRenderState & GeoRenderState>
        extends GeoEntityRenderer<ScuttleCrabEntity, R> {

    public ScuttleCrabRenderer(EntityRendererProvider.Context context) {
        super(context, new ScuttleCrabModel());
        this.shadowRadius = 0.6f;
    }
}
