package me.infamous.accessmod.common.network;

import me.infamous.accessmod.AccessMod;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.Optional;

public class AccessModNetwork {
    private static final ResourceLocation CHANNEL_NAME = new ResourceLocation(AccessMod.MODID, "sync_channel");
    private static final String PROTOCOL_VERSION = "1.0";
    public static final SimpleChannel SYNC_CHANNEL = NetworkRegistry.newSimpleChannel(
            CHANNEL_NAME, () -> "1.0",
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    private static int INDEX = 0;

    public static void init() {
        SYNC_CHANNEL.registerMessage(INDEX++, ClientboundDuneSinkPacket.class, ClientboundDuneSinkPacket::encode, ClientboundDuneSinkPacket::new, ClientboundDuneSinkPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        SYNC_CHANNEL.registerMessage(INDEX++, ServerboundDuneJumpPacket.class, ServerboundDuneJumpPacket::encode, ServerboundDuneJumpPacket::new, ServerboundDuneJumpPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
}
