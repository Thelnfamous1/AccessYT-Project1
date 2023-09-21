package me.infamous.accessmod.common.capability;

import me.infamous.accessmod.common.AccessModUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.*;

public class SoulsCapabilityImpl implements SoulsCapability {

    private final Queue<EntityType<?>> souls = new ArrayDeque<>();
    private final Map<EntityType<?>, Integer> counts = new HashMap<>();

    @Nullable
    @Override
    public Entity summon(World world) {
        EntityType<?> polledType = this.souls.poll();
        if(polledType == null){
            return null;
        }
        Entity summoned = polledType.create(world);
        Integer currentCount = counts.getOrDefault(polledType, 0);
        if(currentCount > 0){
            counts.put(polledType, currentCount - 1);
        }
        return summoned;
    }

    @Override
    public void addSummon(EntityType<?> type) {
        Integer currentCount = this.counts.getOrDefault(type, 0);
        if(currentCount < getSummonLimit(type)){
            this.souls.add(type);
            this.counts.put(type, currentCount + 1);
        }
    }

    @Override
    public void clearAllSummons() {
        this.souls.clear();
        this.counts.clear();
    }

    private static int getSummonLimit(EntityType<?> type) {
        return type.is(AccessModUtil.SCYTHE_CAN_HARVEST_SOUL_LIMITED) ? 1 : type.is(AccessModUtil.SCYTHE_CAN_HARVEST_SOUL) ? 5 : 0;
    }

    @Override
    public Iterator<EntityType<?>> getIterator() {
        return this.souls.iterator();
    }

    @Override
    public int getTotalSouls() {
        return this.souls.size();
    }

}