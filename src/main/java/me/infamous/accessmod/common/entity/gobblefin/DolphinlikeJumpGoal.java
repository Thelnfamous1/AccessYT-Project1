package me.infamous.accessmod.common.entity.gobblefin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.JumpGoal;
import net.minecraft.fluid.FluidState;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

public class DolphinlikeJumpGoal<T extends MobEntity> extends JumpGoal {
   private static final int[] STEPS_TO_CHECK = new int[]{0, 1, 4, 5, 6, 7};
   private final T mob;
   private final int interval;
   private final double yD;
   private final double xzD;
   private boolean breached;

   public DolphinlikeJumpGoal(T mob, int interval, double xzD, double yd) {
      this.mob = mob;
      this.interval = interval;
      this.xzD = xzD;
      this.yD = yd;
   }

   @Override
   public boolean canUse() {
      if (this.mob.getRandom().nextInt(this.interval) != 0) {
         return false;
      } else {
         Direction motionDirection = this.mob.getMotionDirection();
         int stepX = motionDirection.getStepX();
         int stepZ = motionDirection.getStepZ();
         BlockPos blockPos = this.mob.blockPosition();

         for(int scale : STEPS_TO_CHECK) {
            if (!this.waterIsClear(blockPos, stepX, stepZ, scale) || !this.surfaceIsClear(blockPos, stepX, stepZ, scale)) {
               return false;
            }
         }

         return true;
      }
   }

   private boolean waterIsClear(BlockPos pPos, int pDx, int pDz, int pScale) {
      BlockPos offset = pPos.offset(pDx * pScale, 0, pDz * pScale);
      return this.mob.level.getFluidState(offset).is(FluidTags.WATER)
              && !this.mob.level.getBlockState(offset).getMaterial().blocksMotion();
   }

   private boolean surfaceIsClear(BlockPos pPos, int pDx, int pDz, int pScale) {
      return this.mob.level.getBlockState(pPos.offset(pDx * pScale, 1, pDz * pScale)).isAir()
              && this.mob.level.getBlockState(pPos.offset(pDx * pScale, 2, pDz * pScale)).isAir();
   }

   @Override
   public boolean canContinueToUse() {
      double yD = this.mob.getDeltaMovement().y;
      return (!(yD * yD < (double)0.03F) || this.mob.xRot == 0.0F || !(Math.abs(this.mob.xRot) < 10.0F) || !this.mob.isInWater())
              && !this.mob.isOnGround();
   }

   @Override
   public boolean isInterruptable() {
      return false;
   }

   @Override
   public void start() {
      Direction motionDirection = this.mob.getMotionDirection();
      this.mob.setDeltaMovement(this.mob.getDeltaMovement().add((double)motionDirection.getStepX() * this.xzD, this.yD, (double)motionDirection.getStepZ() * this.xzD));
      this.mob.getNavigation().stop();
   }

   @Override
   public void stop() {
      this.mob.xRot = 0.0F;
   }

   @Override
   public void tick() {
      boolean wasBreached = this.breached;
      if (!wasBreached) {
         FluidState fluidStateAtBlockPos = this.mob.level.getFluidState(this.mob.blockPosition());
         this.breached = fluidStateAtBlockPos.is(FluidTags.WATER);
      }

      if (this.breached && !wasBreached) {
         this.mob.playSound(SoundEvents.DOLPHIN_JUMP, 1.0F, 1.0F);
      }

      Vector3d deltaMovement = this.mob.getDeltaMovement();
      if (deltaMovement.y * deltaMovement.y < (double)0.03F && this.mob.xRot != 0.0F) {
         this.mob.xRot = MathHelper.rotlerp(this.mob.xRot, 0.0F, 0.2F);
      } else {
         double horizontalDist = Math.sqrt(Entity.getHorizontalDistanceSqr(deltaMovement));
         double targetXRot = Math.signum(-deltaMovement.y) * Math.acos(horizontalDist / deltaMovement.length()) * (double)(180F / (float)Math.PI);
         this.mob.xRot = (float)targetXRot;
      }

   }
}