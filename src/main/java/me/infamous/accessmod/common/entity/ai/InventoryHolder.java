package me.infamous.accessmod.common.entity.ai;

import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.Constants;

public interface InventoryHolder {

    String INVENTORY_TAG = "Inventory";

    default void writeInventory(CompoundNBT tag){
        tag.put(INVENTORY_TAG, this.getInventory().createTag());
    }

    default void readInventory(CompoundNBT tag){
        this.getInventory().fromTag(tag.getList(INVENTORY_TAG, Constants.NBT.TAG_COMPOUND));
    }

    Inventory getInventory();
}
