package dev.migi.megumin.item;

import dev.migi.megumin.MeguminMod;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.core.registries.Registries;

public class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MeguminMod.MOD_ID);

    public static final DeferredItem<ExplosionStaffItem> EXPLOSION_STAFF =
            ITEMS.register("explosion_staff", () -> new ExplosionStaffItem(
                    new Item.Properties()
                            .stacksTo(1)
                            .durability(0) // indestructible (cooldown handles reuse)
            ));
}
