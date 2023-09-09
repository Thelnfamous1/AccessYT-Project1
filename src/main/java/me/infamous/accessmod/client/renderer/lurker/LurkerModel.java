package me.infamous.accessmod.client.renderer.lurker;

import me.infamous.accessmod.AccessMod;
import me.infamous.accessmod.common.entity.lurker.Lurker;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.model.provider.data.EntityModelData;

public class LurkerModel extends AnimatedGeoModel<Lurker> {

    public static final ResourceLocation ANIMATION_LOCATION = new ResourceLocation(AccessMod.MODID, "animations/lurker.animation.json");
    public static final ResourceLocation MODEL_LOCATION = new ResourceLocation(AccessMod.MODID, "geo/lurker.geo.json");
    public static final ResourceLocation TEXTURE_LOCATION = new ResourceLocation(AccessMod.MODID, "textures/entity/lurker/lurker.png");

    @Override
    public ResourceLocation getAnimationFileLocation(Lurker animatable) {
        return ANIMATION_LOCATION;
    }

    @Override
    public ResourceLocation getModelLocation(Lurker object) {
        return MODEL_LOCATION;
    }

    @Override
    public ResourceLocation getTextureLocation(Lurker object) {
        return TEXTURE_LOCATION;
    }

    @Override
    public void setLivingAnimations(Lurker entity, Integer uniqueID, AnimationEvent event) {
        super.setLivingAnimations(entity, uniqueID, event);
        IBone head = this.getAnimationProcessor().getBone("head");

        EntityModelData extraData = (EntityModelData) event.getExtraDataOfType(EntityModelData.class).get(0);
        if (extraData.headPitch != 0 || extraData.netHeadYaw != 0) {
            head.setRotationX(head.getRotationX() + (extraData.headPitch * ((float) Math.PI / 180F)));
            head.setRotationY(head.getRotationY() + (extraData.netHeadYaw * ((float) Math.PI / 180F)));
        }
    }
}
