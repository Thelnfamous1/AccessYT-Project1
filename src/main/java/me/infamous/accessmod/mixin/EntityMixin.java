package me.infamous.accessmod.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.infamous.accessmod.datagen.AccessModTags;
import me.infamous.accessmod.duck.DuneSubmergeable;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin implements DuneSubmergeable {
    
    @Shadow
    public abstract World getCommandSenderWorld();
    
    @Shadow public abstract double getEyeY();

    @Shadow public abstract BlockPos blockPosition();

    @Shadow protected abstract BlockPos getOnPos();

    @Shadow public World level;
    private boolean submergedByDune;
    
    @Override
    public boolean isSubmergedByDune() {
        return this.submergedByDune;
    }
    
    @Override
    public void updateSubmergedByDune() {
        this.submergedByDune = this.getCommandSenderWorld()
            .getBlockState(new BlockPos(this.blockPosition().getX(), this.getEyeY() - 0.11111111F, this.blockPosition().getZ()))
            .is(AccessModTags.DUNE_WRATH_SINK_IN);
    }
    
    @Inject(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;updateFluidOnEyes()V"))
    private void baseTick$updateSubmergedByDune(CallbackInfo ci) {
        this.updateSubmergedByDune();
    }

    @ModifyExpressionValue(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;is(Lnet/minecraft/tags/ITag;)Z"))
    private boolean move$checkIsOnSubmergeable(boolean original) {
        if (original) return true;
        BlockPos onPos = this.getOnPos();
        BlockState onState = this.level.getBlockState(onPos);
        return onState.is(AccessModTags.DUNE_WRATH_SINK_IN);
    }
}