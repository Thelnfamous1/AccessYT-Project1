package me.infamous.accessmod.common.entity.dune;

import me.infamous.accessmod.common.AccessModUtil;
import me.infamous.accessmod.common.entity.ai.attack.AnimatableMeleeAttack;

public enum DuneAttackType implements AnimatableMeleeAttack.AttackType {
    NONE(0, 0, 0),
    SWIPE(1, AccessModUtil.secondsToTicks(0.12D), AccessModUtil.secondsToTicks(0.875D));

    private final int id;
    private final int attackAnimationActionPoint;
    private final int attackAnimationLength;

    DuneAttackType(int id, int attackAnimationActionPoint, int attackAnimationLength) {
        this.id = id;
        this.attackAnimationActionPoint = attackAnimationActionPoint;
        this.attackAnimationLength = attackAnimationLength;
    }

    public static DuneAttackType byId(int pId) {
        for (DuneAttackType attackType : values()) {
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
