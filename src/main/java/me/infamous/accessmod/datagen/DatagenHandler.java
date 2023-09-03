package me.infamous.accessmod.datagen;

import me.infamous.accessmod.AccessMod;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = AccessMod.MODID)
public class DatagenHandler {

    @SubscribeEvent
    static void gatherDataEvent(GatherDataEvent event){
        boolean includeClient = event.includeClient();
        boolean includeServer = event.includeServer();
        DataGenerator generator = event.getGenerator();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        AccessModLanguageProvider languageProvider = new AccessModLanguageProvider(generator);
        AccessModItemModelProvider itemModelProvider = new AccessModItemModelProvider(generator, existingFileHelper);
        AccessModBlockTagsProvider blockTagsProvider = new AccessModBlockTagsProvider(generator, existingFileHelper);
        AccessModEntityTypeTagsProvider entityTypeTagsProvider = new AccessModEntityTypeTagsProvider(generator, existingFileHelper);
        if(includeClient){
            generator.addProvider(languageProvider);
            generator.addProvider(itemModelProvider);
        }
        if(includeServer){
            generator.addProvider(blockTagsProvider);
            generator.addProvider(entityTypeTagsProvider);
        }
    }
}