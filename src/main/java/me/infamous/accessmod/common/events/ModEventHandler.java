package me.infamous.accessmod.common.events;

import me.infamous.accessmod.AccessMod;
import me.infamous.accessmod.common.entity.dune.Dune;
import me.infamous.accessmod.common.entity.lurker.Lurker;
import me.infamous.accessmod.common.network.AccessModNetwork;
import me.infamous.accessmod.common.registry.AccessModEntityTypes;
import me.infamous.accessmod.common.registry.AccessModLootFunctions;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = AccessMod.MODID)
public class ModEventHandler {

    @SubscribeEvent
    public static void onClientSetup(EntityAttributeCreationEvent event) {
        event.put(AccessModEntityTypes.DUNE.get(), Dune.createAttributes().build());
        event.put(AccessModEntityTypes.LURKER.get(), Lurker.createAttributes().build());
    }

    @SubscribeEvent
    static void onCommonSetup(FMLCommonSetupEvent event){
        event.enqueueWork(() -> {
            AccessModNetwork.init();
            EntitySpawnPlacementRegistry.register(
                    AccessModEntityTypes.DUNE.get(),
                    EntitySpawnPlacementRegistry.PlacementType.ON_GROUND,
                    Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                    Dune::checkDuneSpawnRules);
            EntitySpawnPlacementRegistry.register(
                    AccessModEntityTypes.LURKER.get(),
                    EntitySpawnPlacementRegistry.PlacementType.ON_GROUND,
                    Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                    MonsterEntity::checkMonsterSpawnRules);
            AccessModLootFunctions.register();
        });
    }
}
