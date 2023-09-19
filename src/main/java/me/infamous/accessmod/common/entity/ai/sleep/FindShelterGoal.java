package me.infamous.accessmod.common.entity.ai.sleep;

import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.goal.FleeSunGoal;
import net.minecraft.util.math.BlockPos;

public class FindShelterGoal<T extends CreatureEntity & SleepingMob> extends FleeSunGoal {
    private final T sleepingMob;
    private final int maxInterval;
    private int interval;
    private final boolean fearsThunder;

    public FindShelterGoal(T mob, double speedModifier, int maxInterval, boolean fearsThunder) {
        super(mob, speedModifier);
        this.sleepingMob = mob;
        this.maxInterval = maxInterval;
        this.interval = maxInterval;
        this.fearsThunder = fearsThunder;
    }

    @Override
    public boolean canUse() {
        if (!this.sleepingMob.isSleepingMob() && this.mob.getTarget() == null) {
            if (this.fearsThunder && this.mob.level.isThundering()) {
                return true;
            } else if (this.interval > 0) {
                --this.interval;
                return false;
            } else {
                this.interval = this.maxInterval;
                BlockPos blockpos = this.mob.blockPosition();
                return this.sleepingMob.wantsToSleep() && this.mob.level.canSeeSky(blockpos) && this.setWantedPos();
            }
        } else {
            return false;
        }
    }

    @Override
    public void start() {
        super.start();
    }
}