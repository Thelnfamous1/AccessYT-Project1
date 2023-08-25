package me.infamous.accessmod.common.entity.ai.attack;

public interface AnimatableMeleeAttack {
    byte START_ATTACK_EVENT = 4;

    int getAttackAnimationTick();

    void setAttackAnimationTick(int attackAnimationTick);

    int getAttackAnimationLength();

    int getAttackAnimationActionPoint();

    default void startAttackAnimation(){
        this.setAttackAnimationTick(this.getAttackAnimationLength());
    }

    default boolean isAttackAnimationInProgress(){
        return this.getAttackAnimationTick() > 0;
    }

    default boolean isTimeToAttack(){
        return this.getAttackAnimationTick() == this.getAttackAnimationActionPoint();
    }
}
