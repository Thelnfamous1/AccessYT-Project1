package me.infamous.accessmod.common.entity.ai.eater;

import net.minecraft.entity.Entity;

import javax.annotation.Nullable;

public interface EatTargeting {
    boolean wantsToEat(Entity entity);

    void eat(Entity entity);

    void setEatTarget(@Nullable Entity target);

    @Nullable
    Entity getEatTarget();
}
