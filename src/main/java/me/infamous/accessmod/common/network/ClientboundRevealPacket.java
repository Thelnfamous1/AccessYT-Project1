package me.infamous.accessmod.common.network;

import me.infamous.accessmod.common.entity.ai.disguise.AnimatableDisguise;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundRevealPacket<T extends Entity & AnimatableDisguise> {

    private final int id;

    public ClientboundRevealPacket(T entity){
        this.id = entity.getId();
    }

    public ClientboundRevealPacket(PacketBuffer buffer){
        this.id = buffer.readInt();
    }

    public static void encode(ClientboundRevealPacket<?> packet, PacketBuffer buffer){
        buffer.writeInt(packet.id);
    }

    public static void handle(ClientboundRevealPacket<?> packet, Supplier<NetworkEvent.Context> context){
        context.get().enqueueWork(() -> {
            Entity entity = Minecraft.getInstance().level.getEntity(packet.id);
            if(entity instanceof AnimatableDisguise){
                ((AnimatableDisguise)entity).setRevealing();
            }
        });
        context.get().setPacketHandled(true);
    }
}
