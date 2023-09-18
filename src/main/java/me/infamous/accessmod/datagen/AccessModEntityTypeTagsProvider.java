package me.infamous.accessmod.datagen;

import me.infamous.accessmod.AccessMod;
import me.infamous.accessmod.common.AccessModUtil;
import me.infamous.accessmod.common.registry.AccessModEntityTypes;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.EntityTypeTagsProvider;
import net.minecraft.entity.EntityType;
import net.minecraft.tags.EntityTypeTags;
import net.minecraftforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;

public class AccessModEntityTypeTagsProvider extends EntityTypeTagsProvider {
    public AccessModEntityTypeTagsProvider(DataGenerator pGenerator, @Nullable ExistingFileHelper existingFileHelper) {
        super(pGenerator, AccessMod.MODID, existingFileHelper);
    }

    @Override
    protected void addTags() {
        this.tag(EntityTypeTags.IMPACT_PROJECTILES).add(AccessModEntityTypes.WRATHFUL_DUST.get());
        this.tag(AccessModUtil.LURKER_DISGUISES_AS).add(
                EntityType.PIG, EntityType.COW, EntityType.CHICKEN,
                EntityType.BEE, EntityType.SHEEP, EntityType.PARROT);
    }
}
