package me.infamous.accessmod.common.entity.gobblefin;

import me.infamous.accessmod.common.network.AccessModNetwork;
import me.infamous.accessmod.common.network.ClientboundVortexPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.ExplosionContext;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;

public class VortexHelper {

    public static Vortex vortex(World level, @Nullable Entity pEntity, Vector3d position, float pExplosionRadius, Vortex.Mode pMode) {
        return vortex(level, pEntity, null, null, position, pExplosionRadius, pMode);
    }

    public static Vortex vortex(World level, @Nullable Entity pExploder, @Nullable DamageSource pDamageSource, @Nullable ExplosionContext pContext, Vector3d position, float pSize, Vortex.Mode pMode) {
        Vortex vortex = new Vortex(level, pExploder, pDamageSource, pContext, position, pSize, pMode);
        //if (net.minecraftforge.event.ForgeEventFactory.onExplosionStart(this, vortex)) return vortex;
        vortex.explode();
        vortex.finalizeVortex(level.isClientSide);

        if(!level.isClientSide){
            if (pMode == Vortex.Mode.NONE) {
                vortex.clearToBlow();
            }

            for(ServerPlayerEntity player : ((ServerWorld)level).players()) {
                if (player.distanceToSqr(position) < 4096.0D) {
                    AccessModNetwork.SYNC_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                            new ClientboundVortexPacket(position, pSize, vortex.getBlocksToBlow(), vortex.getPlayerHitVec(player).orElse(null)));
                }
            }
        }

        return vortex;
    }

    public static void createClientVortex(ClientboundVortexPacket packet, World level, @Nullable PlayerEntity player) {
       Vortex vortex = new Vortex(level, null, packet.getPosition(), packet.getPower(), packet.getBlocksToBlow());
       vortex.finalizeVortex(true);
       if(player != null){
          player.setDeltaMovement(player.getDeltaMovement().add(packet.getKnockbackX(), packet.getKnockbackY(), packet.getKnockbackZ()));
       }
    }
}
