package me.infamous.accessmod.common.entity.ai.magic;

import net.minecraft.util.SoundEvent;

import javax.annotation.Nullable;

public interface AnimatableMagic<M extends AnimatableMagic.MagicType> {

    int getMagicAnimationTick();

    void setMagicAnimationTick(int magicAnimationTick);

    default void startMagicAnimation(M magicType){
        this.setMagicAnimationTick(magicType.getCastingTime());
    }

    default boolean isMagicAnimationInProgress() {
        return this.getMagicAnimationTick() > 0;
    }

    SoundEvent getUseMagicSound();

    M getCurrentMagicType();

    void setCurrentMagicType(M magicType);

    M getDefaultMagicType();

    default void resetMagicType(){
        this.setCurrentMagicType(this.getDefaultMagicType());
    }

    interface MagicType{

        int getId();

        int getWarmupTime();

        int getCastingTime();

        int getCooldownTime();

        @Nullable
        SoundEvent getPrepareSound();
    }
}