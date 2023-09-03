package me.infamous.accessmod.mixin;

import me.infamous.accessmod.duck.DuneSubmergeable;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class AbstractBlockStateMixin {

    @Shadow protected abstract BlockState asState();

    @Inject(method = "entityInside", at = @At("RETURN"))
    private void handleEntityInside(World pLevel, BlockPos pPos, Entity pEntity, CallbackInfo ci){
        DuneSubmergeable.entityInsideByDune(this.asState(), pLevel, pPos, pEntity);
    }
}
