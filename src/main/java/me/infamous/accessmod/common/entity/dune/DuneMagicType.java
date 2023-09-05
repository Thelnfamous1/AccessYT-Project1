package me.infamous.accessmod.common.entity.dune;

import me.infamous.accessmod.common.entity.ai.magic.AnimatableMagic;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;

import javax.annotation.Nullable;

public enum DuneMagicType implements AnimatableMagic.MagicType {
    NONE(0, 0, 0, 0, null),
    RANGED(1, 15, 21, 20, SoundEvents.ILLUSIONER_PREPARE_BLINDNESS),
    DRAG(2, 15, 28, 200, SoundEvents.EVOKER_PREPARE_ATTACK);

    private final int id;
    private final int warmupTime;
    private final int castingTime;
    private final int cooldownTime;
    @Nullable
    private final SoundEvent prepareSound;

    DuneMagicType(int id, int warmupTime, int castingTime, int cooldownTime, SoundEvent prepareSound) {
        this.id = id;
        this.warmupTime = warmupTime;
        this.castingTime = castingTime;
        this.cooldownTime = cooldownTime;
        this.prepareSound = prepareSound;
    }

    public static DuneMagicType byId(int pId) {
        for (DuneMagicType magicType : values()) {
            if (pId == magicType.id) {
                return magicType;
            }
        }

        return NONE;
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public int getWarmupTime() {
        return this.warmupTime;
    }

    @Override
    public int getCastingTime() {
        return this.castingTime;
    }

    @Override
    public int getCooldownTime() {
        return this.cooldownTime;
    }

    @Nullable
    @Override
    public SoundEvent getPrepareSound() {
        return this.prepareSound;
    }
}
