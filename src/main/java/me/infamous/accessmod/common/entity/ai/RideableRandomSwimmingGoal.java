package me.infamous.accessmod.common.entity.ai;

import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.pathfinding.PathType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

import javax.annotation.Nullable;

public class RideableRandomSwimmingGoal extends RideableRandomWalkingGoal{
    public RideableRandomSwimmingGoal(CreatureEntity creatureEntity, double speedModifier, int interval) {
        super(creatureEntity, speedModifier, interval);
    }


    @Nullable
    protected Vector3d getPosition() {
        Vector3d randomPos = RandomPositionGenerator.getPos(this.mob, 10, 7);

        for(int i = 0; randomPos != null && !this.mob.level.getBlockState(new BlockPos(randomPos)).isPathfindable(this.mob.level, new BlockPos(randomPos), PathType.WATER) && i++ < 10; randomPos = RandomPositionGenerator.getPos(this.mob, 10, 7)) {
        }

        return randomPos;
    }
}
