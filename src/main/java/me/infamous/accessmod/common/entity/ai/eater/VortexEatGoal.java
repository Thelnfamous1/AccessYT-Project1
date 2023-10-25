package me.infamous.accessmod.common.entity.ai.eater;

import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.item.ItemEntity;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public class VortexEatGoal<T extends MobEntity & VortexEater> extends Goal {
    private final T mob;

    private final int searchRate;
    private long searchCooldown;
    private final int searchDistance;
    private final int eatRate;
    private int nextEatTime;
    private final double speedModifier;
    private List<? extends Entity> nearbyTargets = Collections.emptyList();

    public VortexEatGoal(T mob, int searchRate, int searchDistance, int eatRate, double speedModifier) {
        this.mob = mob;
        this.searchRate = searchRate;
        this.searchCooldown = mob.getRandom().nextInt(searchRate);
        this.searchDistance = searchDistance;
        this.eatRate = eatRate;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if(this.mob.isThrowingUp() || this.riderControlsEating()){
            return false;
        } else if (this.nextEatTime > this.mob.tickCount) {
            return false;
        } else {
            this.nearbyTargets = this.findAndCacheNearbyTargets(false);
            return !this.nearbyTargets.isEmpty() && this.mob.getLastHurtByMob() == null;
        }
    }

    private boolean riderControlsEating() {
        return this.mob.isVehicle() && this.mob.canBeControlledByRider();
    }

    private List<? extends Entity> findAndCacheNearbyTargets(boolean ignoreCooldown) {
        if (--this.searchCooldown <= 0L || ignoreCooldown) {
            this.searchCooldown = this.searchRate;
            // Don't convert the canEat call to a method reference, as that causes an exception with intersection types such as the 'T' type used for the mob instance field
            //noinspection Convert2MethodRef
            return this.mob.level.getEntitiesOfClass(ItemEntity.class, this.mob.getBoundingBox().inflate(this.searchDistance, this.searchDistance, this.searchDistance), e -> this.mob.canEat(e));
        }
        return this.nearbyTargets;
    }

    @Override
    public void start() {
        this.mob.setMouthOpen();
        this.nearbyTargets = this.findAndCacheNearbyTargets(false);
        if (!this.nearbyTargets.isEmpty()) {
            this.mob.getNavigation().moveTo(this.nearbyTargets.get(0), this.speedModifier);
        }
        this.nextEatTime = 0;
    }

    @Override
    public void tick() {
        this.nearbyTargets = this.findAndCacheNearbyTargets(false);

        if (!this.nearbyTargets.isEmpty()) {
            Entity target = this.nearbyTargets.get(0);
            switch (this.mob.getEatState()){
                case MOUTH_OPEN:
                    if(this.mob.isWithinEatRange(target)){
                        this.stopAndLookAtTarget(target);
                        this.mob.setSuckingUp(false);
                    } else{
                        this.pursueTarget(target);
                    }
                    break;
                case SUCKING_UP:
                    if(this.mob.isWithinEatRange(target)){
                        this.stopAndLookAtTarget(target);
                        // mob should be automatically transitioning to swallowing
                    } else{
                        this.pursueTarget(target);
                    }
                    break;
                case SWALLOWING:
                    if(this.mob.isWithinEatRange(target)){
                        // mob should be automatically transitioning to open/closed
                    } else{
                        this.pursueTarget(target);
                    }
                    break;
                case MOUTH_CLOSED:
                    this.setOnEatCooldown();
                    break;
            }
        }
    }

    private void setOnEatCooldown() {
        this.nextEatTime = this.mob.tickCount + this.eatRate;
    }

    private void pursueTarget(Entity target) {
        this.mob.setMouthOpen();
        this.mob.getNavigation().moveTo(target, this.speedModifier);
    }

    private void stopAndLookAtTarget(Entity target) {
        this.mob.getNavigation().stop();
        this.mob.getLookControl().setLookAt(target, this.mob.getMaxHeadYRot(), this.mob.getMaxHeadXRot());
    }

    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse();
    }

    @Override
    public void stop() {
        if(!this.mob.isThrowingUp() && !this.riderControlsEating()){
            this.mob.setMouthClosed();
        }
        if(this.nextEatTime <= 0) this.setOnEatCooldown();
    }

}