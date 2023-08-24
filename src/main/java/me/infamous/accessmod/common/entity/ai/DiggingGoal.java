package me.infamous.accessmod.common.entity.ai;

import me.infamous.accessmod.common.entity.Digger;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;

public class DiggingGoal<T extends MobEntity & Digger> extends Goal {

    protected final T mob;
    private final int duration;
    private int diggingTicks;

    public DiggingGoal(T mob, int duration){
        this.mob = mob;
        this.duration = duration;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.mob.isSurfaced() && this.mob.isOnGround() && this.mob.wantsToDig();
    }

    @Override
    public void start() {
        this.diggingTicks = this.duration;
        if (this.mob.isOnGround()) {
            this.mob.setDigging();
            this.mob.playSound(this.mob.getDigSound(), 5.0F, 1.0F);
        } else {
            this.mob.playSound(this.mob.getAgitatedSound(), 5.0F, 1.0F);
            this.stop();
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.diggingTicks--;
    }

    @Override
    public boolean canContinueToUse() {
        return this.diggingTicks > 0 && this.mob.isAlive();
    }

    @Override
    public void stop() {
        if(this.diggingTicks <= 0){
            this.mob.setBuried();
        } else{
            this.mob.setSurfaced();
        }
    }
}
