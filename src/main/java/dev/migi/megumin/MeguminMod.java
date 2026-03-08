package dev.migi.megumin;

import dev.migi.megumin.item.ModItems;
import dev.migi.megumin.event.ExplosionEvents;
import dev.migi.megumin.effect.ModEffects;
import dev.migi.megumin.sound.ModSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(MeguminMod.MOD_ID)
public class MeguminMod {

    public static final String MOD_ID = "megumin";

    public MeguminMod(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.ITEMS.register(modEventBus);
        ModEffects.EFFECTS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        modEventBus.addListener(ExplosionEvents::registerCapabilities);
    }
}
