package me.infamous.accessmod.duck;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

public interface Summonable {

    String SUMMONER_TAG = "Summoner";
    String LIFE_TICKS_TAG = "LifeTicks";

    static Summonable cast(MobEntity entity){
        return (Summonable) entity;
    }

    default boolean isSummoned() {
        return this.getSummonerUUID() != null;
    }

    UUID getSummonerUUID();

    void setSummonerUUID(UUID summonerUUID);

    void setLimitedLife(int limitedLifeTicks);

    int getLimitedLifeTicks();

    boolean hasLimitedLife();

    default void writeSummonableInfo(CompoundNBT tag){
        if(this.getSummonerUUID() != null){
            tag.putUUID(SUMMONER_TAG, this.getSummonerUUID());
        }

        if (tag.contains(LIFE_TICKS_TAG)) {
            this.setLimitedLife(tag.getInt(LIFE_TICKS_TAG));
        }
    }

    default void readSummonableInfo(CompoundNBT tag){
        if(tag.hasUUID(SUMMONER_TAG)){
            this.setSummonerUUID(tag.getUUID(SUMMONER_TAG));
        }

        if (this.hasLimitedLife()) {
            tag.putInt(LIFE_TICKS_TAG, this.getLimitedLifeTicks());
        }
    }

    default LivingEntity getSummoner(World world){
        try {
            UUID summonerUUID = this.getSummonerUUID();
            return summonerUUID == null ? null : world.getPlayerByUUID(summonerUUID);
        } catch (IllegalArgumentException illegalargumentexception) {
            return null;
        }
    }

    default boolean canSummonableAttack(LivingEntity pTarget) {
        return !this.isSummonedBy(pTarget);
    }

    default boolean isSummonedBy(LivingEntity pEntity) {
        return pEntity == this.getSummoner(pEntity.level);
    }

    default boolean doesSummonableWantToAttack(LivingEntity target, LivingEntity mySummoner) {
        if(target instanceof MobEntity){
            MobEntity mobTarget = (MobEntity) target;
            Summonable targetSummonable = cast(mobTarget);
            return !targetSummonable.isSummoned() || targetSummonable.getSummoner(mobTarget.level) != mySummoner;
        } else{
            return true;
        }
    }

    default Optional<Boolean> isSummonableAlliedTo(Entity pEntity) {
        if (this.isSummoned()) {
            LivingEntity summoner = this.getSummoner(pEntity.level);
            if (pEntity == summoner) {
                return Optional.of(true);
            }

            if (summoner != null) {
                return Optional.of(summoner.isAlliedTo(pEntity));
            }
        }

        return Optional.empty();
    }
}
