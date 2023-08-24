package me.infamous.accessmod.common.entity.ai;

import me.infamous.accessmod.common.entity.Digger;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;

public class BuriedGoal<T extends MobEntity & Digger> extends Goal {

    protected final T mob;

    public BuriedGoal(T mob){
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.mob.isBuried();
    }

    @Override
    public void start() {
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean canContinueToUse() {
        return this.mob.isBuried();
    }
}
