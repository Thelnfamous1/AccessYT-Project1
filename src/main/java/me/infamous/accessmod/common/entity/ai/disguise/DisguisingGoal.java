package me.infamous.accessmod.common.entity.ai.disguise;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.function.Function;

public class DisguisingGoal<T extends MobEntity & AnimatableDisguise> extends Goal {

    protected final T mob;
    private final int duration;
    private final Function<T, EntityType<?>> disguiseTypeSupplier;
    private int disguisingTicks;

    public DisguisingGoal(T mob, int duration, EntityType<?> disguiseType){
        this(mob, duration, m -> disguiseType);
    }

    public DisguisingGoal(T mob, int duration, Function<T, EntityType<?>> disguiseTypeSupplier){
        this.mob = mob;
        this.duration = duration;
        this.disguiseTypeSupplier = disguiseTypeSupplier;
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
            this.mob.setDisguised(this.disguiseTypeSupplier.apply(this.mob));
        } else{
            this.mob.setRevealed();
        }
    }
}
