package me.infamous.accessmod.mixin;

import me.infamous.accessmod.client.audio.VortexSound;
import me.infamous.accessmod.common.entity.ai.eater.VortexEater;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.server.SEntityStatusPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetHandler.class)
public class ClientPlayNetHandlerMixin {

    @Shadow private Minecraft minecraft;

    @Shadow private ClientWorld level;

    @Inject(method = "handleEntityEvent", at = @At("HEAD"), cancellable = true)
    private void accessmod_handleEntityEvent(SEntityStatusPacket pPacket, CallbackInfo ci){
        PacketThreadUtil.ensureRunningOnSameThread(pPacket, (ClientPlayNetHandler)(Object)this, this.minecraft);
        Entity entity = pPacket.getEntity(this.level);
        if (entity instanceof VortexEater) {
            if (pPacket.getEventId() == VortexEater.VORTEX_EVENT_ID) {
                ci.cancel();
                VortexEater vortexEater = (VortexEater) entity;
                this.minecraft.getSoundManager().play(VortexSound.hackyCreate(vortexEater));
            }
        }
    }
}
