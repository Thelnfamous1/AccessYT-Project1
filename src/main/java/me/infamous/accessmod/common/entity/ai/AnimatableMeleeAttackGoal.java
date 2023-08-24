package me.infamous.accessmod.common.entity.ai;

import me.infamous.accessmod.common.entity.AnimatableMeleeAttack;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.util.Hand;

public class AnimatableMeleeAttackGoal<T extends CreatureEntity & AnimatableMeleeAttack> extends MeleeAttackGoal {
    protected final T attacker;

    public AnimatableMeleeAttackGoal(T attacker, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(attacker, speedModifier, followingTargetEvenIfNotSeen);
        this.attacker = attacker;
    }

    @Override
    public boolean canUse() {
        return super.canUse() && !this.attacker.isAttackAnimationInProgress();
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity pEnemy, double pDistToEnemySqr) {
        double attackReachSqr = this.getAttackReachSqr(pEnemy);
        if (pDistToEnemySqr <= attackReachSqr) {
            if(!this.attacker.isAttackAnimationInProgress()){
                this.attacker.startAttackAnimation();
                this.attacker.level.broadcastEntityEvent(this.attacker, AnimatableMeleeAttack.START_ATTACK_EVENT);
            } else if(this.attacker.isTimeToAttack()){
                this.resetAttackCooldown();
                this.mob.swing(Hand.MAIN_HAND);
                this.mob.doHurtTarget(pEnemy);
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse();
    }

    @Override
    public void stop() {
        super.stop();
    }
}
