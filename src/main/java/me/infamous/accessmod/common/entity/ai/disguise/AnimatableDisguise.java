package me.infamous.accessmod.common.entity.ai.disguise;

import net.minecraft.entity.Entity;
import xyz.nucleoid.disguiselib.casts.EntityDisguise;

public interface AnimatableDisguise {

    static <T extends Entity & AnimatableDisguise> EntityDisguise cast(T entity){
        return (EntityDisguise) entity;
    }
}
