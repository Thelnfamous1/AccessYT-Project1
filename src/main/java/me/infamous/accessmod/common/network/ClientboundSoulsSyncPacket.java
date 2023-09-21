package me.infamous.accessmod.common.network;

import me.infamous.accessmod.common.capability.SoulsCapability;
import me.infamous.accessmod.common.item.SoulScytheItem;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundSoulsSyncPacket {
   private final CompoundNBT soulsTag;
   private final int slot;

   public ClientboundSoulsSyncPacket(SoulsCapability souls, int slot) {
      this.soulsTag = souls.serializeNBT();
      this.slot = slot;
   }

   public ClientboundSoulsSyncPacket(PacketBuffer buffer) {
      this.soulsTag = buffer.readNbt();
      this.slot = buffer.readVarInt();
   }

   public static void encode(ClientboundSoulsSyncPacket packet, PacketBuffer pBuffer) {
      pBuffer.writeNbt(packet.soulsTag);
      pBuffer.writeVarInt(packet.slot);
   }

   public static void handle(ClientboundSoulsSyncPacket packet, Supplier<NetworkEvent.Context> context) {
      context.get().enqueueWork(() -> {
         PlayerEntity player = Minecraft.getInstance().player;
         int slot = packet.slot;
         ItemStack inventoryItem = player.inventory.getItem(slot);
         SoulsCapability souls = SoulScytheItem.getSouls(inventoryItem).orElse(null);
         //noinspection ConstantConditions
         if(souls != null){
            souls.deserializeNBT(packet.soulsTag);
         }
      });
      context.get().setPacketHandled(true);
   }
}