package me.infamous.accessmod.common.entity.ai.sleep;

import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;
import java.util.function.BiPredicate;

public class SleepGoal<T extends CreatureEntity & SleepingMob> extends Goal {
    private final T mob;
    private final int ticksUntilNextSleepCheck;
    private final EntityPredicate alertableTargeting;
    private final int alertableXZ;
    private final int alertableY;
    private int countdown;

    public SleepGoal(T mob, int ticksUntilNextSleepCheck){
        this(mob, ticksUntilNextSleepCheck, (m, le) -> false, 0, 0);
    }

    public SleepGoal(T mob, int ticksUntilNextSleepCheck, BiPredicate<T, LivingEntity> alertableSelector, int alertableXZ, int alertableY) {
        this.mob = mob;
        this.ticksUntilNextSleepCheck = ticksUntilNextSleepCheck;
        this.countdown = this.mob.getRandom().nextInt(ticksUntilNextSleepCheck);
        this.alertableTargeting = new EntityPredicate().allowUnseeable().range(alertableXZ).selector(le -> alertableSelector.test(this.mob, le));
        this.alertableXZ = alertableXZ;
        this.alertableY = alertableY;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }
    
    protected boolean hasShelter() {
        BlockPos blockpos = new BlockPos(this.mob.getX(), this.mob.getBoundingBox().maxY, this.mob.getZ());
        return !this.mob.level.canSeeSky(blockpos) && this.mob.getWalkTargetValue(blockpos) >= 0.0F;
    }

    protected boolean alertable() {
        return !this.mob.level.getNearbyEntities(LivingEntity.class, this.alertableTargeting, this.mob,
                this.mob.getBoundingBox().inflate(this.alertableXZ, this.alertableY, this.alertableXZ)).isEmpty();
    }
    

    @Override
    public boolean canUse() {
        if (this.mob.xxa == 0.0F && this.mob.yya == 0.0F && this.mob.zza == 0.0F) {
            return this.canSleep() || this.mob.isSleepingMob();
        } else {
            return false;
        }
    }

    @Override
    public boolean canContinueToUse() {
         return this.canSleep();
    }
      
    protected boolean canSleep() {
        if (this.countdown > 0) {
            --this.countdown;
            return false;
        } else {
            return this.mob.wantsToSleep() && this.hasShelter() && !this.alertable() && this.mob.getTarget() == null && this.mob.getLastDamageSource() == null;
        }
    }

    @Override
    public void stop() {
        this.countdown = this.mob.getRandom().nextInt(this.ticksUntilNextSleepCheck);
        this.mob.setSleepingMob(false);
    }

    @Override
    public void start() {
        this.mob.setJumping(false);
        this.mob.setSleepingMob(true);
        this.mob.getNavigation().stop();
        this.mob.getMoveControl().setWantedPosition(this.mob.getX(), this.mob.getY(), this.mob.getZ(), 0.0D);
    }
}