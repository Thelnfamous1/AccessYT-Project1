package me.infamous.accessmod.common.entity.ai.magic;

import com.google.common.collect.Maps;
import net.minecraft.entity.MobEntity;
import net.minecraft.util.math.MathHelper;

import java.util.Map;

public class MagicCooldownTracker<T extends MobEntity & AnimatableMagic<M>, M extends AnimatableMagic.MagicType> {
    private final Map<M, MagicCooldownTracker.Cooldown> cooldowns = Maps.newHashMap();
    private final T mage;
    private int tickCount;

    public MagicCooldownTracker(T mage){
        this.mage = mage;
    }

    public boolean isOnCooldown(M magicType) {
        return this.getCooldownPercent(magicType, 0.0F) > 0.0F;
    }

    public float getCooldownPercent(M magicType, float pPartialTicks) {
        MagicCooldownTracker.Cooldown cooldown = this.cooldowns.get(magicType);
        if (cooldown != null) {
            float duration = (float)(cooldown.endTime - cooldown.startTime);
            float current = (float)cooldown.endTime - ((float)this.tickCount + pPartialTicks);
            return MathHelper.clamp(current / duration, 0.0F, 1.0F);
        } else {
            return 0.0F;
        }
    }

    public void tick() {
        ++this.tickCount;
        if (!this.cooldowns.isEmpty()) {
            this.cooldowns.entrySet().removeIf(entry -> (entry.getValue()).endTime <= this.tickCount);
        }
    }

    public void addCooldown(M magicType) {
        this.cooldowns.put(magicType, new MagicCooldownTracker.Cooldown(
                this.tickCount,
                this.tickCount + magicType.getCooldownTime().randomValue(this.mage.getRandom())));
    }

    public void removeCooldown(M magicType) {
        this.cooldowns.remove(magicType);
    }

    static class Cooldown {
        private final int startTime;
        private final int endTime;

        private Cooldown(int pStartTime, int pEndTime) {
            this.startTime = pStartTime;
            this.endTime = pEndTime;
        }
    }
}
