package me.infamous.accessmod.common.entity.ai.digger;

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
            DigState digState = DigState.byOrdinal(pCompound.getByte(DIG_STATE_TAG));
            if(!digState.isTransitional()) this.setDigState(digState);
        }
    }

    default void writeDigState(CompoundNBT pCompound) {
        if(!this.getDigState().isTransitional()) pCompound.putByte(DIG_STATE_TAG, (byte) this.getDigState().ordinal());
    }

    SoundEvent getEmergeSound();

    SoundEvent getDigSound();

    SoundEvent getDigFailSound();

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
        SURFACED(false),
        DIGGING(true),
        BURIED(false),
        EMERGING(true);

        private final boolean transitional;

        DigState(boolean transitional) {
            this.transitional = transitional;
        }

        public static DigState byOrdinal(int ordinal){
            if (ordinal < 0 || ordinal > values().length) {
                ordinal = 0;
            }

            return values()[ordinal];
        }

        public boolean isTransitional() {
            return this.transitional;
        }
    }
}
