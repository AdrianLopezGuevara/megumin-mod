package dev.migi.megumin.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class ExplosionExhaustionEffect extends MobEffect {

    public ExplosionExhaustionEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // Drain hunger each tick (Megumin collapses from exhaustion)
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            player.causeFoodExhaustion(0.05f);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
