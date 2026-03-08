package com.eilai.runeterra.entity.client;

import com.eilai.runeterra.entity.ElderDrakeFireball;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.NoopRenderer;

/**
 * Renderer for the Elder Drake fireball — NeoForge 1.21.11 / GeckoLib 5.
 *
 * Extends NoopRenderer so the entity registers without crashing (no null renderer).
 * The fireball is invisible but still has a hitbox and deals damage correctly.
 *
 * NOTE: If you want a visible fireball in the future, the correct approach in
 * NeoForge 1.21.11 is to use particles spawned server-side via
 * level.addParticle() in ElderDrakeFireball#tick(), since the raw SubmitNodeCollector
 * pipeline is not publicly accessible without knowing the exact vanilla package layout.
 */
public class ElderDrakeFireballRenderer extends NoopRenderer<ElderDrakeFireball> {

    public ElderDrakeFireballRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
}