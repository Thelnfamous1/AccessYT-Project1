package me.infamous.accessmod.common.capability;

import me.infamous.accessmod.common.AccessModUtil;
import me.infamous.accessmod.common.entity.ai.summonable.FollowSummonerGoal;
import me.infamous.accessmod.common.entity.ai.summonable.SummonerHurtByTargetGoal;
import me.infamous.accessmod.common.entity.ai.summonable.SummonerHurtTargetGoal;
import me.infamous.accessmod.duck.Summonable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.pathfinding.FlyingPathNavigator;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.*;

public class SoulsCapabilityImpl implements SoulsCapability {

    private final Queue<EntityType<?>> souls = new ArrayDeque<>();
    private final Map<EntityType<?>, Integer> counts = new HashMap<>();

    @Nullable
    @Override
    public Entity summon(LivingEntity summoner, World world) {
        EntityType<?> polledType = this.souls.poll();
        if(polledType == null){
            return null;
        }
        Entity summoned = polledType.create(world);
        Integer currentCount = counts.getOrDefault(polledType, 0);
        if(currentCount > 0){
            counts.put(polledType, currentCount - 1);
        }

        if(summoned instanceof MobEntity){
            MobEntity summonedMob = (MobEntity) summoned;
            Summonable.cast(summonedMob).setSummonerUUID(summoner.getUUID());
            summonedMob.goalSelector.addGoal(1, new FollowSummonerGoal(summonedMob, 1.0D, 2, 10, summonedMob.getNavigation() instanceof FlyingPathNavigator));
            summonedMob.targetSelector.addGoal(1, new SummonerHurtByTargetGoal(summonedMob));
            summonedMob.targetSelector.addGoal(2, new SummonerHurtTargetGoal(summonedMob));
        }
        return summoned;
    }

    @Override
    public boolean addSummon(EntityType<?> type) {
        Integer currentCount = this.counts.getOrDefault(type, 0);
        if(currentCount < getSummonLimit(type)){
            this.souls.add(type);
            this.counts.put(type, currentCount + 1);
            return true;
        } else{
            return false;
        }
    }

    private static int getSummonLimit(EntityType<?> type) {
        return type.is(AccessModUtil.SCYTHE_CAN_HARVEST_SOUL_LIMITED) ? 1 : type.is(AccessModUtil.SCYTHE_CAN_HARVEST_SOUL) ? 5 : 0;
    }

    @Override
    public Iterator<EntityType<?>> getIterator() {
        return this.souls.iterator();
    }
}