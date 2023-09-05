package me.infamous.accessmod.common.registry;

import me.infamous.accessmod.AccessMod;
import me.infamous.accessmod.common.loot.SetModelType;
import net.minecraft.loot.ILootSerializer;
import net.minecraft.loot.LootFunctionType;
import net.minecraft.loot.functions.ILootFunction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

public class AccessModLootFunctions {
    private static boolean REGISTERED;

    public static LootFunctionType SET_MODEL_TYPE;

    private static LootFunctionType register(String pId, ILootSerializer<? extends ILootFunction> pSerializer) {
        return Registry.register(Registry.LOOT_FUNCTION_TYPE, new ResourceLocation(AccessMod.MODID, pId), new LootFunctionType(pSerializer));
    }

    public static void register() {
        if(!REGISTERED){
            REGISTERED = true;
            SET_MODEL_TYPE = register("set_model_type", new SetModelType.Serializer());
        } else{
            AccessMod.LOGGER.error("Tried to re-register loot functions!");
        }
    }
}
