package me.infamous.accessmod.common.entity.dune;

import me.infamous.accessmod.common.AccessModUtil;
import me.infamous.accessmod.common.entity.ai.magic.AnimatableMagicGoal;
import net.minecraft.util.SoundEvents;

public class DuneRangedGoal extends AnimatableMagicGoal<Dune, DuneMagicType> {

    public DuneRangedGoal(Dune mage) {
        super(mage, DuneMagicType.RANGED);
    }

    @Override
    protected void useMagic() {
        AccessModUtil.shootLikeSnowball(
                this.mage,
                this.mage.getTarget(),
                new WrathfulDust(this.mage.level, this.mage),
                SoundEvents.WITHER_SHOOT);
    }

}
