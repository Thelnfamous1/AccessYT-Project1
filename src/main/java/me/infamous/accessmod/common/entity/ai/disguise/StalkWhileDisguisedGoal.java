package me.infamous.accessmod.common.entity.ai.disguise;

import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;

public class StalkWhileDisguisedGoal<T extends CreatureEntity & AnimatableDisguise> extends Goal {
   protected final T mob;
   private final double speedModifier;
   private final double closeEnough;

   public StalkWhileDisguisedGoal(T mob, double speedModifier, double closeEnough) {
      this.mob = mob;
      this.speedModifier = speedModifier;
      this.closeEnough = closeEnough;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
   }

   @Override
   public boolean canUse() {
      return this.mob.isDisguised() && this.mob.getTarget() != null;
   }

   @Override
   public boolean canContinueToUse() {
      return this.canUse();
   }

   public void start() {
   }

   @Override
   public void stop() {
      this.mob.getNavigation().stop();
   }

   @Override
   public void tick() {
      this.mob.getLookControl().setLookAt(this.mob.getTarget(), (float)(this.mob.getMaxHeadYRot() + 20), (float)this.mob.getMaxHeadXRot());
      if (this.mob.closerThan(this.mob.getTarget(), this.closeEnough)) {
         this.mob.getNavigation().stop();
      } else {
         this.mob.getNavigation().moveTo(this.mob.getTarget(), this.speedModifier);
      }
   }
}