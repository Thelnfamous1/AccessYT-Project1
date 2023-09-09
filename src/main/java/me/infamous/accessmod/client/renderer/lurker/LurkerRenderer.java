package me.infamous.accessmod.client.renderer.lurker;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import me.infamous.accessmod.common.entity.lurker.Lurker;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

public class LurkerRenderer extends GeoEntityRenderer<Lurker> {
    public LurkerRenderer(EntityRendererManager renderManager) {
        super(renderManager, new LurkerModel());
    }

    @Override
    public RenderType getRenderType(Lurker animatable, float partialTicks, MatrixStack stack,
                                    IRenderTypeBuffer renderTypeBuffer, IVertexBuilder vertexBuilder, int packedLightIn,
                                    ResourceLocation textureLocation) {
        return RenderType.entityTranslucent(this.getTextureLocation(animatable));
    }
}
