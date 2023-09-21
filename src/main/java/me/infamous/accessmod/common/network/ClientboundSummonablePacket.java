package me.infamous.accessmod.common.network;

import me.infamous.accessmod.duck.Summonable;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;

public class ClientboundSummonablePacket {

    private final int id;
    @Nullable
    private final UUID summonerUUID;

    public ClientboundSummonablePacket(MobEntity mob, @Nullable UUID summonerUUID){
        this.id = mob.getId();
        this.summonerUUID = summonerUUID;
    }

    public ClientboundSummonablePacket(PacketBuffer buffer){
        this.id = buffer.readInt();
        boolean hasSummonerUUID = buffer.readBoolean();
        if(hasSummonerUUID) {
            this.summonerUUID = buffer.readUUID();
        } else{
            this.summonerUUID = null;
        }
    }

    public static void encode(ClientboundSummonablePacket packet, PacketBuffer buffer){
        buffer.writeInt(packet.id);
        boolean hasSummonerUUID = packet.summonerUUID != null;
        buffer.writeBoolean(hasSummonerUUID);
        if (hasSummonerUUID) {
            buffer.writeUUID(packet.summonerUUID);
        }
    }

    public static void handle(ClientboundSummonablePacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            Entity entity = Minecraft.getInstance().level.getEntity(packet.id);
            if(entity instanceof MobEntity){
                Summonable.cast((MobEntity) entity).setSummonerUUID(packet.summonerUUID);
            }
        });
        context.get().setPacketHandled(true);
    }
}
