package me.infamous.accessmod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.EntityPosWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityPosWrapper.class)
public interface EntityPosWrapperAccessor {

    @Accessor("entity")
    Entity accessmod_getEntity();

    @Accessor("trackEyeHeight")
    boolean accessmod_getTrackEyeHeight();
}
