package me.infamous.accessmod.mixin;

import me.infamous.accessmod.duck.Summonable;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.UUID;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin extends LivingEntity implements Summonable {

    @Nullable
    private UUID summonerUUID;
    @Unique
    private boolean hasLimitedLife;
    @Unique
    private int limitedLifeTicks;

    protected MobEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public UUID getSummonerUUID() {
        return this.summonerUUID;
    }

    @Override
    public void setSummonerUUID(UUID summonerUUID) {
        this.summonerUUID = summonerUUID;
        Summonable.syncSummonerUUID((MobEntity) (Object) this);
    }

    @Override
    public void setLimitedLife(int limitedLifeTicks) {
        this.hasLimitedLife = true;
        this.limitedLifeTicks = limitedLifeTicks;
    }

    @Override
    public int getLimitedLifeTicks() {
        return this.limitedLifeTicks;
    }

    @Override
    public boolean hasLimitedLife() {
        return this.hasLimitedLife;
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void handleReadSaveData(CompoundNBT pCompound, CallbackInfo ci){
        this.readSummonableInfo(pCompound);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void handleAddSaveData(CompoundNBT pCompound, CallbackInfo ci){
        this.writeSummonableInfo(pCompound);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void handleTick(CallbackInfo ci){
        if (this.level.isClientSide && this.summonerUUID != null && this.tickCount % 50 == 0) {
            Vector3d vector3d = this.getDeltaMovement();
            this.level.addParticle(ParticleTypes.SOUL,
                    this.getX() + (this.random.nextDouble() - 0.5D) * (double)this.getBbWidth(),
                    this.getY(0.5D),
                    this.getZ() + (this.random.nextDouble() - 0.5D) * (double)this.getBbWidth(),
                    vector3d.x * -0.2D,
                    0.1D,
                    vector3d.z * -0.2D);
        }
        if (this.hasLimitedLife && --this.limitedLifeTicks <= 0) {
            this.kill();
        }
    }
}
