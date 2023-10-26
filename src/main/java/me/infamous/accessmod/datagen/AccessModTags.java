package me.infamous.accessmod.datagen;

import me.infamous.accessmod.AccessMod;
import net.minecraft.block.Block;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.Tags;

public class AccessModTags {

    public static Tags.IOptionalNamedTag<Block> DUNES_SPAWN_ON = blockTag("dunes_spawn_on");
    public static Tags.IOptionalNamedTag<Block> DUNE_WRATH_SINK_IN = blockTag("dune_wrath_sink_in");
    public static Tags.IOptionalNamedTag<Block> VORTEX_IMMUNE = blockTag("vortex_immune");

    private static Tags.IOptionalNamedTag<Block> blockTag(String name)
    {
        return BlockTags.createOptional(new ResourceLocation(AccessMod.MODID, name));
    }
}
