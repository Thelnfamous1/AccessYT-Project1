package me.infamous.accessmod.common.capability;

import me.infamous.accessmod.common.network.AccessModNetwork;
import me.infamous.accessmod.common.network.ClientboundSoulsSyncPacket;
import me.infamous.accessmod.duck.Summonable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Optional;

public interface SoulsCapability extends INBTSerializable<CompoundNBT> {

    String SOULS = "souls";
    int DEFAULT_LIMITED_LIFE_TICKS = 6000;

    static void syncSouls(LivingEntity summoner, Hand handHoldingSoulsItem, SoulsCapability souls) {
        if(summoner instanceof ServerPlayerEntity){
            ServerPlayerEntity summonerPlayer = (ServerPlayerEntity) summoner;
            int slot = handHoldingSoulsItem == Hand.MAIN_HAND ? summonerPlayer.inventory.selected : 40;
            AccessModNetwork.SYNC_CHANNEL.send(PacketDistributor.PLAYER.with(() -> summonerPlayer), new ClientboundSoulsSyncPacket(souls, slot));
        }
    }

    @Nullable
    Entity summon(World world);

    @Nullable
    default Entity summon(LivingEntity summoner, World world, Hand handHoldingSoulsItem){
        Entity summon = this.summon(world);

        syncSouls(summoner, handHoldingSoulsItem, this);

        if(summon instanceof MobEntity){
            MobEntity summonedMob = (MobEntity) summon;
            Summonable cast = Summonable.cast(summonedMob);
            cast.setSummonerUUID(summoner.getUUID());
            cast.setLimitedLife(DEFAULT_LIMITED_LIFE_TICKS);
        }
        return summon;
    }

    void addSummon(EntityType<?> type);

    default void addSummon(EntityType<?> type, LivingEntity summoner, Hand handHoldingSoulsItem){
        this.addSummon(type);
        syncSouls(summoner, handHoldingSoulsItem, this);
    }

    void clearAllSummons();

    Iterator<EntityType<?>> getIterator();

    int getTotalSouls();

    @Override
    default CompoundNBT serializeNBT() {
        CompoundNBT tag = new CompoundNBT();
        ListNBT souls = new ListNBT();
        Iterator<EntityType<?>> iterator = this.getIterator();
        while(iterator.hasNext()){
            EntityType<?> next = iterator.next();
            souls.add(StringNBT.valueOf(EntityType.getKey(next).toString()));
        }
        tag.put(SoulsCapability.SOULS, souls);
        return tag;
    }

    @Override
    default void deserializeNBT(CompoundNBT nbt) {
        this.clearAllSummons();
        ListNBT souls = nbt.getList(SoulsCapability.SOULS, Constants.NBT.TAG_STRING);
        for(int i = 0; i < souls.size(); i++){
            Optional<EntityType<?>> type = EntityType.byString(souls.getString(i));
            type.ifPresent(this::addSummon);
        }
    }
}