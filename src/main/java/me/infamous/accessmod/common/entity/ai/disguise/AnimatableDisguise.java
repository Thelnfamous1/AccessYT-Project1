package me.infamous.accessmod.common.entity.ai.disguise;

import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.util.Constants;
import xyz.nucleoid.disguiselib.casts.EntityDisguise;

public interface AnimatableDisguise {

    String DISGUISE_LIB_TAG = "DisguiseLib";

    static EntityDisguise entityDisguise(AnimatableDisguise entity){
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
        EntityDisguise entityDisguise = entityDisguise(this);
        if(entityDisguise.isDisguised()) entityDisguise.removeDisguise();
    }

    /**
     * If the DisguiseLib tag is present, then we must be disguised
     */
    default void readDisguiseState(CompoundNBT pCompound) {
        if (pCompound.contains(DISGUISE_LIB_TAG, Constants.NBT.TAG_COMPOUND)) {
            this.setDisguiseState(DisguiseState.DISGUISED);
        } else{
            this.setDisguiseState(DisguiseState.REVEALED);
        }
    }

    SoundEvent getRevealSound();

    SoundEvent getDisguiseSound();

    default void setRevealed(){
        this.setDisguiseState(DisguiseState.REVEALED);
        EntityDisguise entityDisguise = entityDisguise(this);
        if(entityDisguise.isDisguised()) entityDisguise.removeDisguise();
    }

    default boolean isRevealed(){
        return this.getDisguiseState() == DisguiseState.REVEALED;
    }

    default void setDisguised(EntityType<?> disguiseType){
        this.setDisguiseState(DisguiseState.DISGUISED);
        entityDisguise(this).disguiseAs(disguiseType);
    }

    default boolean isDisguised(){
        return this.getDisguiseState() == DisguiseState.DISGUISED;
    }

    AnimatableDisguise.DisguiseState getDisguiseState();

    void setDisguiseState(AnimatableDisguise.DisguiseState disguiseState);

    boolean wantsToDisguise();

    boolean wantsToReveal();

    enum DisguiseState{
        REVEALED(false),
        DISGUISING(true),
        DISGUISED(false),
        REVEALING(true);

        private final boolean transitional;

        DisguiseState(boolean transitional) {
            this.transitional = transitional;
        }

        public boolean isTransitional() {
            return this.transitional;
        }
    }
}
