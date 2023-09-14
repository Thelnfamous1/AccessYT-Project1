package me.infamous.accessmod.common.entity.ai.attack;

public interface AnimatableMeleeAttack<A extends AnimatableMeleeAttack.AttackType> {

    int getAttackAnimationTick();

    void setAttackAnimationTick(int attackAnimationTick);

    default void startAttackAnimation(A attackType){
        this.setAttackAnimationTick(attackType.getAttackAnimationLength());
    }

    default boolean isAttackAnimationInProgress(){
        return this.getAttackAnimationTick() > 0;
    }

    default boolean isTimeToAttack(){
        return this.getCurrentAttackType().getAttackAnimationLength() - this.getAttackAnimationTick() == this.getCurrentAttackType().getAttackAnimationActionPoint();
    }

    A getCurrentAttackType();

    void setCurrentAttackType(A attackType);

    A getDefaultAttackType();

    default void resetAttackType(){
        this.setCurrentAttackType(this.getDefaultAttackType());
    }

    interface AttackType{
        int getId();

        int getAttackAnimationActionPoint();

        int getAttackAnimationLength();
    }
}
