package me.infamous.accessmod.common.network;

import com.google.common.collect.Lists;
import me.infamous.accessmod.common.entity.gobblefin.VortexHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class ClientboundVortexPacket {
   private final Vector3d position;
   private final float power;
   private final List<BlockPos> blocksToBlow;

   public ClientboundVortexPacket(Vector3d position, float pPower, List<BlockPos> pToBlow) {
      this.position = position;
      this.power = pPower;
      this.blocksToBlow = Lists.newArrayList(pToBlow);

   }

   public ClientboundVortexPacket(PacketBuffer buffer) {
      this.position = new Vector3d(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());

      this.power = buffer.readFloat();
      int numBlocksToBlow = buffer.readInt();
      this.blocksToBlow = Lists.newArrayListWithCapacity(numBlocksToBlow);
      Vector3d position = this.getPosition();
      int sourceX = MathHelper.floor(position.x);
      int sourceY = MathHelper.floor(position.y);
      int sourceZ = MathHelper.floor(position.z);

      for(int idx = 0; idx < numBlocksToBlow; ++idx) {
         int targetX = buffer.readByte() + sourceX;
         int targetY = buffer.readByte() + sourceY;
         int targetZ = buffer.readByte() + sourceZ;
         this.blocksToBlow.add(new BlockPos(targetX, targetY, targetZ));
      }
   }

   public static void encode(ClientboundVortexPacket packet, PacketBuffer pBuffer) {
      pBuffer.writeFloat((float) packet.position.x);
      pBuffer.writeFloat((float) packet.position.y);
      pBuffer.writeFloat((float) packet.position.z);

      pBuffer.writeFloat(packet.power);

      pBuffer.writeInt(packet.blocksToBlow.size());
      Vector3d position = packet.getPosition();
      int sourceX = MathHelper.floor(position.x);
      int sourceY = MathHelper.floor(position.y);
      int sourceZ = MathHelper.floor(position.z);

      for(BlockPos blockToBlow : packet.blocksToBlow) {
         int xDist = blockToBlow.getX() - sourceX;
         int yDist = blockToBlow.getY() - sourceY;
         int zDist = blockToBlow.getZ() - sourceZ;
         pBuffer.writeByte(xDist);
         pBuffer.writeByte(yDist);
         pBuffer.writeByte(zDist);
      }
   }

   public void handle(Supplier<NetworkEvent.Context> context) {
      context.get().enqueueWork(() -> {
         VortexHelper.createClientVortex(this, Minecraft.getInstance().level, Minecraft.getInstance().player);
      });
      context.get().setPacketHandled(true);
   }

   public Vector3d getPosition() {
      return this.position;
   }

   public float getPower() {
      return this.power;
   }

   public List<BlockPos> getBlocksToBlow() {
      return this.blocksToBlow;
   }
}