package me.infamous.accessmod.common.registry;

import me.infamous.accessmod.AccessMod;
import ovh.corail.flyingthings.item.ItemMagicCarpet;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class AccessModItems {

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AccessMod.MODID);

    public static final RegistryObject<Item> DUNE_SPAWN_EGG = ITEMS.register("dune_spawn_egg",
            () -> new ForgeSpawnEggItem(AccessModEntityTypes.DUNE, 7958625, 15125652,
                    (new Item.Properties()).tab(ItemGroup.TAB_MISC)));

    public static final RegistryObject<Item> MAGIC_CARPET = ITEMS.register("magic_carpet", ItemMagicCarpet::new);

    public static final RegistryObject<Item> LURKER_SPAWN_EGG = ITEMS.register("lurker_spawn_egg",
            () -> new ForgeSpawnEggItem(AccessModEntityTypes.LURKER, 12698049, 4802889,
                    (new Item.Properties()).tab(ItemGroup.TAB_MISC)));

    public static void register(IEventBus modEventBus){
        ITEMS.register(modEventBus);
    }
}
