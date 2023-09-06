package me.infamous.accessmod.common.registry;

import com.google.common.collect.ImmutableSet;
import me.infamous.accessmod.AccessMod;
import net.minecraft.util.ResourceLocation;
import net.minecraft.village.PointOfInterestType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class AccessModPOITypes {

    private static final DeferredRegister<PointOfInterestType> POI_TYPES = DeferredRegister.create(ForgeRegistries.POI_TYPES, AccessMod.MODID);

    public static final RegistryObject<PointOfInterestType> DESERT_WELL = POI_TYPES.register("desert_well",
            () -> new PointOfInterestType(
                    new ResourceLocation(AccessMod.MODID, "desert_well").toString(),
                    ImmutableSet.of(),
                    1,
                    1));

    public static void register(IEventBus modEventBus){
        POI_TYPES.register(modEventBus);
    }
}
