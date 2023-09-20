package me.infamous.accessmod.common.capability;

import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Optional;

public class SoulsCapabilityStorage implements Capability.IStorage<SoulsCapability> {

    private static final String SOULS = "souls";

    @Nullable
    @Override
    public INBT writeNBT(Capability<SoulsCapability> capability, SoulsCapability instance, Direction side) {
        CompoundNBT tag = new CompoundNBT();
        ListNBT souls = new ListNBT();
        Iterator<EntityType<?>> iterator = instance.getIterator();
        while(iterator.hasNext()){
            EntityType<?> next = iterator.next();
            souls.add(StringNBT.valueOf(EntityType.getKey(next).toString()));
        }
        tag.put(SOULS, souls);
        return tag;
    }

    @Override
    public void readNBT(Capability<SoulsCapability> capability, SoulsCapability instance, Direction side, INBT nbt) {
        CompoundNBT tag = (CompoundNBT) nbt;
        ListNBT souls = tag.getList(SOULS, Constants.NBT.TAG_STRING);
        for(int i = 0; i < souls.size(); i++){
            Optional<EntityType<?>> type = EntityType.byString(souls.getString(i));
            type.ifPresent(instance::addSummon);
        }
    }
}