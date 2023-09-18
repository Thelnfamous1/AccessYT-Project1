package me.infamous.accessmod.common.entity.ai.sleep;

import net.minecraft.nbt.CompoundNBT;

public interface SleepingMob {

    String SLEEPING_TAG = "Sleeping";

    void setSleepingMob(boolean sleeping);

    default void writeSleepingNBT(CompoundNBT compoundNBT){
        compoundNBT.putBoolean(SLEEPING_TAG, this.isSleepingMob());
    }

    boolean isSleepingMob();

    default void readSleepingNBT(CompoundNBT compoundNBT){
        this.setSleepingMob(compoundNBT.getBoolean(SLEEPING_TAG));
    }

    boolean wantsToSleep();
}
