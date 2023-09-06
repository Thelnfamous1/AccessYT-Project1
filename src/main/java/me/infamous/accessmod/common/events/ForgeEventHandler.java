package me.infamous.accessmod.common.events;

import me.infamous.accessmod.AccessMod;
import me.infamous.accessmod.common.AccessModUtil;
import me.infamous.accessmod.common.network.AccessModNetwork;
import me.infamous.accessmod.common.network.ServerboundDuneJumpPacket;
import me.infamous.accessmod.duck.DuneSinker;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = AccessMod.MODID)
public class ForgeEventHandler {

    /*
    @SubscribeEvent(priority = EventPriority.HIGH)
    static void onBiomeLoading(BiomeLoadingEvent event){
        if(event.getCategory() == Biome.Category.DESERT){
            event.getSpawns().getSpawner(EntityClassification.MONSTER)
                    .add(new MobSpawnInfo.Spawners(AccessModEntityTypes.DUNE.get(), 19, 1, 1));
        }
    }
     */

    @SubscribeEvent
    static void onClientPlayerTick(TickEvent.PlayerTickEvent event){
        if(event.phase == TickEvent.Phase.END && event.side == LogicalSide.CLIENT && DuneSinker.canSink(event.player)){
            ClientPlayerEntity player = (ClientPlayerEntity) event.player;
            AccessModNetwork.SYNC_CHANNEL.sendToServer(new ServerboundDuneJumpPacket(player.input.jumping));
        }
    }

    @SubscribeEvent
    static void onItemUseFinish(LivingEntityUseItemEvent.Finish event){
        if(AccessModUtil.isFromDesertWell(event.getItem())){
            if(!event.getEntityLiving().level.isClientSide){
                AccessModUtil.summonDune(event.getEntityLiving(), (ServerWorld) event.getEntityLiving().level);
            }
        }
    }

}
