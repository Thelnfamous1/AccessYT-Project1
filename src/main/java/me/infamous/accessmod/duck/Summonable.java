package me.infamous.accessmod.duck;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

public interface Summonable {

    String SUMMONER_TAG = "Summoner";

    static Summonable cast(LivingEntity entity){
        return (Summonable) entity;
    }

    default boolean isSummoned() {
        return this.getSummonerUUID() != null;
    }

    UUID getSummonerUUID();

    void setSummonerUUID(UUID summonerUUID);

    default void writeSummoner(CompoundNBT tag){
        if(this.getSummonerUUID() != null){
            tag.putUUID(SUMMONER_TAG, this.getSummonerUUID());
        }
    }

    default void readSummoner(CompoundNBT tag){
        if(tag.hasUUID(SUMMONER_TAG)){
            this.setSummonerUUID(tag.getUUID(SUMMONER_TAG));
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
        Summonable targetSummonable = cast(target);
        return !targetSummonable.isSummoned() || targetSummonable.getSummoner(target.level) != mySummoner;
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
