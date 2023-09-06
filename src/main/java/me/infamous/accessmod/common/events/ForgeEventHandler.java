package me.infamous.accessmod.common.events;

import me.infamous.accessmod.AccessMod;
import me.infamous.accessmod.common.AccessModUtil;
import me.infamous.accessmod.common.network.AccessModNetwork;
import me.infamous.accessmod.common.network.ServerboundDuneJumpPacket;
import me.infamous.accessmod.common.registry.AccessModPOITypes;
import me.infamous.accessmod.duck.DuneSinker;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.item.GlassBottleItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
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
    static void rightClickBlock(PlayerInteractEvent.RightClickItem event){
        if(!event.getWorld().isClientSide && event.getItemStack().getItem() instanceof GlassBottleItem){
            ServerWorld serverWorld = (ServerWorld) event.getWorld();
            BlockRayTraceResult playerPOVHitResult = AccessModUtil.getPlayerPOVHitResult(event.getWorld(), event.getPlayer(), RayTraceContext.FluidMode.SOURCE_ONLY);
            if(playerPOVHitResult.getType() == RayTraceResult.Type.BLOCK){
                BlockPos blockPos = playerPOVHitResult.getBlockPos();
                AccessMod.LOGGER.info("BlockState at position: {}", serverWorld.getBlockState(blockPos));
                if(serverWorld.getPoiManager().existsAtPosition(AccessModPOITypes.DESERT_WELL.get(), blockPos)){
                    AccessMod.LOGGER.info("Taking water out of a desert well at {}", blockPos);
                }
            }
        }
    }

}
