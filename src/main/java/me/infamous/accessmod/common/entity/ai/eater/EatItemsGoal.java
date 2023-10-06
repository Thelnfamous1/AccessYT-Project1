package me.infamous.accessmod.common.entity.ai.eater;

import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.passive.DolphinEntity;
import net.minecraft.util.SoundEvents;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public class EatItemsGoal<T extends MobEntity & Eater & EatTargeting> extends Goal {
    private final T mob;

    private final int searchRate;
    private long searchCooldown;
    private final int searchDistance;
    private final int eatRate;
    private int nextEatTime;
    private final float speedModifier;
    private final int suckUpDuration;
    private final int swallowDuration;
    private int actionTicks;
    private List<ItemEntity> nearbyItems = Collections.emptyList();
    private Phase phase;

    public EatItemsGoal(T mob, int searchRate, int searchDistance, int eatRate, float speedModifier, int suckUpDuration, int swallowDuration) {
        this.mob = mob;
        this.searchRate = searchRate;
        this.searchCooldown = mob.getRandom().nextInt(searchRate);
        this.searchDistance = searchDistance;
        this.eatRate = eatRate;
        this.speedModifier = speedModifier;
        this.suckUpDuration = suckUpDuration;
        this.swallowDuration = swallowDuration;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if(this.mob.isThrowingUp()){
            return false;
        } else if (this.nextEatTime > this.mob.tickCount) {
            return false;
        } else {
            this.nearbyItems = this.findAndCacheNearbyItems(false);
            return !this.nearbyItems.isEmpty() && this.mob.getLastHurtByMob() == null;
        }
    }

    private List<ItemEntity> findAndCacheNearbyItems(boolean ignoreCooldown) {
        if (--this.searchCooldown <= 0L || ignoreCooldown) {
            this.searchCooldown = this.searchRate;
            return this.mob.level.getEntitiesOfClass(ItemEntity.class, this.mob.getBoundingBox().inflate(this.searchDistance, this.searchDistance, this.searchDistance), DolphinEntity.ALLOWED_ITEMS);
        }
        return this.nearbyItems;
    }

    @Override
    public void start() {
        this.phase = Phase.SEARCH;
        this.mob.setMouthOpen();
        this.nearbyItems = this.findAndCacheNearbyItems(false);
        if (!this.nearbyItems.isEmpty()) {
            this.mob.getNavigation().moveTo(this.nearbyItems.get(0), this.speedModifier);
            this.mob.playSound(SoundEvents.DOLPHIN_PLAY, 1.0F, 1.0F);
        }
        this.nextEatTime = 0;
    }

    @Override
    public void tick() {
        this.nearbyItems = this.findAndCacheNearbyItems(false);

        if (!this.nearbyItems.isEmpty()) {
            ItemEntity target = this.nearbyItems.get(0);
            switch (this.phase){
                case SEARCH:
                    if(this.canPickUpItem(target)){
                        this.mob.getNavigation().stop();
                        this.mob.getLookControl().setLookAt(target, this.mob.getMaxHeadYRot(), this.mob.getMaxHeadXRot());
                        this.phase = Phase.SUCK_UP;
                        this.mob.setSuckingUp();
                        this.mob.setEatTarget(target);
                        this.actionTicks = this.suckUpDuration;
                    } else{
                        this.pursueItem(target);
                    }
                    break;
                case SUCK_UP:
                    this.actionTicks--;
                    if(this.actionTicks == 0){
                        if(this.canPickUpItem(target)){
                            this.mob.getNavigation().stop();
                            this.mob.getLookControl().setLookAt(target, this.mob.getMaxHeadYRot(), this.mob.getMaxHeadXRot());
                            this.phase = Phase.SWALLOW;
                            this.mob.setSwallowing();
                            this.actionTicks = this.swallowDuration;
                        } else{
                            this.pursueItem(target);
                        }
                    }
                    break;
                case SWALLOW:
                    this.actionTicks--;
                    if(this.actionTicks == 0){
                        if(this.canPickUpItem(target)){
                            this.mob.eat(target);
                            this.phase = Phase.SEARCH;
                            this.mob.setMouthOpen();
                            this.mob.setEatTarget(null);
                            this.actionTicks = 0;
                        } else{
                            this.pursueItem(target);
                        }
                    }
                    break;
            }
        }
    }

    private boolean canPickUpItem(ItemEntity item) {
        return item.isAlive()
                && !item.getItem().isEmpty()
                && !item.hasPickUpDelay()
                && this.mob.wantsToPickUp(item.getItem())
                && this.mob.wantsToEat(item)
                && this.mob.closerThan(item, this.mob.getBbWidth());
    }

    private void pursueItem(ItemEntity item) {
        this.phase = Phase.SEARCH;
        this.mob.setMouthOpen();
        this.mob.setEatTarget(null);
        this.actionTicks = 0;
        this.mob.getNavigation().moveTo(item, this.speedModifier);
    }

    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse();
    }

    @Override
    public void stop() {
        if(!this.mob.isThrowingUp()){
            this.mob.setMouthClosed();
        }
        this.nextEatTime = this.mob.tickCount + this.mob.getRandom().nextInt(this.eatRate);
    }

    enum Phase{
        SEARCH,
        SUCK_UP,
        SWALLOW
    }

}