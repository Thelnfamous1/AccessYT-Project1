package me.infamous.accessmod.common.network;

import com.google.common.collect.Lists;
import me.infamous.accessmod.common.entity.gobblefin.VortexHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

public class ClientboundVortexPacket {
   private final Vector3d position;
   private final float power;
   private final List<BlockPos> blocksToBlow;
   private float knockbackX;
   private float knockbackY;
   private float knockbackZ;

   public ClientboundVortexPacket(Vector3d position, float pPower, List<BlockPos> pToBlow, @Nullable Vector3d playerKnockbackVec) {
      this.position = position;
      this.power = pPower;
      this.blocksToBlow = Lists.newArrayList(pToBlow);
      if (playerKnockbackVec != null) {
         this.knockbackX = (float)playerKnockbackVec.x;
         this.knockbackY = (float)playerKnockbackVec.y;
         this.knockbackZ = (float)playerKnockbackVec.z;
      }

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

      this.knockbackX = buffer.readFloat();
      this.knockbackY = buffer.readFloat();
      this.knockbackZ = buffer.readFloat();
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

      pBuffer.writeFloat(packet.knockbackX);
      pBuffer.writeFloat(packet.knockbackY);
      pBuffer.writeFloat(packet.knockbackZ);
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

   public float getKnockbackX() {
      return this.knockbackX;
   }

   public float getKnockbackY() {
      return this.knockbackY;
   }

   public float getKnockbackZ() {
      return this.knockbackZ;
   }

   public float getPower() {
      return this.power;
   }

   public List<BlockPos> getBlocksToBlow() {
      return this.blocksToBlow;
   }
}