package me.infamous.accessmod.common.entity.ai.magic;

import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;

public class UsingMagicGoal<T extends MobEntity & AnimatableMagic<M>, M extends AnimatableMagic.MagicType> extends Goal {
    private final T mage;

    public UsingMagicGoal(T magicUserMob) {
        this.mage = magicUserMob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.mage.getMagicAnimationTick() > 0;
    }

    @Override
    public void start() {
        super.start();
        this.mage.getNavigation().stop();
    }

    @Override
    public void stop() {
        super.stop();
        this.mage.resetMagicType();
    }

    @Override
    public void tick() {
        if (this.mage.getTarget() != null) {
            this.mage.getLookControl().setLookAt(this.mage.getTarget(), (float) this.mage.getMaxHeadYRot(), (float) this.mage.getMaxHeadXRot());
        }
    }
}