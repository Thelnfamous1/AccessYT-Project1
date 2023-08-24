package me.infamous.accessmod.datagen;

import me.infamous.accessmod.AccessMod;
import me.infamous.accessmod.common.registry.AccessModItems;
import net.minecraft.data.DataGenerator;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.fml.RegistryObject;

public class AccessModItemModelProvider extends ItemModelProvider {
    public AccessModItemModelProvider(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator, AccessMod.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        this.spawnEgg(AccessModItems.DUNE_SPAWN_EGG);
        /*
        ResourceLocation id = AccessYTItems.MAGIC_CARPET.getId();
        this.singleTexture(id.getPath(), mcLoc("item/generated"), new ResourceLocation(id.getNamespace(), "item/" + id.getPath()));
         */

    }

    private void spawnEgg(RegistryObject<Item> spawnEgg) {
        ResourceLocation id = spawnEgg.getId();
        this.withExistingParent(id.getPath(), mcLoc("item/template_spawn_egg"));
    }
}