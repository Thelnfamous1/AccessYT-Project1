package me.infamous.accessmod.common.entity.ai.summonable;

import java.util.EnumSet;

import me.infamous.accessmod.duck.Summonable;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.pathfinding.FlyingPathNavigator;
import net.minecraft.pathfinding.GroundPathNavigator;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;

public class FollowSummonerGoal extends Goal {
   private final MobEntity mob;
   private LivingEntity summoner;
   private final IWorldReader level;
   private final double speedModifier;
   private final PathNavigator navigation;
   private int timeToRecalcPath;
   private final float stopDistance;
   private final float startDistance;
   private float oldWaterCost;
   private final boolean canFly;

   public FollowSummonerGoal(MobEntity mob, double speedModifier, float startDistance, float stopDistance, boolean canFly) {
      this.mob = mob;
      this.level = mob.level;
      this.speedModifier = speedModifier;
      this.navigation = mob.getNavigation();
      this.startDistance = startDistance;
      this.stopDistance = stopDistance;
      this.canFly = canFly;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
      if (!(mob.getNavigation() instanceof GroundPathNavigator) && !(mob.getNavigation() instanceof FlyingPathNavigator)) {
         throw new IllegalArgumentException("Unsupported mob type for FollowSummonerGoal");
      }
   }

   /**
    * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
    * method as well.
    */
   public boolean canUse() {
      LivingEntity livingentity = Summonable.cast(this.mob).getSummoner(this.mob.level);
      if (livingentity == null) {
         return false;
      } else if (livingentity.isSpectator()) {
         return false;
      } else if (this.mob.distanceToSqr(livingentity) < (double)(this.startDistance * this.startDistance)) {
         return false;
      } else {
         this.summoner = livingentity;
         return true;
      }
   }

   /**
    * Returns whether an in-progress EntityAIBase should continue executing
    */
   public boolean canContinueToUse() {
      if (this.navigation.isDone()) {
         return false;
      } else {
         return !(this.mob.distanceToSqr(this.summoner) <= (double)(this.stopDistance * this.stopDistance));
      }
   }

   /**
    * Execute a one shot task or start executing a continuous task
    */
   public void start() {
      this.timeToRecalcPath = 0;
      this.oldWaterCost = this.mob.getPathfindingMalus(PathNodeType.WATER);
      this.mob.setPathfindingMalus(PathNodeType.WATER, 0.0F);
   }

   /**
    * Reset the task's internal state. Called when this task is interrupted by another one
    */
   public void stop() {
      this.summoner = null;
      this.navigation.stop();
      this.mob.setPathfindingMalus(PathNodeType.WATER, this.oldWaterCost);
   }

   /**
    * Keep ticking a continuous task that has already been started
    */
   public void tick() {
      this.mob.getLookControl().setLookAt(this.summoner, 10.0F, (float)this.mob.getMaxHeadXRot());
      if (--this.timeToRecalcPath <= 0) {
         this.timeToRecalcPath = 10;
         if (!this.mob.isLeashed() && !this.mob.isPassenger()) {
            if (this.mob.distanceToSqr(this.summoner) >= 144.0D) {
               this.teleportToOwner();
            } else {
               this.navigation.moveTo(this.summoner, this.speedModifier);
            }

         }
      }
   }

   private void teleportToOwner() {
      BlockPos blockpos = this.summoner.blockPosition();

      for(int i = 0; i < 10; ++i) {
         int j = this.randomIntInclusive(-3, 3);
         int k = this.randomIntInclusive(-1, 1);
         int l = this.randomIntInclusive(-3, 3);
         boolean flag = this.maybeTeleportTo(blockpos.getX() + j, blockpos.getY() + k, blockpos.getZ() + l);
         if (flag) {
            return;
         }
      }

   }

   private boolean maybeTeleportTo(int pX, int pY, int pZ) {
      if (Math.abs((double)pX - this.summoner.getX()) < 2.0D && Math.abs((double)pZ - this.summoner.getZ()) < 2.0D) {
         return false;
      } else if (!this.canTeleportTo(new BlockPos(pX, pY, pZ))) {
         return false;
      } else {
         this.mob.moveTo((double)pX + 0.5D, pY, (double)pZ + 0.5D, this.mob.yRot, this.mob.xRot);
         this.navigation.stop();
         return true;
      }
   }

   private boolean canTeleportTo(BlockPos pPos) {
      PathNodeType pathnodetype = WalkNodeProcessor.getBlockPathTypeStatic(this.level, pPos.mutable());
      if (pathnodetype != PathNodeType.WALKABLE) {
         return false;
      } else {
         BlockState blockstate = this.level.getBlockState(pPos.below());
         if (!this.canFly && blockstate.getBlock() instanceof LeavesBlock) {
            return false;
         } else {
            BlockPos blockpos = pPos.subtract(this.mob.blockPosition());
            return this.level.noCollision(this.mob, this.mob.getBoundingBox().move(blockpos));
         }
      }
   }

   private int randomIntInclusive(int pMin, int pMax) {
      return this.mob.getRandom().nextInt(pMax - pMin + 1) + pMin;
   }
}