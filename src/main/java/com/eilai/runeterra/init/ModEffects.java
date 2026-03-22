package com.eilai.runeterra.init;

import com.eilai.runeterra.Runeterra;
import com.eilai.runeterra.effect.TrueDamageEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Runeterra.MODID);

    public static final DeferredHolder<MobEffect, TrueDamageEffect> TRUE_DAMAGE =
            MOB_EFFECTS.register("true_damage", TrueDamageEffect::new);
}