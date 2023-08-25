package me.infamous.accessmod.common.entity.dune;

import me.infamous.accessmod.common.entity.ai.magic.AnimatableMagicGoal;
import me.infamous.accessmod.common.registry.AccessModEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;

public class DuneWrathGoal extends AnimatableMagicGoal<Dune, Dune.DuneMagicType> {
    public DuneWrathGoal(Dune mage) {
        super(mage, Dune.DuneMagicType.WRATH);
    }

    @Override
    public boolean canUse() {
        return super.canUse() && this.mage.getTarget().hasEffect(AccessModEffects.DUNE_WRATH.get());
    }

    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse() && this.mage.getTarget().hasEffect(AccessModEffects.DUNE_WRATH.get());
    }

    @Override
    protected void useMagic() {
        LivingEntity target = this.mage.getTarget();
        target.sendMessage(new StringTextComponent("You are being dragged into the ground!"), Util.NIL_UUID);
    }
}
