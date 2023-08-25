package me.infamous.accessmod.common.entity.ai.magic;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.SoundEvent;

public abstract class AnimatableMagicGoal<T extends MobEntity & AnimatableMagic<M>, M extends AnimatableMagic.MagicType> extends Goal {
    private int attackWarmupDelay;
    private int nextAttackTickCount;
    protected final T mage;
    private final M magicType;

    protected AnimatableMagicGoal(T mage, M magicType) {
        this.mage = mage;
        this.magicType = magicType;
    }

    @Override
    public boolean canUse() {
        LivingEntity livingentity = this.mage.getTarget();
        if (livingentity != null && livingentity.isAlive()) {
            if (this.mage.isMagicAnimationInProgress()) {
                return false;
            } else {
                return this.mage.tickCount >= this.nextAttackTickCount;
            }
        } else {
            return false;
        }
    }

    @Override
    public void start() {
        this.attackWarmupDelay = this.magicType.getWarmupTime();
        this.mage.startMagicAnimation(this.magicType);
        this.nextAttackTickCount = this.mage.tickCount + this.magicType.getCooldownTime();
        SoundEvent prepareSound = this.magicType.getPrepareSound();
        if (prepareSound != null) {
            this.mage.playSound(prepareSound, 1.0F, 1.0F);
        }

        this.mage.setCurrentMagicType(this.magicType);
    }

    @Override
    public void tick() {
        --this.attackWarmupDelay;
        if (this.attackWarmupDelay == 0) {
            this.useMagic();
            this.mage.playSound(this.mage.getUseMagicSound(), 1.0F, 1.0F);
        }
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity livingentity = this.mage.getTarget();
        return livingentity != null && livingentity.isAlive() && this.attackWarmupDelay > 0;
    }

    protected abstract void useMagic();
}