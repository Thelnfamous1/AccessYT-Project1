package me.infamous.accessmod.common.capability;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Iterator;

public interface SoulsCapability {

    @Nullable
    Entity summon(LivingEntity summoner, World world);

    boolean addSummon(EntityType<?> type);

    Iterator<EntityType<?>> getIterator();
}