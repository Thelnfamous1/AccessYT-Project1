package me.infamous.accessmod.common.network;

import me.infamous.accessmod.duck.DuneSinker;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundDuneSinkPacket {

    private final int id;
    private final int sinkTimer;

    public ClientboundDuneSinkPacket(Entity entity, int sinkTimer){
        this.id = entity.getId();
        this.sinkTimer = sinkTimer;
    }

    public ClientboundDuneSinkPacket(PacketBuffer buffer){
        this.id = buffer.readInt();
        this.sinkTimer = buffer.readInt();
    }

    public static void encode(ClientboundDuneSinkPacket packet, PacketBuffer buffer){
        buffer.writeInt(packet.id);
        buffer.writeInt(packet.sinkTimer);
    }

    public static void handle(ClientboundDuneSinkPacket packet, Supplier<NetworkEvent.Context> context){
        context.get().enqueueWork(() -> {
            Entity entity = Minecraft.getInstance().level.getEntity(packet.id);
            if(entity != null){
                ((DuneSinker)entity).setCanSinkTimer(packet.sinkTimer);
            }
        });
        context.get().setPacketHandled(true);
    }
}
