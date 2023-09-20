package me.infamous.accessmod.common.events;

import me.infamous.accessmod.AccessMod;
import me.infamous.accessmod.common.AccessModUtil;
import me.infamous.accessmod.common.capability.SoulsCapabilityProvider;
import me.infamous.accessmod.common.entity.ai.summonable.FollowSummonerGoal;
import me.infamous.accessmod.common.entity.ai.summonable.SummonerHurtByTargetGoal;
import me.infamous.accessmod.common.entity.ai.summonable.SummonerHurtTargetGoal;
import me.infamous.accessmod.common.item.SoulScytheItem;
import me.infamous.accessmod.common.network.AccessModNetwork;
import me.infamous.accessmod.common.network.ServerboundDuneJumpPacket;
import me.infamous.accessmod.duck.DuneSinker;
import me.infamous.accessmod.duck.Summonable;
import me.infamous.accessmod.mixin.GoalSelectorAccesor;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.FlyingPathNavigator;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.EventPriority;
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

    @SubscribeEvent
    static void onItemCapabilityAttach(AttachCapabilitiesEvent<ItemStack> event){
        if(event.getObject().getItem() instanceof SoulScytheItem){
            final SoulsCapabilityProvider provider = new SoulsCapabilityProvider();
            event.addCapability(SoulsCapabilityProvider.IDENTIFIER, provider);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onKillEntity(LivingDeathEvent event){
        if(event.isCanceled()) return;
        LivingEntity died = event.getEntityLiving();
        if(died.level.isClientSide) return;

        if(SoulScytheItem.canHarvestSoul(died)){
            if(event.getSource().getDirectEntity() instanceof LivingEntity){
                LivingEntity attacker = (LivingEntity) event.getSource().getDirectEntity();
                if(attacker.getMainHandItem().getItem() instanceof SoulScytheItem){
                    SoulScytheItem.getSouls(attacker.getMainHandItem()).ifPresent(souls -> souls.addSummon(died.getType()));
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onEntityJoinWorld(EntityJoinWorldEvent event){
        if(event.getWorld().isClientSide) return;
        if(event.isCanceled()) return;

        if(event.getEntity() instanceof MobEntity && Summonable.cast((MobEntity)event.getEntity()).isSummoned()){
            MobEntity summonedMob = (MobEntity) event.getEntity();
            int followPriority = ((GoalSelectorAccesor)summonedMob.goalSelector).accessmod_getAvailableGoals()
                    .stream()
                    .filter(pg -> pg.getGoal() instanceof MeleeAttackGoal)
                    .map(pg -> pg.getPriority() + 1).findFirst() // 1 more than the attack goal's priority, so it runs after it
                    .orElse(6); // 6 is the priority used by the Wolf's FollowOwnerGoal
            summonedMob.goalSelector.addGoal(followPriority, new FollowSummonerGoal(summonedMob, 1.0D, 2, 10, summonedMob.getNavigation() instanceof FlyingPathNavigator));
            summonedMob.targetSelector.addGoal(1, new SummonerHurtByTargetGoal(summonedMob)); // 1 is the priority used by the Wolf's OwnerHurtByTargetGoal
            summonedMob.targetSelector.addGoal(2, new SummonerHurtTargetGoal(summonedMob)); // 2 is the priority used by the Wolf's OwnerHurtTargetGoal
        }
    }

}
