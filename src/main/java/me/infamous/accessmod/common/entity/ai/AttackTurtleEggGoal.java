package me.infamous.accessmod.common.entity.ai;

import net.minecraft.block.Blocks;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.goal.BreakBlockGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

public class AttackTurtleEggGoal extends BreakBlockGoal {
    protected final CreatureEntity attacker;
    public AttackTurtleEggGoal(CreatureEntity attacker, double speedModifier, int searchRange) {
        super(Blocks.TURTLE_EGG, attacker, speedModifier, searchRange);
        this.attacker = attacker;
    }

    public void playDestroyProgressSound(IWorld pLevel, BlockPos pPos) {
        pLevel.playSound((PlayerEntity)null, pPos, SoundEvents.ZOMBIE_DESTROY_EGG, SoundCategory.HOSTILE, 0.5F, 0.9F + this.attacker.getRandom().nextFloat() * 0.2F);
    }

    public void playBreakSound(World pLevel, BlockPos pPos) {
        pLevel.playSound((PlayerEntity)null, pPos, SoundEvents.TURTLE_EGG_BREAK, SoundCategory.BLOCKS, 0.7F, 0.9F + pLevel.random.nextFloat() * 0.2F);
    }

    public double acceptedDistance() {
         return 1.14D;
      }
}