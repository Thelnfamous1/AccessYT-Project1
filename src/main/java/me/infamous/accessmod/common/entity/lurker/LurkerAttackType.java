package me.infamous.accessmod.common.entity.lurker;

import me.infamous.accessmod.common.AccessModUtil;
import me.infamous.accessmod.common.entity.ai.attack.AnimatableMeleeAttack;

public enum LurkerAttackType implements AnimatableMeleeAttack.AttackType {
    NONE(0, 0, 0),
    SLASH(1, AccessModUtil.secondsToTicks(0.08D), AccessModUtil.secondsToTicks(0.625D)),
    HOOK(2, AccessModUtil.secondsToTicks(0.12D), AccessModUtil.secondsToTicks(0.625D));

    private final int id;
    private final int attackAnimationActionPoint;
    private final int attackAnimationLength;

    LurkerAttackType(int id, int attackAnimationActionPoint, int attackAnimationLength) {
        this.id = id;
        this.attackAnimationActionPoint = attackAnimationActionPoint;
        this.attackAnimationLength = attackAnimationLength;
    }

    public static LurkerAttackType byId(int pId) {
        for (LurkerAttackType attackType : values()) {
            if (pId == attackType.id) {
                return attackType;
            }
        }

        return NONE;
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public int getAttackAnimationActionPoint() {
        return this.attackAnimationActionPoint;
    }

    @Override
    public int getAttackAnimationLength() {
        return this.attackAnimationLength;
    }
}
