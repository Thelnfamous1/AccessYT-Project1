package me.infamous.accessmod.datagen;

import me.infamous.accessmod.AccessMod;
import me.infamous.accessmod.common.registry.AccessModEffects;
import me.infamous.accessmod.common.registry.AccessModEntityTypes;
import me.infamous.accessmod.common.registry.AccessModItems;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.LanguageProvider;

public class AccessModLanguageProvider extends LanguageProvider {
    public AccessModLanguageProvider(DataGenerator gen) {
        super(gen, AccessMod.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        this.addEntityType(AccessModEntityTypes.DUNE, "Dune");
        this.addEntityType(AccessModEntityTypes.WRATHFUL_DUST, "Wrathful Dust");
        this.add(AccessModItems.DUNE_SPAWN_EGG.get(), "Dune Spawn Egg");
        this.add(AccessModEffects.DUNE_WRATH.get(), "Dune's Wrath'");
        this.add("death.attack.suffocation", "%s suffocated");
        /*
        this.addEntityType(AccessYTEntityTypes.MAGIC_CARPET, "Magic Carpet");
        this.add(AccessYTItems.MAGIC_CARPET.get(), "Magic Carpet");
         */
        this.addEntityType(AccessModEntityTypes.LURKER, "Lurker");
        this.add(AccessModItems.LURKER_SPAWN_EGG.get(), "Lurker Spawn Egg");
        this.add(AccessModItems.SCYTHE.get(), "Scythe");

        this.addEntityType(AccessModEntityTypes.GOBBLEFIN, "Gobblefin");
        this.add(AccessModItems.GOBBLEFIN_SPAWN_EGG.get(), "Gobblefin Spawn Egg");
    }
}