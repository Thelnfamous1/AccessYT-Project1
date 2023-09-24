package me.infamous.accessmod.common.registry;

import me.infamous.accessmod.AccessMod;
import me.infamous.accessmod.common.entity.ai.eater.Eater;
import me.infamous.accessmod.common.entity.ai.digger.Digger;
import me.infamous.accessmod.common.entity.ai.disguise.AnimatableDisguise;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.IDataSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DataSerializerEntry;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class AccessModDataSerializers {

    public static final DeferredRegister<DataSerializerEntry> DATA_SERIALIZERS = DeferredRegister.create(ForgeRegistries.DATA_SERIALIZERS, AccessMod.MODID);

    public static final RegistryObject<DataSerializerEntry> DIG_STATE = DATA_SERIALIZERS.register("dig_state", () -> new DataSerializerEntry(new IDataSerializer<Digger.DigState>() {
        @Override
        public void write(PacketBuffer pBuffer, Digger.DigState pValue) {
            pBuffer.writeEnum(pValue);
        }

        @Override
        public Digger.DigState read(PacketBuffer pBuffer) {
            return pBuffer.readEnum(Digger.DigState.class);
        }

        @Override
        public Digger.DigState copy(Digger.DigState pValue) {
            return pValue;
        }
    }));
    public static final RegistryObject<DataSerializerEntry> DISGUISE_STATE = DATA_SERIALIZERS.register("disguise_state", () -> new DataSerializerEntry(new IDataSerializer<AnimatableDisguise.DisguiseState>() {
        @Override
        public void write(PacketBuffer pBuffer, AnimatableDisguise.DisguiseState pValue) {
            pBuffer.writeEnum(pValue);
        }

        @Override
        public AnimatableDisguise.DisguiseState read(PacketBuffer pBuffer) {
            return pBuffer.readEnum(AnimatableDisguise.DisguiseState.class);
        }

        @Override
        public AnimatableDisguise.DisguiseState copy(AnimatableDisguise.DisguiseState pValue) {
            return pValue;
        }
    }));
    public static final RegistryObject<DataSerializerEntry> EAT_STATE = DATA_SERIALIZERS.register("eat_state", () -> new DataSerializerEntry(new IDataSerializer<Eater.EatState>() {
        @Override
        public void write(PacketBuffer pBuffer, Eater.EatState pValue) {
            pBuffer.writeEnum(pValue);
        }

        @Override
        public Eater.EatState read(PacketBuffer pBuffer) {
            return pBuffer.readEnum(Eater.EatState.class);
        }

        @Override
        public Eater.EatState copy(Eater.EatState pValue) {
            return pValue;
        }
    }));


    public static <T> IDataSerializer<T> getSerializer(RegistryObject<DataSerializerEntry> entry){
        return (IDataSerializer<T>) entry.get().getSerializer();
    }

    public static void register(IEventBus modEventBus){
        DATA_SERIALIZERS.register(modEventBus);
    }
}
