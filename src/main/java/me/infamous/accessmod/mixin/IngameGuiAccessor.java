package me.infamous.accessmod.mixin;

import net.minecraft.client.gui.IngameGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Random;

@Mixin(IngameGui.class)
public interface IngameGuiAccessor {

    @Accessor("random")
    Random accessmod_getRandom();
}
