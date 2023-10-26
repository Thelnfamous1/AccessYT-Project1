package me.infamous.accessmod.common.events;

import com.google.common.collect.Maps;
import me.infamous.accessmod.AccessMod;
import me.infamous.accessmod.common.AccessModUtil;
import me.infamous.accessmod.common.capability.SoulsCapabilityProvider;
import me.infamous.accessmod.common.entity.ai.summonable.FollowSummonerGoal;
import me.infamous.accessmod.common.entity.ai.summonable.SummonerHurtByTargetGoal;
import me.infamous.accessmod.common.entity.ai.summonable.SummonerHurtTargetGoal;
import me.infamous.accessmod.common.entity.gobblefin.Gobblefin;
import me.infamous.accessmod.common.item.SoulScytheItem;
import me.infamous.accessmod.common.network.AccessModNetwork;
import me.infamous.accessmod.common.network.ServerboundDuneJumpPacket;
import me.infamous.accessmod.common.registry.AccessModEntityTypes;
import me.infamous.accessmod.common.spawner.LurkerSpawner;
import me.infamous.accessmod.duck.DuneSinker;
import me.infamous.accessmod.duck.Summonable;
import me.infamous.accessmod.mixin.GoalSelectorAccesor;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.FlyingPathNavigator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EntityPredicates;
import net.minecraft.util.Hand;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.MobSpawnInfo;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.spawner.ISpecialSpawner;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = AccessMod.MODID)
public class ForgeEventHandler {
    private static final Map<RegistryKey<World>, List<ISpecialSpawner>> CUSTOM_SPAWNERS = Maps.newLinkedHashMap();

    @SubscribeEvent
    static void onWorldTick(TickEvent.WorldTickEvent event){
        if(event.world instanceof ServerWorld && event.phase == TickEvent.Phase.END){
            ServerWorld serverLevel = (ServerWorld) event.world;
            MinecraftServer server = serverLevel.getServer();
            RegistryKey<World> dimension = serverLevel.dimension();
            List<ISpecialSpawner> customSpawners = CUSTOM_SPAWNERS.computeIfAbsent(dimension, k -> {
                List<ISpecialSpawner> spawners = new ArrayList<>();
                if(k == World.OVERWORLD){
                    spawners.add(new LurkerSpawner());
                }
                return spawners;
            });
            customSpawners.forEach(cs -> cs.tick(serverLevel, isSpawningMonsters(server), server.isSpawningAnimals()));
        }
    }

    private static boolean isSpawningMonsters(MinecraftServer server) {
        return server.getWorldData().getDifficulty() != Difficulty.PEACEFUL;
    }


    @SubscribeEvent(priority = EventPriority.HIGH)
    static void onBiomeLoading(BiomeLoadingEvent event){
        ResourceLocation biomeName = event.getName();
        if(biomeName == null) return;

        RegistryKey<Biome> biomeKey = RegistryKey.create(Registry.BIOME_REGISTRY, biomeName);
        if(BiomeDictionary.hasType(biomeKey, BiomeDictionary.Type.OVERWORLD)
                && BiomeDictionary.hasType(biomeKey, BiomeDictionary.Type.OCEAN)
                && !BiomeDictionary.hasType(biomeKey, BiomeDictionary.Type.COLD)
                && biomeName.getPath().contains("deep")){ // looking for overworld deep oceans that are not cold/frozen
            AccessMod.LOGGER.info("Adding {} spawns to {}", AccessModEntityTypes.GOBBLEFIN.getId(), biomeName);
            event.getSpawns().getSpawner(EntityClassification.WATER_CREATURE)
                    .add(new MobSpawnInfo.Spawners(AccessModEntityTypes.GOBBLEFIN.get(), 1, 1, 2));
        }
    }

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
                    SoulScytheItem.getSouls(attacker.getMainHandItem()).ifPresent(souls -> {
                        souls.addSummon(died.getType(), attacker, Hand.MAIN_HAND);
                        AccessModUtil.sendParticle((ServerWorld) died.level, ParticleTypes.SOUL, died, 0, 1.0D);
                    });
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
            summonedMob.goalSelector.addGoal(followPriority, new FollowSummonerGoal(summonedMob, 1.0D, 10, 2, summonedMob.getNavigation() instanceof FlyingPathNavigator));
            summonedMob.targetSelector.addGoal(1, new SummonerHurtByTargetGoal(summonedMob)); // 1 is the priority used by the Wolf's OwnerHurtByTargetGoal
            summonedMob.targetSelector.addGoal(2, new SummonerHurtTargetGoal(summonedMob)); // 2 is the priority used by the Wolf's OwnerHurtTargetGoal
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onLivingDrops(LivingDropsEvent event){
        if(event.isCanceled()) return;

        if(event.getEntity() instanceof MobEntity && Summonable.cast(((MobEntity) event.getEntity())).isSummoned()){
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onLivingDrops(LivingExperienceDropEvent event){
        if(event.isCanceled()) return;

        if(event.getEntity() instanceof MobEntity && Summonable.cast(((MobEntity) event.getEntity())).isSummoned()){
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onStartTracking(PlayerEvent.StartTracking event){
        if(event.getTarget() instanceof MobEntity && Summonable.cast((MobEntity) event.getTarget()).isSummoned()){
            Summonable.syncSummonerUUID((MobEntity) event.getTarget());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onDismount(EntityMountEvent event){
        if(!event.isCanceled() && event.getEntityBeingMounted() instanceof Gobblefin){
            Gobblefin gobblefin = (Gobblefin) event.getEntityBeingMounted();
            if(event.isDismounting()){
                Entity rider = event.getEntityMounting();
                if(gobblefin.isAlive() && gobblefin.isTrappedPassenger(rider) && EntityPredicates.NO_CREATIVE_OR_SPECTATOR.test(rider)){
                    event.setCanceled(true);
                } else if(!event.getWorldObj().isClientSide && !gobblefin.isThrowingUp()){
                    gobblefin.setThrowingUp();
                }
            }
        }
    }

}
