package me.infamous.accessmod.common.entity.gobblefin;

public class SimpleTicker {

    private int ticksLeft;

    public SimpleTicker(int ticksLeft){
        this.ticksLeft = ticksLeft;
    }

    public void tick(){
        if(this.ticksLeft > 0){
            this.ticksLeft--;
        }
    }

    public boolean isActive(){
        return this.ticksLeft > 0;
    }
}
