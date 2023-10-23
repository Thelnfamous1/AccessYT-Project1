package me.infamous.accessmod.client.keybinding;

import me.infamous.accessmod.AccessMod;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.glfw.GLFW;

public class AccessModKeybindings {

    public static final String GOBBLEFIN_VORTEX_KEY = String.format("key.%s.gobblefin_vortex", AccessMod.MODID);
    public static final KeyBinding GOBBLEFIN_VORTEX = new KeyBinding(GOBBLEFIN_VORTEX_KEY, GLFW.GLFW_KEY_LEFT_CONTROL, "key.categories.gameplay");

    public static final String GOBBLEFIN_BOOST_KEY = String.format("key.%s.gobblefin_boost", AccessMod.MODID);
    public static final KeyBinding GOBBLEFIN_BOOST = new KeyBinding(GOBBLEFIN_BOOST_KEY, GLFW.GLFW_KEY_SPACE, "key.categories.gameplay");
}
