package me.infamous.accessmod.common.capability;

import me.infamous.accessmod.AccessMod;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SoulsCapabilityProvider implements ICapabilitySerializable<CompoundNBT> {

    @CapabilityInject(SoulsCapability.class)
    public static final Capability<SoulsCapability> INSTANCE = null;
    private final LazyOptional<SoulsCapability> instance = LazyOptional.of(INSTANCE::getDefaultInstance);

    public static final ResourceLocation IDENTIFIER = new ResourceLocation(AccessMod.MODID, "souls");

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == INSTANCE ? instance.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundNBT serializeNBT() {
        return (CompoundNBT) INSTANCE.getStorage().writeNBT(INSTANCE, this.instance.orElseThrow(() -> new IllegalArgumentException("LazyOptional must not be empty!")), null);
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        INSTANCE.getStorage().readNBT(INSTANCE, this.instance.orElseThrow(() -> new IllegalArgumentException("LazyOptional must not be empty!")), null, nbt);
    }
}
