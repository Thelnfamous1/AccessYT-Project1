package me.infamous.accessmod.common.entity.ai.magic;

import net.minecraft.entity.MobEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.world.Difficulty;

public class EffectMagicGoal<T extends MobEntity & AnimatableMagic<M>, M extends AnimatableMagic.MagicType> extends AnimatableMagicGoal<T, M> {
    private int lastTargetId;
    private final Effect effect;
    private final int duration;
    private final int amplifier;

    public EffectMagicGoal(T magicUser, M magicType, Effect effect, int duration, int amplifier) {
        super(magicUser, magicType);
        this.effect = effect;
        this.duration = duration;
        this.amplifier = amplifier;
    }

    @Override
    public boolean canUse() {
        if (!super.canUse()) {
            return false;
        } else if (this.mage.getTarget() == null) {
            return false;
        } else if (this.mage.getTarget().getId() == this.lastTargetId) {
            return false;
        } else {
            return this.mage.level.getCurrentDifficultyAt(this.mage.blockPosition()).isHarderThan((float) Difficulty.EASY.ordinal());
        }
    }

    @Override
    public void start() {
        super.start();
        this.lastTargetId = this.mage.getTarget().getId();
    }

    @Override
    protected void useMagic() {
        this.mage.getTarget().addEffect(new EffectInstance(this.effect, this.duration, this.amplifier));
    }

}