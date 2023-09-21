package me.infamous.accessmod.mixin;

import me.infamous.accessmod.duck.Summonable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingRenderer.class)
public abstract class LivingRendererMixin<T extends LivingEntity> extends EntityRenderer<T> {

    protected LivingRendererMixin(EntityRendererManager manager) {
        super(manager);
    }

    @ModifyVariable(method = "render(Lnet/minecraft/entity/LivingEntity;FFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V",
            at = @At("STORE"),
            ordinal = 0)
    private RenderType modifyRenderType(RenderType original, T pEntity){
        if(pEntity instanceof MobEntity && Summonable.cast((MobEntity) pEntity).isSummonedBy(Minecraft.getInstance().player)){
            return RenderType.itemEntityTranslucentCull(this.getTextureLocation(pEntity));
        }
        return original;
    }

    @ModifyConstant(method = "render(Lnet/minecraft/entity/LivingEntity;FFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V",
    constant = @Constant(floatValue = 1.0F, ordinal = 7))
    private float modifyRenderToBufferAlpha(float original, T pEntity){
        if(pEntity instanceof MobEntity && Summonable.cast((MobEntity) pEntity).isSummonedBy(Minecraft.getInstance().player)){
            return 0.5F;
        }
        return original;
    }
}
