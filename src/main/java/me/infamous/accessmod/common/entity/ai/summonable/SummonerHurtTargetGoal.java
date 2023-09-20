package me.infamous.accessmod.common.entity.ai.summonable;

import me.infamous.accessmod.duck.Summonable;
import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.TargetGoal;

import java.util.EnumSet;

public class SummonerHurtTargetGoal extends TargetGoal {
   private final MobEntity tameAnimal;
   private LivingEntity ownerLastHurt;
   private int timestamp;

   public SummonerHurtTargetGoal(MobEntity mob) {
      super(mob, false);
      this.tameAnimal = mob;
      this.setFlags(EnumSet.of(Goal.Flag.TARGET));
   }

   /**
    * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
    * method as well.
    */
   public boolean canUse() {
      Summonable summonable = Summonable.cast(this.tameAnimal);
      if (summonable.isSummoned()) {
         LivingEntity summoner = summonable.getSummoner(this.tameAnimal.level);
         if (summoner == null) {
            return false;
         } else {
            this.ownerLastHurt = summoner.getLastHurtMob();
            int i = summoner.getLastHurtMobTimestamp();
            return i != this.timestamp && this.canAttack(this.ownerLastHurt, EntityPredicate.DEFAULT) && summonable.doesSummonableWantToAttack(this.ownerLastHurt, summoner);
         }
      } else {
         return false;
      }
   }

   /**
    * Execute a one shot task or start executing a continuous task
    */
   public void start() {
      this.mob.setTarget(this.ownerLastHurt);
      LivingEntity summoner = Summonable.cast(this.tameAnimal).getSummoner(this.tameAnimal.level);
      if (summoner != null) {
         this.timestamp = summoner.getLastHurtMobTimestamp();
      }

      super.start();
   }
}