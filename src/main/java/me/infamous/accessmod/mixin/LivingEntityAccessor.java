package me.infamous.accessmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {

    @Invoker
    SoundEvent callGetHurtSound(DamageSource pDamageSource);

    @Invoker
    SoundEvent callGetDeathSound();
}
