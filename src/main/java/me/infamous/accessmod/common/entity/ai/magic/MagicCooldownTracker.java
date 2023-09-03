package me.infamous.accessmod.common.entity.ai.magic;

import com.google.common.collect.Maps;
import net.minecraft.util.math.MathHelper;

import java.util.Map;

public class MagicCooldownTracker {
    private final Map<AnimatableMagic.MagicType, MagicCooldownTracker.Cooldown> cooldowns = Maps.newHashMap();
    private int tickCount;

    public boolean isOnCooldown(AnimatableMagic.MagicType magicType) {
        return this.getCooldownPercent(magicType, 0.0F) > 0.0F;
    }

    public float getCooldownPercent(AnimatableMagic.MagicType magicType, float pPartialTicks) {
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

    public void addCooldown(AnimatableMagic.MagicType magicType) {
        this.cooldowns.put(magicType, new MagicCooldownTracker.Cooldown(this.tickCount, this.tickCount + magicType.getCooldownTime()));
    }

    public void removeCooldown(AnimatableMagic.MagicType magicType) {
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
