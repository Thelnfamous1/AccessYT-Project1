package me.infamous.accessmod.common.entity.ai;

import me.infamous.accessmod.common.entity.Digger;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;

public class EmergingGoal<T extends MobEntity & Digger> extends Goal {

    protected final T mob;
    private final int duration;
    private int emergingTicks;

    public EmergingGoal(T mob, int duration){
        this.mob = mob;
        this.duration = duration;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.mob.isBuried() && this.mob.wantsToEmerge();
    }

    @Override
    public void start() {
        this.mob.setEmerging();
        this.mob.playSound(this.mob.getEmergeSound(), 5.0F, 1.0F);
        this.emergingTicks = this.duration;
    }

    @Override
    public void tick() {
        super.tick();
        this.emergingTicks--;
    }

    @Override
    public boolean canContinueToUse() {
        return this.emergingTicks > 0 && this.mob.isAlive();
    }

    @Override
    public void stop() {
        if(this.emergingTicks <= 0){
            this.mob.setSurfaced();
        } else{
            this.mob.setBuried();
        }
    }
}
