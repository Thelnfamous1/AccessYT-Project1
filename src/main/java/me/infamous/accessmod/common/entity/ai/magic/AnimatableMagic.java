package me.infamous.accessmod.common.entity.ai.magic;

import net.minecraft.entity.MobEntity;
import net.minecraft.util.RangedInteger;
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

    MagicCooldownTracker<?, M> getMagicCooldowns();

    interface MagicType{

        int getId();

        int getWarmupTime();

        int getCastingTime();

        RangedInteger getCooldownTime();

        @Nullable
        SoundEvent getPrepareSound();
    }
}