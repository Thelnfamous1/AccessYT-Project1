package me.infamous.accessmod.common.registry;

import me.infamous.accessmod.AccessMod;
import me.infamous.accessmod.common.entity.dune.Dune;
import me.infamous.accessmod.common.entity.dune.WrathfulDust;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class AccessModEntityTypes {

    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITIES, AccessMod.MODID);

    public static final RegistryObject<EntityType<Dune>> DUNE = register("dune",
            EntityType.Builder.of(Dune::new, EntityClassification.MONSTER)
                    .sized(0.75F, 2.25F)
                    .clientTrackingRange(8));

    public static final RegistryObject<EntityType<WrathfulDust>> WRATHFUL_DUST = register("wrathful_dust",
            EntityType.Builder.<WrathfulDust>of(WrathfulDust::new, EntityClassification.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10));


    private static <T extends Entity> RegistryObject<EntityType<T>> register(String pKey, EntityType.Builder<T> pBuilder) {
        return ENTITY_TYPES.register(pKey, () -> pBuilder.build(AccessMod.MODID + ":" + pKey));
    }

    public static void register(IEventBus modEventBus){
        ENTITY_TYPES.register(modEventBus);
    }
}
