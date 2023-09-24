package me.infamous.accessmod.common.entity.gobblefin;

import me.infamous.accessmod.common.AccessModUtil;
import me.infamous.accessmod.common.entity.ai.InventoryHolder;
import me.infamous.accessmod.common.entity.ai.EatItemsGoal;
import me.infamous.accessmod.common.entity.ai.eater.Eater;
import me.infamous.accessmod.common.registry.AccessModDataSerializers;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.DolphinLookController;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.GuardianEntity;
import net.minecraft.entity.passive.WaterMobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.SwimmerPathNavigator;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.DamageSource;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraftforge.event.ForgeEventFactory;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

public class Gobblefin extends WaterMobEntity implements IAnimatable, Eater, InventoryHolder {
    private final AnimationFactory factory = GeckoLibUtil.createFactory(this);
    protected static final AnimationBuilder IDLE_ANIM = new AnimationBuilder().addAnimation("idle", true);
    protected static final AnimationBuilder SWIM_ANIM = new AnimationBuilder().addAnimation("swim", true);
    protected static final AnimationBuilder SWIM_SLOW_ANIM = new AnimationBuilder().addAnimation("swim_slow", true);
    protected static final AnimationBuilder SWIM_MOUTH_OPEN_ANIM = new AnimationBuilder().addAnimation("swim_mouthopen", true);
    protected static final AnimationBuilder WHIRLWIND_ANIM = new AnimationBuilder().addAnimation("whirlwind", false);
    private static final int SUCK_UP_DURATION = AccessModUtil.secondsToTicks(2.0833);
    protected static final AnimationBuilder EAT_ANIM = new AnimationBuilder().addAnimation("eat", false);
    private static final int SWALLOW_DURATION = AccessModUtil.secondsToTicks(0.5833);
    protected static final AnimationBuilder THROWUP_ANIM = new AnimationBuilder().addAnimation("throwup", false);
    private static final int THROW_UP_DURATION = AccessModUtil.secondsToTicks(0.75);

    private static final DataParameter<EatState> DATA_EAT_STATE = EntityDataManager.defineId(Gobblefin.class, AccessModDataSerializers.getSerializer(AccessModDataSerializers.EAT_STATE));
    private final Inventory inventory = new Inventory(8);
    public Gobblefin(EntityType<? extends Gobblefin> type, World world) {
        super(type, world);
        this.moveControl = new DolphinlikeMoveHelperController(this);
        this.lookControl = new DolphinLookController(this, 10);
        this.setCanPickUpLoot(true);
    }

    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return MobEntity.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.2F)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    public static boolean checkGobblefinSpawnRules(EntityType<Gobblefin> type, IWorld world, SpawnReason spawnReason, BlockPos blockPos, Random random) {
        if (blockPos.getY() > 45 && blockPos.getY() < world.getSeaLevel()) {
            Optional<RegistryKey<Biome>> biomeName = world.getBiomeName(blockPos);
            return (!Objects.equals(biomeName, Optional.of(Biomes.OCEAN)) || !Objects.equals(biomeName, Optional.of(Biomes.DEEP_OCEAN)))
                    && world.getFluidState(blockPos).is(FluidTags.WATER);
        } else {
            return false;
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_EAT_STATE, EatState.MOUTH_CLOSED);
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT pCompound) {
        super.addAdditionalSaveData(pCompound);
        this.writeInventory(pCompound);
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.readInventory(pCompound);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource pSource, int pLooting, boolean pRecentlyHit) {
        super.dropCustomDeathLoot(pSource, pLooting, pRecentlyHit);
        this.getInventory().removeAllItems().forEach(this::spawnAtLocation);
    }

    @Override
    protected void registerGoals() {
        //this.goalSelector.addGoal(0, new BreatheAirGoal(this));
        this.goalSelector.addGoal(0, new FindWaterGoal(this));
        //this.goalSelector.addGoal(1, new DolphinEntity.SwimToTreasureGoal(this));
        //this.goalSelector.addGoal(2, new DolphinEntity.SwimWithPlayerGoal(this, 4.0D));
        this.goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0D, 10));
        this.goalSelector.addGoal(4, new LookRandomlyGoal(this));
        this.goalSelector.addGoal(5, new LookAtGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.addGoal(5, new DolphinlikeJumpGoal<>(this, 10, 0.6D, 0.7D));
        this.goalSelector.addGoal(6, new MeleeAttackGoal(this, 1.2F, true));
        this.goalSelector.addGoal(8, new EatItemsGoal<>(this, 20, 8, 100, 1.2F, SUCK_UP_DURATION, SWALLOW_DURATION, THROW_UP_DURATION));
        //this.goalSelector.addGoal(8, new FollowBoatGoal(this));
        this.goalSelector.addGoal(9, new AvoidEntityGoal<>(this, GuardianEntity.class, 8.0F, 1.0D, 1.0D));

        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this, GuardianEntity.class)).setAlertOthers());
    }

    @Override
    protected PathNavigator createNavigation(World pLevel) {
        return new SwimmerPathNavigator(this, pLevel);
    }

    @Override
    public boolean doHurtTarget(Entity pEntity) {
        boolean hurt = pEntity.hurt(DamageSource.mobAttack(this), (float)((int)this.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        if (hurt) {
            this.doEnchantDamageEffects(this, pEntity);
            this.playSound(SoundEvents.DOLPHIN_ATTACK, 1.0F, 1.0F);
        }
        return hurt;
    }

    @Override
    protected float getStandingEyeHeight(Pose pPose, EntitySize pSize) {
        return 0.3F;
    }

    @Override
    public int getMaxHeadXRot() {
        return 1;
    }

    @Override
    public int getMaxHeadYRot() {
        return 1;
    }

    @Override
    protected boolean canRide(Entity pEntity) {
        return true;
    }

    @Override
    public boolean canTakeItem(ItemStack pItemstack) {
        EquipmentSlotType slotForItem = MobEntity.getEquipmentSlotForItem(pItemstack);
        if (!this.getItemBySlot(slotForItem).isEmpty()) {
            return false;
        } else {
            return slotForItem == EquipmentSlotType.MAINHAND && super.canTakeItem(pItemstack);
        }
    }

    @Override
    public boolean wantsToPickUp(ItemStack stack) {
        return ForgeEventFactory.getMobGriefingEvent(this.level, this) && this.canPickUpLoot() && this.canPutInMainhand(stack);
    }

    private boolean canPutInMainhand(ItemStack stack){
        return true;
    }

    private boolean canPutInInventory(ItemStack stack) {
        return this.getInventory().canAddItem(stack);
    }

    @Override
    protected void pickUpItem(ItemEntity pItemEntity) {
        this.pickUpItemAndPutInMainhand(pItemEntity);
    }

    private void pickUpItemAndPutInMainhand(ItemEntity pItemEntity) {
        if (this.getItemBySlot(EquipmentSlotType.MAINHAND).isEmpty()) {
            ItemStack itemstack = pItemEntity.getItem();
            if (this.canHoldItem(itemstack)) {
                this.onItemPickup(pItemEntity);
                this.setItemSlot(EquipmentSlotType.MAINHAND, itemstack);
                this.handDropChances[EquipmentSlotType.MAINHAND.getIndex()] = 2.0F;
                this.take(pItemEntity, itemstack.getCount());
                pItemEntity.remove();
            }
        }
    }

    private void pickupItemAndPutInInventory(ItemEntity pItemEntity) {
        this.onItemPickup(pItemEntity);
        this.take(pItemEntity, 1);
        ItemStack oneItem = AccessModUtil.removeOneItemFromItemEntity(pItemEntity);
        ItemStack remainder = this.getInventory().addItem(oneItem);
        AccessModUtil.throwItemsTowardRandomPos(this, Collections.singletonList(remainder));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isNoAi()) {
        } else {
            if (this.isInWaterRainOrBubble()) {
            } else {
                if (this.onGround) {
                    this.setDeltaMovement(this.getDeltaMovement().add((this.random.nextFloat() * 2.0F - 1.0F) * 0.2F, 0.5D, (this.random.nextFloat() * 2.0F - 1.0F) * 0.2F));
                    this.yRot = this.random.nextFloat() * 360.0F;
                    this.onGround = false;
                    this.hasImpulse = true;
                }
            }

            if (this.level.isClientSide && this.isInWater() && this.getDeltaMovement().lengthSqr() > 0.03D) {
                Vector3d vector3d = this.getViewVector(0.0F);
                float f = MathHelper.cos(this.yRot * AccessModUtil.TO_RADIANS) * 0.3F;
                float f1 = MathHelper.sin(this.yRot * AccessModUtil.TO_RADIANS) * 0.3F;
                float f2 = 1.2F - this.random.nextFloat() * 0.7F;

                for(int i = 0; i < 2; ++i) {
                    this.level.addParticle(ParticleTypes.DOLPHIN, this.getX() - vector3d.x * (double)f2 + (double)f, this.getY() - vector3d.y, this.getZ() - vector3d.z * (double)f2 + (double)f1, 0.0D, 0.0D, 0.0D);
                    this.level.addParticle(ParticleTypes.DOLPHIN, this.getX() - vector3d.x * (double)f2 - (double)f, this.getY() - vector3d.y, this.getZ() - vector3d.z * (double)f2 - (double)f1, 0.0D, 0.0D, 0.0D);
                }
            }

        }
    }

    @Override
    public void handleEntityEvent(byte pId) {
        if (pId == 38) {
            this.addParticlesAroundSelf(ParticleTypes.HAPPY_VILLAGER);
        } else {
            super.handleEntityEvent(pId);
        }

    }

    private void addParticlesAroundSelf(IParticleData particleData) {
        for(int i = 0; i < 7; ++i) {
            double xSpeed = this.random.nextGaussian() * 0.01D;
            double ySpeed = this.random.nextGaussian() * 0.01D;
            double zSpeed = this.random.nextGaussian() * 0.01D;
            this.level.addParticle(particleData, this.getRandomX(1.0D), this.getRandomY() + 0.2D, this.getRandomZ(1.0D), xSpeed, ySpeed, zSpeed);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.DOLPHIN_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.DOLPHIN_DEATH;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return this.isInWater() ? SoundEvents.DOLPHIN_AMBIENT_WATER : SoundEvents.DOLPHIN_AMBIENT;
    }

    @Override
    protected SoundEvent getSwimSplashSound() {
        return SoundEvents.DOLPHIN_SPLASH;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.DOLPHIN_SWIM;
    }

    protected boolean closeToNextPos() {
        BlockPos blockpos = this.getNavigation().getTargetPos();
        return blockpos != null ? blockpos.closerThan(this.position(), 12.0D) : false;
    }

    @Override
    public void travel(Vector3d pTravelVector) {
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(this.getSpeed(), pTravelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
            if (this.getTarget() == null) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.005D, 0.0D));
            }
        } else {
            super.travel(pTravelVector);
        }
    }

    @Override
    public boolean canBeLeashed(PlayerEntity pPlayer) {
        return true;
    }

    /**
     * Methods for {@link IAnimatable}
     */

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 0, this::animationPredicate));
    }

    private <T extends Gobblefin> PlayState animationPredicate(AnimationEvent<T> event) {
        if(this.isMouthOpen()){
            event.getController().setAnimation(SWIM_MOUTH_OPEN_ANIM);
        } else if(this.isSuckingUp()){
            event.getController().setAnimation(WHIRLWIND_ANIM);
        } else if(this.isSwallowing()){
            event.getController().setAnimation(EAT_ANIM);
        } else if(this.isThrowingUp()){
            event.getController().setAnimation(THROWUP_ANIM);
        }

        // mouth closed, use regular animations
        else if(event.isMoving()){
            event.getController().setAnimation(SWIM_ANIM);
        } else{
            event.getController().setAnimation(IDLE_ANIM);
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    /**
     * Methods for {@link Eater}
     */

    @Override
    public EatState getEatState() {
        return this.entityData.get(DATA_EAT_STATE);
    }

    @Override
    public void setEatState(EatState eatState) {
        this.entityData.set(DATA_EAT_STATE, eatState);
    }

    /**
     * Methods for {@link InventoryHolder}
     */

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }
}
