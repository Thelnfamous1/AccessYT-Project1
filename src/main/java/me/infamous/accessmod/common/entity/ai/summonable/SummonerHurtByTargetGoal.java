package me.infamous.accessmod.common.entity.ai.summonable;

import java.util.EnumSet;

import me.infamous.accessmod.duck.Summonable;
import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.TargetGoal;

public class SummonerHurtByTargetGoal extends TargetGoal {
   private final MobEntity tameAnimal;
   private LivingEntity ownerLastHurtBy;
   private int timestamp;

   public SummonerHurtByTargetGoal(MobEntity mob) {
      super(mob, false);
      this.tameAnimal = mob;
      this.setFlags(EnumSet.of(Goal.Flag.TARGET));
   }

   @Override
   public boolean canUse() {
      Summonable summonable = Summonable.cast(this.tameAnimal);
      if (summonable.isSummoned()) {
         LivingEntity summoner = summonable.getSummoner(this.tameAnimal.level);
         if (summoner == null) {
            return false;
         } else {
            this.ownerLastHurtBy = summoner.getLastHurtByMob();
            int i = summoner.getLastHurtByMobTimestamp();
            return i != this.timestamp && this.canAttack(this.ownerLastHurtBy, EntityPredicate.DEFAULT) && summonable.doesSummonableWantToAttack(this.ownerLastHurtBy, summoner);
         }
      } else {
         return false;
      }
   }

   @Override
   public void start() {
      this.mob.setTarget(this.ownerLastHurtBy);
      LivingEntity summoner = Summonable.cast(this.tameAnimal).getSummoner(this.tameAnimal.level);
      if (summoner != null) {
         this.timestamp = summoner.getLastHurtByMobTimestamp();
      }

      super.start();
   }
}