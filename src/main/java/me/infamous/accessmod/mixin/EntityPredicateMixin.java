package me.infamous.accessmod.mixin;

import me.infamous.accessmod.duck.Summonable;
import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityPredicate.class)
public class EntityPredicateMixin {

    @Shadow private boolean allowNonAttackable;

    @Shadow private boolean allowSameTeam;

    @Inject(method = "test", at = @At("RETURN"), cancellable = true)
    private void handleTest(LivingEntity pAttacker, LivingEntity pTarget, CallbackInfoReturnable<Boolean> cir){
        if(cir.getReturnValue() && pAttacker instanceof MobEntity){
            MobEntity mobAttacker = (MobEntity) pAttacker;
            if (!this.allowNonAttackable) {
                if (!Summonable.cast(mobAttacker).canSummonableAttack(pTarget)) {
                    cir.setReturnValue(false);
                }
            }

            if (!this.allowSameTeam && Summonable.cast(mobAttacker).isSummonableAlliedTo(pTarget).orElse(false)) {
                cir.setReturnValue(false);
            }
        }
    }
}
