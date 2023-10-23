package me.infamous.accessmod.common.entity.ai.eater;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.util.Constants;

public interface Eater {

    String EAT_STATE_TAG = "EatState";

    default void readEatState(CompoundNBT pCompound) {
        if (pCompound.contains(EAT_STATE_TAG, Constants.NBT.TAG_ANY_NUMERIC)) {
            EatState eatState = EatState.byOrdinal(pCompound.getByte(EAT_STATE_TAG));
            if(!eatState.isTransitional()) this.setEatState(eatState);
        }
    }

    default void writeEatState(CompoundNBT pCompound) {
        if(!this.getEatState().isTransitional()) pCompound.putByte(EAT_STATE_TAG, (byte) this.getEatState().ordinal());
    }

    default boolean isMouthOpen(){
        return this.getEatState() == EatState.MOUTH_OPEN;
    }

    default void setMouthOpen(){
        this.setEatState(EatState.MOUTH_OPEN);
    }

    default boolean isMouthClosed(){
        return this.getEatState() == EatState.MOUTH_CLOSED;
    }

    default void setMouthClosed(){
        this.setEatState(EatState.MOUTH_CLOSED);
    }

    default boolean isSuckingUp(){
        return this.getEatState() == EatState.SUCKING_UP;
    }

    default void setSuckingUp(){
        this.setEatState(EatState.SUCKING_UP);
    }

    default boolean isSwallowing(){
        return this.getEatState() == EatState.SWALLOWING;
    }

    default void setSwallowing(){
        this.setEatState(EatState.SWALLOWING);
    }

    default boolean isThrowingUp(){
        return this.getEatState() == EatState.THROWING_UP;
    }

    default void setThrowingUp(){
        this.setEatState(EatState.THROWING_UP);
    }

    SoundEvent getEatSound();

    EatState getEatState();

    void setEatState(EatState eatState);

    int getEatActionTimer();

    int getEatActionPoint();

    enum EatState {
        MOUTH_CLOSED(false),
        MOUTH_OPEN(false),
        SUCKING_UP(true),
        SWALLOWING(true),
        THROWING_UP(true);

        private final boolean transitional;

        EatState(boolean transitional) {
            this.transitional = transitional;
        }

        public static EatState byOrdinal(int ordinal){
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
