package me.infamous.accessmod.common.entity.ai.eater;

import me.infamous.accessmod.common.entity.gobblefin.Vortex;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Vector3d;

import javax.annotation.Nullable;

public interface VortexEater{

    byte VORTEX_EVENT_ID = 21;

    default boolean isMouthOpen(){
        return this.getEatState() == EatState.MOUTH_OPEN;
    }

    default void setMouthOpen(){
        this.setEatState(EatState.MOUTH_OPEN);
        this.setEatActionTimer(this.getEatActionDuration(EatState.MOUTH_OPEN));
        this.setActiveVortex(null);
    }

    default boolean isMouthClosed(){
        return this.getEatState() == EatState.MOUTH_CLOSED;
    }

    default void setMouthClosed(){
        this.setEatState(EatState.MOUTH_CLOSED);
        this.setEatActionTimer(this.getEatActionDuration(EatState.MOUTH_CLOSED));
        this.setActiveVortex(null);
    }

    default boolean isSuckingUp(){
        return this.getEatState() == EatState.SUCKING_UP;
    }

    default void setSuckingUp(boolean breakBlocks){
        this.setEatState(EatState.SUCKING_UP);
        this.setEatActionTimer(this.getEatActionDuration(EatState.SUCKING_UP));
        this.setActiveVortex(this.createDefaultVortex(breakBlocks));
    }

    default boolean isSwallowing(){
        return this.getEatState() == EatState.SWALLOWING;
    }

    default void setSwallowing(){
        this.setEatState(EatState.SWALLOWING);
        this.setEatActionTimer(this.getEatActionDuration(EatState.SWALLOWING));
    }

    default boolean isThrowingUp(){
        return this.getEatState() == EatState.THROWING_UP;
    }

    default void setThrowingUp(){
        this.setEatState(EatState.THROWING_UP);
        this.setEatActionTimer(this.getEatActionDuration(EatState.THROWING_UP));
        this.setActiveVortex(null);
    }

    default void updateEating(boolean isServerSide, boolean manual) {
        if(this.getEatActionTimer() > 0){
            this.setEatActionTimer(this.getEatActionTimer() - 1);
            if(this.getEatActionTimer() == 0 && isServerSide){
                if(this.isSuckingUp()){
                    this.setSwallowing();
                } else if(this.isSwallowing()){
                    if(manual) {
                        this.setSuckingUp(true);
                    } else{
                        // reset
                        this.setMouthClosed();
                    }
                } else if(this.isThrowingUp()){
                    if(manual) {
                        this.setSuckingUp(true);
                    } else{
                        // reset
                        this.setMouthClosed();
                    }
                }
            }
        }
        if(this.getActiveVortex() != null && isServerSide){
            Vector3d mouthPosition = this.getMouthPosition();
            this.getActiveVortex().setPosition(mouthPosition);
            this.getActiveVortex().tick();
            if(this.isSwallowing() && this.getEatActionTimer() == this.getEatActionPoint(EatState.SWALLOWING)){
                this.playEatSound();
                this.getActiveVortex().getHitEntities().forEach(target -> {
                    if(this.canEat(target) && this.isWithinEatRange(target)){
                        this.eat(target);
                    }
                });
            }
        }
    }

    boolean isWithinEatRange(Entity target);

    boolean isWithinVortexRange(Entity target);

    void playEatSound();

    EatState getEatState();

    void setEatState(EatState eatState);

    int getEatActionTimer();

    void setEatActionTimer(int eatActionTimer);

    int getEatActionDuration(EatState eatState);

    int getEatActionPoint(EatState eatState);

    Vector3d getMouthPosition();

    @Nullable
    Vortex getActiveVortex();

    void setActiveVortex(@Nullable Vortex vortex);

    Vortex createDefaultVortex(boolean breakBlocks);

    boolean canEat(Entity target);

    void eat(Entity target);

    default float getVortexAnimationScale(float partialTicks){
        int suckingUpDuration = this.getEatActionDuration(EatState.SUCKING_UP);
        return ((float)(suckingUpDuration - this.getEatActionTimer()) + partialTicks) / (float) suckingUpDuration;
    }

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
