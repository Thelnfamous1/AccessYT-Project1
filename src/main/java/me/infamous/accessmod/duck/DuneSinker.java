package me.infamous.accessmod.duck;

import me.infamous.accessmod.common.network.AccessModNetwork;
import me.infamous.accessmod.common.network.ClientboundDuneSinkPacket;
import me.infamous.accessmod.datagen.AccessModTags;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.FallingBlockEntity;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.EntitySelectionContext;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.PacketDistributor;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

public interface DuneSinker {
    VoxelShape FALLING_COLLISION_SHAPE = VoxelShapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.9F, 1.0D);
    Vector3d POWDER_SNOW_MOTION_MULTIPLIER = new Vector3d(0.9F, 1.5F, 0.9F);
    Vector3d BERRY_BUSH_MOTION_MULTIPLIER = new Vector3d(0.8F, 0.75D, 0.8F);
    Vector3d COBWEB_MOTION_MULTIPLIER = new Vector3d(0.25F, 0.05F, 0.25F);
    DamageSource SUFFOCATION = new DamageSource("suffocation").bypassArmor();

    static void entityInside(BlockState state, World world, BlockPos blockPos, Entity entity){
        if (canSink(entity) && entity instanceof LivingEntity && ((LivingEntity) entity).getFeetBlockState().is(AccessModTags.DUNE_WRATH_SINK_IN)) {
            entity.makeStuckInBlock(state, COBWEB_MOTION_MULTIPLIER);
            if (world.isClientSide) {
                Random random = world.getRandom();
                boolean moved = entity.xOld != entity.getX() || entity.zOld != entity.getZ();
                if (moved && random.nextBoolean()) {
                    world.addParticle(new BlockParticleData(ParticleTypes.BLOCK, state),
                            entity.getX(), blockPos.getY() + 1, entity.getZ(),
                            MathHelper.nextFloat(random, -1.0F, 1.0F) * 0.083333336F, 0.05F, MathHelper.nextFloat(random, -1.0F, 1.0F) * 0.083333336F);
                }
            }
        }

        //entity.setIsInPowderSnow(true);
    }

    static void getCollisionShape(BlockState blockState, ISelectionContext pContext, CallbackInfoReturnable<VoxelShape> cir) {
        if (pContext instanceof EntitySelectionContext) {
            EntitySelectionContext esc = (EntitySelectionContext) pContext;
            Entity entity = esc.getEntity();
            if (entity != null && canSink(entity) && blockState.is(AccessModTags.DUNE_WRATH_SINK_IN)) {
                if (entity.fallDistance > 2.5F) {
                    cir.setReturnValue(FALLING_COLLISION_SHAPE);
                }

                boolean fallingBlock = entity instanceof FallingBlockEntity;
                if (!fallingBlock) {
                    cir.setReturnValue(VoxelShapes.empty());
                }
            }
        }
    }

    static void sink(Entity target, int sinkTimer){
        ((DuneSinker)target).setSinkTimer(sinkTimer);
        if(!target.level.isClientSide){
            AccessModNetwork.SYNC_CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target), new ClientboundDuneSinkPacket(target, sinkTimer));
        }
    }

    static boolean canSink(Entity entity){
        return ((DuneSinker)entity).getSinkTimer() > 0;
    }

    int getSinkTimer();

    void setSinkTimer(int sinkTimer);

    boolean isSunkByDune();

}
