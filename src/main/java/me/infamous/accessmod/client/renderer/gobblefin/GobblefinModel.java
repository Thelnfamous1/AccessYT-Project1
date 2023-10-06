package me.infamous.accessmod.client.renderer.gobblefin;

import me.infamous.accessmod.AccessMod;
import me.infamous.accessmod.common.entity.gobblefin.Gobblefin;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.model.provider.data.EntityModelData;

public class GobblefinModel extends AnimatedGeoModel<Gobblefin> {

    public static final ResourceLocation ANIMATION_LOCATION = new ResourceLocation(AccessMod.MODID, "animations/gobblefin.animation.json");
    public static final ResourceLocation MODEL_LOCATION = new ResourceLocation(AccessMod.MODID, "geo/gobblefin.geo.json");
    public static final ResourceLocation TEXTURE_LOCATION = new ResourceLocation(AccessMod.MODID, "textures/entity/gobblefin/gobblefin.png");
    public static final ResourceLocation MOUNTED_TEXTURE_LOCATION = new ResourceLocation(AccessMod.MODID, "textures/entity/gobblefin/gobblefin_mounted.png");

    @Override
    public ResourceLocation getAnimationFileLocation(Gobblefin animatable) {
        return ANIMATION_LOCATION;
    }

    @Override
    public ResourceLocation getModelLocation(Gobblefin object) {
        return MODEL_LOCATION;
    }

    @Override
    public ResourceLocation getTextureLocation(Gobblefin object) {
        if(object.getControllingPassenger() == Minecraft.getInstance().player){
            return MOUNTED_TEXTURE_LOCATION;
        }
        return TEXTURE_LOCATION;
    }

    @Override
    public void setLivingAnimations(Gobblefin entity, Integer uniqueID, AnimationEvent event) {
        super.setLivingAnimations(entity, uniqueID, event);
        IBone head = this.getAnimationProcessor().getBone("head");

        EntityModelData extraData = (EntityModelData) event.getExtraDataOfType(EntityModelData.class).get(0);
        if (extraData.headPitch != 0 || extraData.netHeadYaw != 0) {
            head.setRotationX(head.getRotationX() + (extraData.headPitch * ((float) Math.PI / 180F)));
            head.setRotationY(head.getRotationY() + (extraData.netHeadYaw * ((float) Math.PI / 180F)));
        }
    }
}
