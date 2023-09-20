package me.infamous.accessmod.common.capability;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

public class SoulsCapabilityStorage implements Capability.IStorage<SoulsCapability> {

    @Nullable
    @Override
    public INBT writeNBT(Capability<SoulsCapability> capability, SoulsCapability instance, Direction side) {
        return instance.serializeNBT();
    }

    @Override
    public void readNBT(Capability<SoulsCapability> capability, SoulsCapability instance, Direction side, INBT nbt) {
        instance.deserializeNBT((CompoundNBT) nbt);
    }
}