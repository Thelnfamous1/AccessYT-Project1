package me.infamous.accessmod.mixin;

import net.minecraft.client.renderer.RenderTypeBuffers;
import net.minecraft.client.renderer.WorldRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Shadow @Final private RenderTypeBuffers renderBuffers;

    /*
    @Unique
    private static boolean renderingOutlines;


    @ModifyVariable(method = "renderEntity", at = @At(value = "LOAD", ordinal = 0), ordinal = 0, argsOnly = true)
    private IRenderTypeBuffer modifyBuffer(IRenderTypeBuffer original, Entity pEntity){
        if(pEntity instanceof MobEntity && Summonable.cast((MobEntity) pEntity).isSummonedBy(Minecraft.getInstance().player)){
            renderingOutlines = true;

            // replace the passed in buffer with an overlay one
            OutlineLayerBuffer outlinelayerbuffer = this.renderBuffers.outlineBufferSource();
            int k2 = SoulsOverlay.TOTAL_SOULS_COLOR >> 16 & 255;
            int l2 = SoulsOverlay.TOTAL_SOULS_COLOR >> 8 & 255;
            int i3 = SoulsOverlay.TOTAL_SOULS_COLOR & 255;
            outlinelayerbuffer.setColor(k2, l2, i3, 255);

            return outlinelayerbuffer;
        }
        return original;
    }

    @ModifyVariable(method = "renderLevel", at = @At(value = "LOAD", ordinal = 0), ordinal = 3)
    private boolean modifyRenderingOutlines(boolean original){
        boolean flag2 = original || renderingOutlines;
        renderingOutlines = false;
        return flag2;
    }
     */
}
