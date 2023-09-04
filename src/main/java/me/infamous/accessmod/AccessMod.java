package me.infamous.accessmod;

import me.infamous.accessmod.common.registry.AccessModDataSerializers;
import me.infamous.accessmod.common.registry.AccessModEffects;
import me.infamous.accessmod.common.registry.AccessModEntityTypes;
import me.infamous.accessmod.common.registry.AccessModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ovh.corail.flyingthings.config.ConfigFlyingThings;
import ovh.corail.flyingthings.config.FlyingThingsModConfig;

import java.nio.file.Path;

@Mod(AccessMod.MODID)
public class AccessMod {
    public static final String MODID = "accessmod";
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MAGIC_CARPET_CONFIGDIR = "magic_carpet";

    public AccessMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        AccessModEntityTypes.register(modEventBus);
        AccessModItems.register(modEventBus);
        AccessModDataSerializers.register(modEventBus);
        AccessModEffects.register(modEventBus);

        ModLoadingContext context = ModLoadingContext.get();
        Path modDirectory = FileUtils.getOrCreateDirectory(FMLPaths.CONFIGDIR.get().resolve(MODID), MODID);
        FileUtils.getOrCreateDirectory(modDirectory.resolve(MAGIC_CARPET_CONFIGDIR), MAGIC_CARPET_CONFIGDIR);
        context.registerConfig(ModConfig.Type.CLIENT, ConfigFlyingThings.CLIENT_SPEC, String.format("%s/%s/%s.toml", MODID, MAGIC_CARPET_CONFIGDIR, ModConfig.Type.CLIENT.extension()));
        context.registerConfig(ModConfig.Type.COMMON, ConfigFlyingThings.GENERAL_SPEC, String.format("%s/%s/%s.toml", MODID, MAGIC_CARPET_CONFIGDIR, ModConfig.Type.COMMON.extension()));
        registerSharedConfig(context);
    }

    private void registerSharedConfig(ModLoadingContext context) {
        context.getActiveContainer().addConfig(new FlyingThingsModConfig(ConfigFlyingThings.SHARED_SPEC, context.getActiveContainer()));
    }
}
