package me.infamous.accessmod.common.entity.ai.disguise;

import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;

public class DisguiseGoal<T extends MobEntity & AnimatableDisguise> extends Goal {

    protected final T mob;
    private final int duration;
    private int disguisingTicks;

    public DisguiseGoal(T mob, int duration){
        this.mob = mob;
        this.duration = duration;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.mob.isRevealed() && this.mob.wantsToDisguise();
    }

    @Override
    public void start() {
        this.disguisingTicks = this.duration;
        this.mob.setDisguising();
        this.mob.playSound(this.mob.getDisguiseSound(), 5.0F, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        this.disguisingTicks--;
    }

    @Override
    public boolean canContinueToUse() {
        return this.disguisingTicks > 0 && this.mob.isAlive();
    }

    @Override
    public void stop() {
        if(this.disguisingTicks <= 0){
            this.mob.setDisguised();
        } else{
            this.mob.setRevealed();
        }
    }
}
