package me.infamous.accessmod.common.entity.gobblefin;

public class SimpleTicker {

    private int ticks;

    public SimpleTicker(int ticks){
        this.ticks = ticks;
    }

    public void tick(){
        if(this.ticks > 0){
            this.ticks--;
        }
    }

    public boolean isActive(){
        return this.ticks > 0;
    }
}
