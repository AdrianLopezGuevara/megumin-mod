package dev.migi.megumin.sound;

import dev.migi.megumin.MeguminMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, MeguminMod.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSION_CHANT =
            SOUNDS.register("explosion_chant",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(MeguminMod.MOD_ID, "explosion_chant")));
}
