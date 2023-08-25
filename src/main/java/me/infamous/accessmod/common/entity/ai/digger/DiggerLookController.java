package me.infamous.accessmod.common.entity.ai.digger;

import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.controller.LookController;

public class DiggerLookController<T extends MobEntity & Digger> extends LookController {
    private final T digger;

    public DiggerLookController(T mob) {
        super(mob);
        this.digger = mob;
    }

    @Override
    public void tick() {
        if(!this.digger.isSurfaced()){
            super.tick();
        }
    }
}
