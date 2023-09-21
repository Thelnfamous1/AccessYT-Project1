package me.infamous.accessmod.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import me.infamous.accessmod.client.overlay.SoulsOverlay;
import me.infamous.accessmod.duck.Summonable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.OutlineLayerBuffer;
import net.minecraft.client.renderer.RenderTypeBuffers;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Shadow @Final private EntityRendererManager entityRenderDispatcher;

    @Shadow @Final private RenderTypeBuffers renderBuffers;

    @Inject(method = "renderEntity", at = @At(value = "HEAD", target = "Lnet/minecraft/client/renderer/entity/EntityRendererManager;render(Lnet/minecraft/entity/Entity;DDDFFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V"),
    cancellable = true)
    private void modifyBuffer(Entity pEntity, double pCamX, double pCamY, double pCamZ, float pPartialTicks, MatrixStack pMatrixStack, IRenderTypeBuffer pBuffer, CallbackInfo ci){
        if(pEntity instanceof MobEntity && Summonable.cast((MobEntity) pEntity).isSummonedBy(Minecraft.getInstance().player)){
            ci.cancel();
            // recreate the same values as in the original call
            double d0 = MathHelper.lerp(pPartialTicks, pEntity.xOld, pEntity.getX());
            double d1 = MathHelper.lerp(pPartialTicks, pEntity.yOld, pEntity.getY());
            double d2 = MathHelper.lerp(pPartialTicks, pEntity.zOld, pEntity.getZ());
            float f = MathHelper.lerp(pPartialTicks, pEntity.yRotO, pEntity.yRot);

            // replace the passed in buffer with an overlay one
            OutlineLayerBuffer outlinelayerbuffer = this.renderBuffers.outlineBufferSource();
            int k2 = SoulsOverlay.TOTAL_SOULS_COLOR >> 16 & 255;
            int l2 = SoulsOverlay.TOTAL_SOULS_COLOR >> 8 & 255;
            int i3 = SoulsOverlay.TOTAL_SOULS_COLOR & 255;
            outlinelayerbuffer.setColor(k2, l2, i3, 255);

            this.entityRenderDispatcher.render(pEntity, d0 - pCamX, d1 - pCamY, d2 - pCamZ, f, pPartialTicks, pMatrixStack, outlinelayerbuffer, this.entityRenderDispatcher.getPackedLightCoords(pEntity, pPartialTicks));
        }
    }
}
