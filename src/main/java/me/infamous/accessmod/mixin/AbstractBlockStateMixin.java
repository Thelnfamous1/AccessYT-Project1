package me.infamous.accessmod.mixin;

import me.infamous.accessmod.duck.DuneSinker;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class AbstractBlockStateMixin {

    @Shadow protected abstract BlockState asState();

    @Inject(method = "entityInside", at = @At("RETURN"))
    private void handleEntityInside(World pLevel, BlockPos pPos, Entity pEntity, CallbackInfo ci){
        DuneSinker.entityInside(this.asState(), pLevel, pPos, pEntity);
    }

    @Inject(method = "getCollisionShape(Lnet/minecraft/world/IBlockReader;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/shapes/ISelectionContext;)Lnet/minecraft/util/math/shapes/VoxelShape;",
            at = @At("HEAD"), cancellable = true)
    private void handleGetCollisionShape(IBlockReader pLevel, BlockPos pPos, ISelectionContext pContext, CallbackInfoReturnable<VoxelShape> cir){
        DuneSinker.getCollisionShape(this.asState(), pContext, cir);
    }

}
