package me.infamous.accessmod.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.MovementInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {

    @Shadow public MovementInput input;

    public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "rideTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/player/AbstractClientPlayerEntity;rideTick()V", shift = At.Shift.AFTER))
    private void addForgeFix(CallbackInfo ci){
        if (this.wantsToStopRiding() && this.isPassenger()) this.input.shiftKeyDown = false;
    }
}
