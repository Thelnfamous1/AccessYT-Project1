package me.infamous.accessmod.common.network;

import me.infamous.accessmod.common.item.SoulScytheItem;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundSoulScythePacket {
   private final ItemStack scythe;
   private final int slot;

   public ClientboundSoulScythePacket(ItemStack scythe, int slot) {
      this.scythe = scythe.copy();
      this.slot = slot;
   }

   public ClientboundSoulScythePacket(PacketBuffer buffer) {
      this.scythe = buffer.readItem();
      this.slot = buffer.readVarInt();
   }

   public static void encode(ClientboundSoulScythePacket packet, PacketBuffer pBuffer) {
      pBuffer.writeItem(packet.scythe);
      pBuffer.writeVarInt(packet.slot);
   }

   public static void handle(ClientboundSoulScythePacket packet, Supplier<NetworkEvent.Context> context) {
      context.get().enqueueWork(() -> {
         PlayerEntity player = Minecraft.getInstance().player;
         ItemStack sentScythe = packet.scythe;
         if (sentScythe.getItem() instanceof SoulScytheItem) {
            int slot = packet.slot;
            ItemStack inventoryItem = player.inventory.getItem(slot);
            if (inventoryItem.getItem() instanceof SoulScytheItem) {
               inventoryItem.readShareTag(sentScythe.getShareTag());
            }
         }
      });
      context.get().setPacketHandled(true);
   }
}