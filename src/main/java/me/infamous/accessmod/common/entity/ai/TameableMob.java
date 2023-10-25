package me.infamous.accessmod.common.entity.ai;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.UUID;

public interface TameableMob {

    String OWNER_TAG = "Owner";
    int SUCCESSFUL_TAME_ID = 7;
    int FAILED_TAME_ID = 6;

    boolean isOwnedBy(LivingEntity pEntity, World level);

    default boolean wantsToAttack(LivingEntity pTarget, LivingEntity pOwner, World level) {
        if (pTarget instanceof TameableEntity) {
            TameableEntity tameTarget = (TameableEntity)pTarget;
            return !tameTarget.isTame() || tameTarget.getOwner() != pOwner;
        } else  if (pTarget instanceof TameableMob) {
            TameableMob tameTarget = (TameableMob)pTarget;
            return !tameTarget.isTame() || tameTarget.getOwner(level) != pOwner;
        } else if (pTarget instanceof PlayerEntity && pOwner instanceof PlayerEntity && !((PlayerEntity)pOwner).canHarmPlayer((PlayerEntity)pTarget)) {
            return false;
        } else return !(pTarget instanceof AbstractHorseEntity) || !((AbstractHorseEntity) pTarget).isTamed();
    }

    @Nullable
    UUID getOwnerUUID();

    void setOwnerUUID(@Nullable UUID pUniqueId);

    default void writeOwner(CompoundNBT pCompound){
        if (this.getOwnerUUID() != null) {
            pCompound.putUUID(OWNER_TAG, this.getOwnerUUID());
        }
    }

    default void readOwner(CompoundNBT pCompound){
        UUID ownerUUID = null;
        if (pCompound.hasUUID(OWNER_TAG)) {
            ownerUUID = pCompound.getUUID(OWNER_TAG);
        }

        if (ownerUUID != null) {
            try {
                this.setOwnerUUID(ownerUUID);
                this.setTame(true);
            } catch (Throwable throwable) {
                this.setTame(false);
            }
        }
    }



    default void tame(PlayerEntity pPlayer) {
        this.setTame(true);
        this.setOwnerUUID(pPlayer.getUUID());
        if (pPlayer instanceof ServerPlayerEntity && this instanceof AnimalEntity) {
            CriteriaTriggers.TAME_ANIMAL.trigger((ServerPlayerEntity)pPlayer, (AnimalEntity) this);
        }

    }

    @Nullable
    default LivingEntity getOwner(World level) {
        try {
            UUID ownerUUID = this.getOwnerUUID();
            return ownerUUID == null ? null : level.getPlayerByUUID(ownerUUID);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    void setTame(boolean tame);

    boolean isTame();
}
