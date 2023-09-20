package me.infamous.accessmod.common.capability;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Optional;

public interface SoulsCapability extends INBTSerializable<CompoundNBT> {

    String SOULS = "souls";

    @Nullable
    Entity summon(LivingEntity summoner, World world);

    boolean addSummon(EntityType<?> type);

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