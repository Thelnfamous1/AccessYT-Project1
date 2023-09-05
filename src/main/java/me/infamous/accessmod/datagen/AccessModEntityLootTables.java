package me.infamous.accessmod.datagen;

import me.infamous.accessmod.common.loot.SetModelType;
import me.infamous.accessmod.common.registry.AccessModEntityTypes;
import me.infamous.accessmod.common.registry.AccessModItems;
import net.minecraft.data.loot.EntityLootTables;
import net.minecraft.entity.EntityType;
import net.minecraft.loot.ConstantRange;
import net.minecraft.loot.ItemLootEntry;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.conditions.KilledByPlayer;
import net.minecraft.loot.functions.SetCount;

public class AccessModEntityLootTables extends EntityLootTables {

    @Override
    protected void addTables() {
        this.add(AccessModEntityTypes.DUNE.get(), LootTable.lootTable().withPool(
                LootPool.lootPool()
                        .setRolls(ConstantRange.exactly(1))
                        .add(ItemLootEntry.lootTableItem(AccessModItems.MAGIC_CARPET.get())
                                .apply(SetCount.setCount(ConstantRange.exactly(1)))
                                .apply(SetModelType.setModelTypeOptions(new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8}))
                                .when(KilledByPlayer.killedByPlayer())
                        )
        ));
    }

    @Override
    protected Iterable<EntityType<?>> getKnownEntities() {
        return AccessModEntityTypes.getKnownEntities();
    }
}
