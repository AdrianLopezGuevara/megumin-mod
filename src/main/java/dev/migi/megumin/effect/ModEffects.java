package dev.migi.megumin.effect;

import dev.migi.megumin.MeguminMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, MeguminMod.MOD_ID);

    public static final DeferredHolder<MobEffect, ExplosionExhaustionEffect> EXPLOSION_EXHAUSTION =
            EFFECTS.register("explosion_exhaustion",
                    () -> new ExplosionExhaustionEffect(MobEffectCategory.HARMFUL, 0xFF3399));
}
