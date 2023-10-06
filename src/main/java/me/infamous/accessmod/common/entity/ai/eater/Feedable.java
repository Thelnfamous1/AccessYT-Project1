package me.infamous.accessmod.common.entity.ai.eater;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

public interface Feedable {

    String GOT_FOOD_TAG = "GotFood";

    default void writeFedData(CompoundNBT pCompound) {
        pCompound.putBoolean(GOT_FOOD_TAG, this.gotFood());
    }

    default void readFedData(CompoundNBT pCompound) {
        this.setGotFood(pCompound.getBoolean(GOT_FOOD_TAG));
    }

    boolean isFood(ItemStack itemstack);

    boolean gotFood();

    void setGotFood(boolean gotFood);
}
