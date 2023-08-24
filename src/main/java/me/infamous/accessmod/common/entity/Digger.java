package me.infamous.accessmod.common.entity;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.util.Constants;

public interface Digger {

    String DIG_STATE_TAG = "DigState";

    default boolean isDigging(){
        return this.getDigState() == DigState.DIGGING;
    }

    default boolean isEmerging(){
        return this.getDigState() == DigState.EMERGING;
    }

    default void setDigging(){
        this.setDigState(DigState.DIGGING);
    }

    default void setEmerging(){
        this.setDigState(DigState.EMERGING);
    }

    default void readDigState(CompoundNBT pCompound) {
        if (pCompound.contains(DIG_STATE_TAG, Constants.NBT.TAG_ANY_NUMERIC)) {
            this.setDigState(DigState.byOrdinal(pCompound.getByte(DIG_STATE_TAG)));
        }
    }

    default void writeDigState(CompoundNBT pCompound) {
        pCompound.putByte(DIG_STATE_TAG, (byte) this.getDigState().ordinal());
    }

    SoundEvent getEmergeSound();

    SoundEvent getDigSound();

    SoundEvent getAgitatedSound();

    default void setSurfaced(){
        this.setDigState(DigState.SURFACED);
    }

    default boolean isSurfaced(){
        return this.getDigState() == DigState.SURFACED;
    }

    default void setBuried(){
        this.setDigState(DigState.BURIED);
    }

    default boolean isBuried(){
        return this.getDigState() == DigState.BURIED;
    }

    DigState getDigState();

    void setDigState(DigState digState);

    boolean wantsToDig();

    boolean wantsToEmerge();

    enum DigState{
        SURFACED,
        DIGGING,
        BURIED,
        EMERGING;

        public static DigState byOrdinal(int ordinal){
            if (ordinal < 0 || ordinal > values().length) {
                ordinal = 0;
            }

            return values()[ordinal];
        }
    }
}
