package me.infamous.accessmod.common.entity.ai;

import me.infamous.accessmod.common.AccessModUtil;
import me.infamous.accessmod.common.entity.ai.eater.Eater;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.passive.DolphinEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvents;

import java.util.Collections;
import java.util.List;

public class EatItemsGoal<T extends MobEntity & Eater> extends Goal {
    private final T mob;

    private final int searchRate;
    private long searchCooldown;
    private final int searchDistance;
    private final int eatRate;
    private int nextEatTime;
    private final float speedModifier;
    private final int suckUpDuration;
    private final int swallowDuration;
    private final int throwUpDuration;
    private List<ItemEntity> nearbyItems = Collections.emptyList();

    public EatItemsGoal(T mob, int searchRate, int searchDistance, int eatRate, float speedModifier, int suckUpDuration, int swallowDuration, int throwUpDuration) {
        this.mob = mob;
        this.searchRate = searchRate;
        this.searchCooldown = mob.getRandom().nextInt(searchRate);
        this.searchDistance = searchDistance;
        this.eatRate = eatRate;
        this.speedModifier = speedModifier;
        this.suckUpDuration = suckUpDuration;
        this.swallowDuration = swallowDuration;
        this.throwUpDuration = throwUpDuration;
    }

    @Override
    public boolean canUse() {
        if (this.nextEatTime > this.mob.tickCount) {
            return false;
        } else {
            this.nearbyItems = this.findNearbyItems();
            return !this.nearbyItems.isEmpty() || !this.getHeldItem().isEmpty();
        }
    }

    private List<ItemEntity> findNearbyItems() {
        if (--this.searchCooldown <= 0L) {
            this.searchCooldown = this.searchRate;
            return this.mob.level.getEntitiesOfClass(ItemEntity.class, this.mob.getBoundingBox().inflate(this.searchDistance, this.searchDistance, this.searchDistance), DolphinEntity.ALLOWED_ITEMS);
        }
        return this.nearbyItems;
    }

    @Override
    public void start() {
        this.mob.setMouthOpen();
        this.nearbyItems = this.findNearbyItems();
        if (!this.nearbyItems.isEmpty()) {
            this.mob.getNavigation().moveTo(this.nearbyItems.get(0), this.speedModifier);
            this.mob.playSound(SoundEvents.DOLPHIN_PLAY, 1.0F, 1.0F);
        }
        this.nextEatTime = 0;
    }

    @Override
    public void tick() {
        this.nearbyItems = this.findNearbyItems();
        if (this.canDropHeldItems()) {
            this.dropHeldItems();
        } else if (!this.nearbyItems.isEmpty()) {
            this.mob.getNavigation().moveTo(this.nearbyItems.get(0), this.speedModifier);
        }
    }

    protected boolean canDropHeldItems() {
        ItemStack heldItem = this.getHeldItem();
        return !heldItem.isEmpty();
    }

    protected ItemStack getHeldItem() {
        return this.mob.getItemBySlot(EquipmentSlotType.MAINHAND);
    }

    protected void dropHeldItems(){
        AccessModUtil.dropItems(this.mob, this.getHeldItem());
        this.mob.setItemSlot(EquipmentSlotType.MAINHAND, ItemStack.EMPTY);
    }

    @Override
    public void stop() {
        this.mob.setMouthClosed();
        if (this.canDropHeldItems()) {
            this.dropHeldItems();
            this.nextEatTime = this.mob.tickCount + this.mob.getRandom().nextInt(this.eatRate);
        }
    }

}