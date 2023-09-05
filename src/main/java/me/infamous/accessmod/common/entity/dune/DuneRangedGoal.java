package me.infamous.accessmod.common.entity.dune;

import me.infamous.accessmod.common.entity.ai.magic.AnimatableMagicGoal;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.common.util.Constants;

public class DuneRangedGoal extends AnimatableMagicGoal<Dune, DuneMagicType> {

    public DuneRangedGoal(Dune mage) {
        super(mage, DuneMagicType.RANGED);
    }

    @Override
    protected void useMagic() {
        LivingEntity target = this.mage.getTarget();
        Vector3d viewVector = this.mage.getViewVector(1.0F);
        double width = this.mage.getBbWidth();
        double xDist = target.getX() - (this.mage.getX() + viewVector.x * width);
        double yDist = target.getY(0.5D) - this.mage.getEyeY();
        double zDist = target.getZ() - (this.mage.getZ() + viewVector.z * width);
        if (!this.mage.isSilent()) {
            this.mage.level.levelEvent(null, Constants.WorldEvents.WITHER_SHOOT_SOUND, this.mage.blockPosition(), 0);
        }

        WrathfulDust wrathfulDust = new WrathfulDust(this.mage.level, this.mage, xDist, yDist, zDist);
        wrathfulDust.setPos(this.mage.getX() + viewVector.x * width, this.mage.getEyeY(), wrathfulDust.getZ() + viewVector.z * width);
        this.mage.level.addFreshEntity(wrathfulDust);
    }

}
