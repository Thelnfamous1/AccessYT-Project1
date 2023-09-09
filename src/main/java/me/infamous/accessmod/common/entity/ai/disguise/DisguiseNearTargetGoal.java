package me.infamous.accessmod.common.entity.ai.disguise;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;

public class DisguiseNearTargetGoal<T extends MobEntity & AnimatableDisguise> extends Goal {

    private final T mob;
    private final double closeEnough;
    private final EntityType<?> disguiseType;

    public DisguiseNearTargetGoal(T mob, double closeEnough, EntityType<?> disguiseType){
        this.mob = mob;
        this.closeEnough = closeEnough;
        this.disguiseType = disguiseType;
    }

    @Override
    public boolean canUse() {
        return this.mob.getTarget() != null
                && this.mob.closerThan(this.mob.getTarget(), this.closeEnough);
    }

    @Override
    public void start() {
        AnimatableDisguise.cast(this.mob).disguiseAs(this.disguiseType);
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }
}
