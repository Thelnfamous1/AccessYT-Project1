package me.infamous.accessmod.common.entity.ai.disguise;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.util.Constants;
import xyz.nucleoid.disguiselib.casts.EntityDisguise;

public interface AnimatableDisguise {

    String DISGUISE_STATE = "DisguiseState";

    static <T extends Entity & AnimatableDisguise> EntityDisguise cast(T entity){
        return (EntityDisguise) entity;
    }

    default boolean isDisguising(){
        return this.getDisguiseState() == DisguiseState.DISGUISING;
    }

    default boolean isRevealing(){
        return this.getDisguiseState() == DisguiseState.REVEALING;
    }

    default void setDisguising(){
        this.setDisguiseState(DisguiseState.DISGUISING);
    }

    default void setRevealing(){
        this.setDisguiseState(DisguiseState.REVEALING);
    }

    default void readDisguiseState(CompoundNBT pCompound) {
        if (pCompound.contains(DISGUISE_STATE, Constants.NBT.TAG_ANY_NUMERIC)) {
            this.setDisguiseState(AnimatableDisguise.DisguiseState.byOrdinal(pCompound.getByte(DISGUISE_STATE)));
        }
    }

    default void writeDisguiseState(CompoundNBT pCompound) {
        pCompound.putByte(DISGUISE_STATE, (byte) this.getDisguiseState().ordinal());
    }

    SoundEvent getRevealSound();

    SoundEvent getDisguiseSound();

    default void setRevealed(){
        this.setDisguiseState(DisguiseState.REVEALED);
    }

    default boolean isRevealed(){
        return this.getDisguiseState() == DisguiseState.REVEALED;
    }

    default void setDisguised(){
        this.setDisguiseState(DisguiseState.DISGUISED);
    }

    default boolean isDisguised(){
        return this.getDisguiseState() == DisguiseState.DISGUISED;
    }

    AnimatableDisguise.DisguiseState getDisguiseState();

    void setDisguiseState(AnimatableDisguise.DisguiseState disguiseState);

    boolean wantsToDisguise();

    boolean wantsToReveal();

    enum DisguiseState{
        REVEALED,
        DISGUISING,
        DISGUISED,
        REVEALING;

        public static AnimatableDisguise.DisguiseState byOrdinal(int ordinal){
            if (ordinal < 0 || ordinal > values().length) {
                ordinal = 0;
            }

            return values()[ordinal];
        }
    }
}
