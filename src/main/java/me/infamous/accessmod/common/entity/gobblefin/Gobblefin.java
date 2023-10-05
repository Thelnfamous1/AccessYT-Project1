package me.infamous.accessmod.common.entity.gobblefin;

import me.infamous.accessmod.common.AccessModUtil;
import me.infamous.accessmod.common.entity.ai.InventoryHolder;
import me.infamous.accessmod.common.entity.ai.OwnableMob;
import me.infamous.accessmod.common.entity.ai.eater.EatItemsGoal;
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
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.SwimmerPathNavigator;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.server.ServerWorld;
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
import java.util.*;

public class Gobblefin extends WaterMobEntity implements IAnimatable, Eater, InventoryHolder, OwnableMob {
    public static final int TAME_EVENT_ID = 38;
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

    private static final DataParameter<Integer> DATA_EAT_TARGET_ID = EntityDataManager.defineId(Gobblefin.class, DataSerializers.INT);

    private static final DataParameter<Optional<UUID>> DATA_ID_OWNER_UUID = EntityDataManager.defineId(Gobblefin.class, DataSerializers.OPTIONAL_UUID);

    private final Inventory inventory = new Inventory(8);
    private int throwUpTicks;
    @Nullable
    private Entity cachedEatTarget;
    @Nullable
    private UUID eatTargetUUID;

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
        this.entityData.define(DATA_EAT_TARGET_ID, 0);
        this.entityData.define(DATA_ID_OWNER_UUID, Optional.empty());
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
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.2F));
        this.goalSelector.addGoal(2, new EatItemsGoal<>(this, 20, 8, 100, 1.2F, SUCK_UP_DURATION, SWALLOW_DURATION));
        this.goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0D, 10));
        this.goalSelector.addGoal(4, new LookRandomlyGoal(this));
        this.goalSelector.addGoal(5, new LookAtGoal(this, PlayerEntity.class, 6.0F));
        //this.goalSelector.addGoal(5, new DolphinlikeJumpGoal<>(this, 10, 0.6D, 0.7D));
        //this.goalSelector.addGoal(6, new MeleeAttackGoal(this, 1.2F, true));
        //this.goalSelector.addGoal(8, new FollowBoatGoal(this));
        this.goalSelector.addGoal(9, new AvoidEntityGoal<>(this, GuardianEntity.class, 8.0F, 1.0D, 1.0D));
        //this.targetSelector.addGoal(1, (new HurtByTargetGoal(this, GuardianEntity.class)).setAlertOthers());
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
    public ActionResultType mobInteract(PlayerEntity pPlayer, Hand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        if(this.isFood(itemstack) && this.getOwnerUUID() == null){
            if(!this.level.isClientSide){
                if(!pPlayer.abilities.instabuild) itemstack.shrink(1);
                this.setOwnerUUID(pPlayer.getUUID());
                this.level.broadcastEntityEvent(this, (byte) TAME_EVENT_ID);
            }
            return ActionResultType.sidedSuccess(this.level.isClientSide);
        }
        return super.mobInteract(pPlayer, pHand);
    }

    private boolean isFood(ItemStack itemstack) {
        return itemstack.getItem().is(AccessModUtil.GOBBLEFIN_FOOD);
    }

    protected void doPlayerRide(PlayerEntity pPlayer) {
        if (!this.level.isClientSide) {
            pPlayer.yRot = this.yRot;
            pPlayer.xRot = this.xRot;
            pPlayer.startRiding(this);
        }

    }

    @Override
    public boolean wantsToPickUp(ItemStack stack) {
        return ForgeEventFactory.getMobGriefingEvent(this.level, this) && this.canPickUpLoot();
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if(this.throwUpTicks > 0){
            this.throwUpTicks--;
            if(this.throwUpTicks == 0 && !this.level.isClientSide && this.isThrowingUp()){
                this.setMouthClosed();
            }
        }
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
    public void aiStep() {
        super.aiStep();

        if(this.isSuckingUp()){
            Entity eatTarget = this.getEatTarget();
            if(eatTarget != null){
                // destination is the mob's mouth
                Vector3d mouthPosition = this.getMouthPosition();
                if(eatTarget.distanceToSqr(mouthPosition) >= this.getBbWidth() * 0.5F){
                    double x = mouthPosition.x - eatTarget.getX();
                    double y = mouthPosition.y - eatTarget.getY();
                    double z = mouthPosition.z - eatTarget.getZ();
                    Vector3d moveVec = new Vector3d(x, y, z).normalize().scale(0.025D); // we want to move at a speed of one block per 40 ticks
                    eatTarget.setDeltaMovement(eatTarget.getDeltaMovement().add(moveVec));
                }
            }
        }
    }

    public Vector3d getMouthPosition(){
        Vector3d lookAngle = this.getLookAngle();
        float width = this.getBbWidth();
        double mouthX = this.getX() + (lookAngle.x * width * 0.5F);
        double mouthY = this.getY() + (lookAngle.y * width * 0.5F);
        double mouthZ = this.getZ() + (lookAngle.z * width * 0.5F);
        return new Vector3d(mouthX, mouthY, mouthZ);
    }

    @Override
    public void handleEntityEvent(byte pId) {
        if (pId == TAME_EVENT_ID) {
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

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        boolean hurt = super.hurt(pSource, pAmount);
        if(hurt && !this.getInventory().isEmpty()){
            this.setThrowingUp();
            this.throwUpTicks = THROW_UP_DURATION;
            AccessModUtil.throwItemsTowardRandomPos(this, this.getInventory().removeAllItems());
        }
        return hurt;
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

    @Override
    public boolean wantsToEat(ItemEntity item) {
        return this.getInventory().canAddItem(item.getItem());
    }

    @Override
    public void eat(ItemEntity item) {
        this.onItemPickup(item);
        ItemStack stack = item.getItem();
        ItemStack remainder = this.getInventory().addItem(stack);
        this.take(item, stack.getCount());
        item.remove();
        if(!remainder.isEmpty()) AccessModUtil.throwItemsTowardRandomPos(this, Collections.singletonList(remainder));
    }

    @Override
    public void setEatTarget(@Nullable Entity target) {
        this.cachedEatTarget = target;
        this.eatTargetUUID = target == null ? null : target.getUUID();
        this.entityData.set(DATA_EAT_TARGET_ID, target == null ? 0 : target.getId());
    }

    @Override
    @Nullable
    public Entity getEatTarget() {
        if(this.cachedEatTarget == null || this.cachedEatTarget.removed){
            int eatTargetId = this.entityData.get(DATA_EAT_TARGET_ID);
            if(this.level.isClientSide && eatTargetId != 0){
                this.cachedEatTarget = this.level.getEntity(eatTargetId);
            } else if(!this.level.isClientSide && this.eatTargetUUID != null){
                this.setEatTarget(((ServerWorld)this.level).getEntity(this.eatTargetUUID));
            } else{
                this.cachedEatTarget = null;
            }
        }
        return this.cachedEatTarget;
    }

    /**
     * Methods for {@link InventoryHolder}
     */

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    @Override
    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_ID_OWNER_UUID).orElse(null);
    }

    @Override
    public void setOwnerUUID(@Nullable UUID pUniqueId) {
        this.entityData.set(DATA_ID_OWNER_UUID, Optional.ofNullable(pUniqueId));
    }
}
