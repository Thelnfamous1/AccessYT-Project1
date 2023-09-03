package me.infamous.accessmod.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.infamous.accessmod.common.AccessModUtil;
import me.infamous.accessmod.duck.DuneSubmergeable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements DuneSubmergeable {

    private LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @ModifyExpressionValue(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isEyeInFluid(Lnet/minecraft/tags/ITag;)Z"))
    private boolean baseTick$checkDrown(boolean original) {
        return original || this.isSubmergedByDune();
    }

    @ModifyExpressionValue(method = "baseTick", at = @At(value = "FIELD", target = "Lnet/minecraft/util/DamageSource;DROWN:Lnet/minecraft/util/DamageSource;"))
    private DamageSource baseTick$modifyDrownDamageSource(DamageSource original) {
        return this.isSubmergedByDune() ? AccessModUtil.SUFFOCATION : original;
    }
}
