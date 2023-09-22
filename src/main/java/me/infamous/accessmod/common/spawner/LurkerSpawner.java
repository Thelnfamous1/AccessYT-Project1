package me.infamous.accessmod.common.spawner;

import me.infamous.accessmod.AccessMod;
import me.infamous.accessmod.common.entity.lurker.Lurker;
import me.infamous.accessmod.common.registry.AccessModEntityTypes;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.stats.ServerStatisticsManager;
import net.minecraft.stats.Stats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.GameRules;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.spawner.ISpecialSpawner;
import net.minecraft.world.spawner.WorldEntitySpawner;
import net.minecraftforge.common.ForgeHooks;

import java.util.Random;

public class LurkerSpawner implements ISpecialSpawner {
   public static final int INSOMNIA_MIN_TICKS = 72000;
   private int nextTick;

   public int tick(ServerWorld pLevel, boolean pSpawnHostiles, boolean pSpawnPassives) {
      if (!pSpawnHostiles) {
         return 0;
      } else if (!pLevel.getGameRules().getBoolean(GameRules.RULE_DOINSOMNIA)) {
         return 0;
      } else if(false) {
         return 0;
      } else {
         Random random = pLevel.random;
         --this.nextTick;
         if (this.nextTick > 0) {
            return 0;
         } else {
            this.nextTick += 100; // (60 + random.nextInt(60)) * 20; // will try to spawn a lurker again in 1200-2380 ticks, or 60-119 seconds
            if (pLevel.getSkyDarken() < 5 && pLevel.dimensionType().hasSkyLight()) {
               return 0;
            } else {
               int spawned = 0;


               AccessMod.LOGGER.info("LurkerSpawner is ticking");
               for(ServerPlayerEntity player : pLevel.players()) {
                  if (!player.isSpectator()) {
                     BlockPos playerBlockPos = player.blockPosition();
                     if (!pLevel.dimensionType().hasSkyLight() || playerBlockPos.getY() >= pLevel.getSeaLevel() && pLevel.canSeeSky(playerBlockPos)) {
                        DifficultyInstance currentDifficultyAt = pLevel.getCurrentDifficultyAt(playerBlockPos);
                        if (true || currentDifficultyAt.isHarderThan(random.nextFloat() * 3.0F)) {
                           AccessMod.LOGGER.info("Current difficulty at {} acceptable for LurkerSpawner", playerBlockPos);
                           ServerStatisticsManager stats = player.getStats();
                           int timeSinceRest = MathHelper.clamp(stats.getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)), 1, Integer.MAX_VALUE);
                           AccessMod.LOGGER.info("{} has not slept for {} ticks", player, timeSinceRest);
                           if (true || random.nextInt(timeSinceRest) >= INSOMNIA_MIN_TICKS) {
                              AccessMod.LOGGER.info("LurkerSpawner is trying to spawn a Lurker");
                              BlockPos nearbyBlockPos = randomNearbyPos(random, playerBlockPos, 10);
                              AccessMod.LOGGER.info("LurkerSpawner is trying to spawn a Lurker at {}", nearbyBlockPos);
                              if (WorldEntitySpawner.isSpawnPositionOk(EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, pLevel, nearbyBlockPos, AccessModEntityTypes.LURKER.get())) {
                                 int lurkersToSpawn = 1;
                                 Lurker lurker = AccessModEntityTypes.LURKER.get().create(pLevel);
                                 lurker.moveTo(nearbyBlockPos, 0.0F, 0.0F);
                                 if(ForgeHooks.canEntitySpawn(lurker, pLevel, nearbyBlockPos.getX(), nearbyBlockPos.getY(), nearbyBlockPos.getZ(), null, SpawnReason.NATURAL) == -1) return 0;
                                 lurker.finalizeSpawn(pLevel, currentDifficultyAt, SpawnReason.NATURAL, null, null);
                                 pLevel.addFreshEntityWithPassengers(lurker);
                                 AccessMod.LOGGER.info("LurkerSpawner spawned a Lurker at {}", nearbyBlockPos);

                                 spawned += lurkersToSpawn;
                              }
                           }
                        }
                     }
                  }
               }

               return spawned;
            }
         }
      }
   }

   private static BlockPos randomNearbyPos(Random random, BlockPos playerBlockPos, int maxXZDist) {
      int bound = (maxXZDist * 2) + 1;
      return playerBlockPos.east(-maxXZDist + random.nextInt(bound)).south(-maxXZDist + random.nextInt(bound));
   }
}