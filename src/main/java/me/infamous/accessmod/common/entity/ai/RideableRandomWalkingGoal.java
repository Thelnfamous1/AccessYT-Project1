package me.infamous.accessmod.common.entity.ai;

import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.goal.RandomWalkingGoal;
import net.minecraft.util.math.vector.Vector3d;

public class RideableRandomWalkingGoal extends RandomWalkingGoal {
    public RideableRandomWalkingGoal(CreatureEntity creatureEntity, double speedModifier, int interval) {
        super(creatureEntity, speedModifier, interval);
    }

    @Override
    public boolean canUse() {
        if (this.riderControlsMovement()) {
            return false;
        } else {
            if (!this.forceTrigger) {
                if (this.mob.getNoActionTime() >= 100) {
                    return false;
                }

                if (this.mob.getRandom().nextInt(this.interval) != 0) {
                    return false;
                }
            }

            Vector3d vector3d = this.getPosition();
            if (vector3d == null) {
                return false;
            } else {
                this.wantedX = vector3d.x;
                this.wantedY = vector3d.y;
                this.wantedZ = vector3d.z;
                this.forceTrigger = false;
                return true;
            }
        }
    }

    private boolean riderControlsMovement() {
        return this.mob.isVehicle() && this.mob.canBeControlledByRider();
    }

    @Override
    public boolean canContinueToUse() {
        return !this.mob.getNavigation().isDone() && !this.riderControlsMovement();
    }
}
