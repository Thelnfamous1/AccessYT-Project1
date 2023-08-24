package me.infamous.accessmod.client.renderer;

import me.infamous.accessmod.AccessMod;
import me.infamous.accessmod.common.entity.Dune;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.model.provider.data.EntityModelData;

public class DuneModel extends AnimatedGeoModel<Dune> {

    public static final ResourceLocation ANIMATION_LOCATION = new ResourceLocation(AccessMod.MODID, "animations/dune.animation.json");
    public static final ResourceLocation MODEL_LOCATION = new ResourceLocation(AccessMod.MODID, "geo/dune.geo.json");
    public static final ResourceLocation TEXTURE_LOCATION = new ResourceLocation(AccessMod.MODID, "textures/entity/dune/dune.png");

    @Override
    public ResourceLocation getAnimationFileLocation(Dune animatable) {
        return ANIMATION_LOCATION;
    }

    @Override
    public ResourceLocation getModelLocation(Dune object) {
        return MODEL_LOCATION;
    }

    @Override
    public ResourceLocation getTextureLocation(Dune object) {
        return TEXTURE_LOCATION;
    }

    @Override
    public void setLivingAnimations(Dune entity, Integer uniqueID, AnimationEvent event) {
        super.setLivingAnimations(entity, uniqueID, event);
        IBone head = this.getAnimationProcessor().getBone("Head");

        EntityModelData extraData = (EntityModelData) event.getExtraDataOfType(EntityModelData.class).get(0);
        if (extraData.headPitch != 0 || extraData.netHeadYaw != 0) {
            head.setRotationX(head.getRotationX() + (extraData.headPitch * ((float) Math.PI / 180F)));
            head.setRotationY(head.getRotationY() + (extraData.netHeadYaw * ((float) Math.PI / 180F)));
        }
    }
}
