package me.infamous.accessmod.duck;

import me.infamous.accessmod.common.AccessModUtil;
import me.infamous.accessmod.datagen.AccessModTags;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import java.util.Random;

public interface DuneSubmergeable {

    boolean isSubmergedByDune();

    void updateSubmergedByDune();

    static void entityInsideByDune(BlockState state, World world, BlockPos pos, Entity entity){
        if(entity instanceof LivingEntity){
            LivingEntity living = (LivingEntity) entity;
            if (living.getFeetBlockState().is(AccessModTags.DUNE_WRATH_SINK_IN)) {
                entity.makeStuckInBlock(state, new Vector3d(0.6D, 0.4D, 0.6D));
            }
            if (!entity.isSpectator() && hasEntityMoved(entity)) {
                if (((DuneSubmergeable)entity).isSubmergedByDune()) {
                    living.hurt(AccessModUtil.SUFFOCATION, 1.0F);
                }
                if(world.getRandom().nextBoolean())
                    spawnParticles(world, state, new Vector3d(entity.getX(), pos.getY(), entity.getZ()));
            }
        }
    }

    static boolean hasEntityMoved(Entity entity) {
        return entity.xOld - entity.getX() >= 0.001 ||
                entity.yOld - entity.getY() >= 0.001 ||
                entity.zOld - entity.getZ() >= 0.001;
    }

    static void spawnParticles(World world, BlockState state, Vector3d pos) {
        if (world.isClientSide) {
            Random random = world.getRandom();
            for (int i = 0; i < random.nextInt(3); ++i) {
                world.addParticle(new BlockParticleData(ParticleTypes.FALLING_DUST, state),
                        pos.x+(MathHelper.nextFloat(random, -1.0F, 1.0F)),
                        pos.y+(MathHelper.nextFloat(random, -1.0F, 1.0F)),
                        pos.z+(MathHelper.nextFloat(random, -1.0F, 1.0F)),
                        (-1.0F + random.nextFloat() * 2.0F) / 12.0F,
                        0.05,
                        (-1.0F + random.nextFloat() * 2.0F) / 12.0F);
            }
        }
    }
}
