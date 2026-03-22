package com.eilai.runeterra.item;

import com.eilai.runeterra.init.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ElderbladeItem extends Item {

    private static final float EXECUTE_THRESHOLD = 0.3f;

    public ElderbladeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target.getHealth() <=( EXECUTE_THRESHOLD*target.getMaxHealth())) {
            if (target.level() instanceof ServerLevel serverLevel) {
                // Kill the target
                target.kill(serverLevel);

                // Spawn visual-only lightning (no damage, no fire)
                net.minecraft.world.entity.LightningBolt lightning =
                        net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(serverLevel, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
                if (lightning != null) {
                    lightning.setPos(target.getX(), target.getY(), target.getZ());
                    lightning.setVisualOnly(true);
                    serverLevel.addFreshEntity(lightning);
                }
            }
            return;
        }
        target.addEffect(new MobEffectInstance(
                ModEffects.TRUE_DAMAGE, 100, 0, false, true
        ));
    }
}