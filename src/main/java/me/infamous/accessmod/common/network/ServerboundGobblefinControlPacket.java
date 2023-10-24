package me.infamous.accessmod.common.network;

import me.infamous.accessmod.common.entity.gobblefin.Gobblefin;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundGobblefinControlPacket {
    private final GobblefinControl control;

    public ServerboundGobblefinControlPacket(GobblefinControl control){
        this.control = control;
    }

    public ServerboundGobblefinControlPacket(PacketBuffer buffer){
        this.control = buffer.readEnum(GobblefinControl.class);
    }

    public static void encode(ServerboundGobblefinControlPacket packet, PacketBuffer buffer){
        buffer.writeEnum(packet.control);
    }

    public static void handle(ServerboundGobblefinControlPacket packet, Supplier<NetworkEvent.Context> context){
        context.get().enqueueWork(() -> {
            ServerPlayerEntity sender = context.get().getSender();
            if(sender != null && sender.getVehicle() instanceof Gobblefin){
                Gobblefin vehicle = (Gobblefin) sender.getVehicle();
                switch (packet.control){
                    case GOBBLEFIN_START_BOOST:
                        vehicle.startManualBoosting();
                        break;
                    case GOBBLEFIN_STOP_BOOST:
                        vehicle.stopManualBoosting();
                        break;
                    case GOBBLEFIN_START_VORTEX:
                        vehicle.startManualVortex();
                        break;
                    case GOBBLEFIN_STOP_VORTEX:
                        vehicle.stopManualVortex();
                        break;
                    default:
                        throw new IllegalStateException("Invalid enum for ServerboundGobblefinControlPacket: " + packet.control);
                }
            }
        });
        context.get().setPacketHandled(true);
    }

    public enum GobblefinControl{
        GOBBLEFIN_START_BOOST,
        GOBBLEFIN_STOP_BOOST,
        GOBBLEFIN_START_VORTEX,
        GOBBLEFIN_STOP_VORTEX
    }
}
