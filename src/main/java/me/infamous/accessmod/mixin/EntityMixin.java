package me.infamous.accessmod.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.infamous.accessmod.datagen.AccessModTags;
import me.infamous.accessmod.duck.DuneSinker;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin implements DuneSinker {

    @Shadow public abstract double getEyeY();

    @Shadow public abstract BlockPos blockPosition();

    @Shadow protected abstract BlockPos getOnPos();

    @Shadow public World level;
    @Unique
    private boolean sunkByDune;
    @Unique
    private int sinkTimer;

    @Unique
    @Override
    public int getSinkTimer() {
        return this.sinkTimer;
    }

    @Unique
    @Override
    public void setSinkTimer(int sinkTimer) {
        this.sinkTimer = sinkTimer;
    }

    @Unique
    @Override
    public boolean isSunkByDune() {
        return this.sunkByDune;
    }

    @Inject(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;updateFluidOnEyes()V"))
    private void baseTick$updateSunkByDune(CallbackInfo ci) {
        if(this.sinkTimer > 0) this.sinkTimer--;
        this.sunkByDune = this.sinkTimer > 0
                && this.level
                .getBlockState(new BlockPos(this.blockPosition().getX(), this.getEyeY() - 0.11111111F, this.blockPosition().getZ()))
                .is(AccessModTags.DUNE_WRATH_SINK_IN);
    }

    @ModifyExpressionValue(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;is(Lnet/minecraft/tags/ITag;)Z"))
    private boolean move$checkOnDuneWrathSinkIn(boolean original) {
        if (original) return true;

        if(this.sinkTimer <= 0) return false;
        BlockPos onPos = this.getOnPos();
        BlockState onState = this.level.getBlockState(onPos);
        return onState.is(AccessModTags.DUNE_WRATH_SINK_IN);
    }
}