package me.infamous.accessmod.common.entity.ai.attack;

import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.util.Hand;

import java.util.function.Function;

public class AnimatableMeleeAttackGoal<T extends CreatureEntity & AnimatableMeleeAttack<A>, A extends AnimatableMeleeAttack.AttackType> extends MeleeAttackGoal {
    protected final T attacker;
    private final Function<T, A> attackTypeGetter;

    public AnimatableMeleeAttackGoal(T attacker, A attackType, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        this(attacker, m -> attackType, speedModifier, followingTargetEvenIfNotSeen);
    }

    public AnimatableMeleeAttackGoal(T attacker, Function<T, A> attackTypeGetter, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(attacker, speedModifier, followingTargetEvenIfNotSeen);
        this.attacker = attacker;
        this.attackTypeGetter = attackTypeGetter;
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
        if (pDistToEnemySqr <= attackReachSqr && this.getTicksUntilNextAttack() <= 0) {
            if(!this.attacker.isAttackAnimationInProgress()){
                A attackType = this.attackTypeGetter.apply(this.attacker);
                this.attacker.startAttackAnimation(attackType);
                this.attacker.setCurrentAttackType(attackType);
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
