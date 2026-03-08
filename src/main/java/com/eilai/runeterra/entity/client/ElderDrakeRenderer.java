package com.eilai.runeterra.entity.client;

import com.eilai.runeterra.entity.ElderDrakeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.renderer.internal.RenderPassInfo;

public class ElderDrakeRenderer<R extends LivingEntityRenderState & GeoRenderState>
        extends GeoEntityRenderer<ElderDrakeEntity, R> {

    public ElderDrakeRenderer(EntityRendererProvider.Context context) {
        super(context, new ElderDrakeModel());
        this.shadowRadius = 2.5f;
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<R> renderPassInfo, float widthScale, float heightScale) {
        super.scaleModelForRender(renderPassInfo, 8 * widthScale, 8 * heightScale);
    }
}