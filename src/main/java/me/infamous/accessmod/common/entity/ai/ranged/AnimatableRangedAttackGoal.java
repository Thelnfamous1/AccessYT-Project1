package me.infamous.accessmod.common.entity.ai.ranged;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.MathHelper;

import java.util.EnumSet;

public abstract class AnimatableRangedAttackGoal<T extends MobEntity> extends Goal {
   private final T mob;
   private LivingEntity target;
   private int attackTime = -1;
   private final double speedModifier;
   private int seeTime;
   private final int attackIntervalMin;
   private final int attackIntervalMax;
   private final float attackRadius;
   private final float attackRadiusSqr;

   protected AnimatableRangedAttackGoal(T mob, double speedModifier, int attackInterval, float attackRadius) {
      this(mob, speedModifier, attackInterval, attackInterval, attackRadius);
   }

   protected AnimatableRangedAttackGoal(T mob, double speedModifier, int attackIntervalMin, int attackIntervalMax, float attackRadius) {
      this.mob = mob;
      this.speedModifier = speedModifier;
      this.attackIntervalMin = attackIntervalMin;
      this.attackIntervalMax = attackIntervalMax;
      this.attackRadius = attackRadius;
      this.attackRadiusSqr = attackRadius * attackRadius;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
   }

   @Override
   public boolean canUse() {
      LivingEntity livingentity = this.mob.getTarget();
      if (livingentity != null && livingentity.isAlive()) {
         this.target = livingentity;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void tick() {
      double distanceToSqr = this.mob.distanceToSqr(this.target.getX(), this.target.getY(), this.target.getZ());
      boolean canSee = this.mob.getSensing().canSee(this.target);
      if (canSee) {
         ++this.seeTime;
      } else {
         this.seeTime = 0;
      }

      if (!(distanceToSqr > (double)this.attackRadiusSqr) && this.seeTime >= 5) {
         this.mob.getNavigation().stop();
      } else {
         this.mob.getNavigation().moveTo(this.target, this.speedModifier);
      }

      this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
      if (--this.attackTime == 0) {
         if (!canSee) {
            return;
         }

         float attackSpeed = MathHelper.sqrt(distanceToSqr) / this.attackRadius;
         float distanceFactor = MathHelper.clamp(attackSpeed, 0.1F, 1.0F);
         this.performRangedAttack(this.target, distanceFactor);
         this.attackTime = MathHelper.floor(attackSpeed * (float)(this.attackIntervalMax - this.attackIntervalMin) + (float)this.attackIntervalMin);
      } else if (this.attackTime < 0) {
         float attackSpeed = MathHelper.sqrt(distanceToSqr) / this.attackRadius;
         this.attackTime = MathHelper.floor(attackSpeed * (float)(this.attackIntervalMax - this.attackIntervalMin) + (float)this.attackIntervalMin);
      }

   }

   protected abstract void performRangedAttack(LivingEntity target, float distanceFactor);

   @Override
   public boolean canContinueToUse() {
      return this.canUse() || !this.mob.getNavigation().isDone();
   }

   @Override
   public void stop() {
      this.target = null;
      this.seeTime = 0;
      this.attackTime = -1;
   }
}