package me.infamous.accessmod.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Shadow private float fov;
    @Unique private float uncappedFov;

    @Inject(method = "tickFov", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;fov:F", opcode = Opcodes.PUTFIELD, ordinal = 1))
    private void preFOVCap(CallbackInfo ci){
        this.uncappedFov = this.fov;
    }

    @Inject(method = "tickFov", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;fov:F", opcode = Opcodes.PUTFIELD, ordinal = 1, shift = At.Shift.AFTER))
    private void postFOVCap(CallbackInfo ci){
        this.fov = this.uncappedFov;
    }
}
