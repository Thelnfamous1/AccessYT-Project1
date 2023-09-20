package me.infamous.accessmod.mixin;

//import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import me.infamous.accessmod.duck.DuneSinker;
import me.infamous.accessmod.duck.Summonable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements DuneSinker, Summonable {

    @Shadow protected boolean jumping;

    @Shadow protected abstract void jumpFromGround();

    @Shadow private int noJumpDelay;

    private UUID summonerUUID;

    private LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    public UUID getSummonerUUID() {
        return this.summonerUUID;
    }

    @Override
    public void setSummonerUUID(UUID summonerUUID) {
        this.summonerUUID = summonerUUID;
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void handleReadSaveData(CompoundNBT pCompound, CallbackInfo ci){
        this.readSummoner(pCompound);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void handleAddSaveData(CompoundNBT pCompound, CallbackInfo ci){
        this.writeSummoner(pCompound);
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isAffectedByFluids()Z"))
    private void aiStep$updateSinkTimerIfJumping(CallbackInfo ci) {
        if(this.jumping && this.isSinking() && this.noJumpDelay <= 0){
            //this.jumpFromGround();
            this.setCanSinkTimer(Math.max(this.getCanSinkTimer() - 10, 0));
            this.noJumpDelay = 10;
        }
    }

    /*
    @ModifyExpressionValue(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isEyeInFluid(Lnet/minecraft/tags/ITag;)Z"))
    private boolean baseTick$checkDrown(boolean original) {
        return original || this.isSinking();
    }

    @ModifyExpressionValue(method = "baseTick", at = @At(value = "FIELD", target = "Lnet/minecraft/util/DamageSource;DROWN:Lnet/minecraft/util/DamageSource;"))
    private DamageSource baseTick$modifyDrownDamageSource(DamageSource original) {
        return this.isSinking() ? DuneSinker.SUFFOCATION : original;
    }
     */
}
