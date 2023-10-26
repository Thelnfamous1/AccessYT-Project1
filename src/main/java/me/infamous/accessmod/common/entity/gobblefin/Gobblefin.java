package me.infamous.accessmod.common.entity.gobblefin;

import me.infamous.accessmod.common.AccessModUtil;
import me.infamous.accessmod.common.entity.ai.DynamicSwimmer;
import me.infamous.accessmod.common.entity.ai.InventoryHolder;
import me.infamous.accessmod.common.entity.ai.RideableRandomSwimmingGoal;
import me.infamous.accessmod.common.entity.ai.TameableMob;
import me.infamous.accessmod.common.entity.ai.eater.Feedable;
import me.infamous.accessmod.common.entity.ai.eater.VortexEatGoal;
import me.infamous.accessmod.common.entity.ai.eater.VortexEater;
import me.infamous.accessmod.common.registry.AccessModDataSerializers;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.DolphinLookController;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.GuardianEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.WaterMobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SCameraPacket;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.SwimmerPathNavigator;
import net.minecraft.scoreboard.Team;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
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

public class Gobblefin extends WaterMobEntity implements IAnimatable, VortexEater, Feedable, InventoryHolder, TameableMob, DynamicSwimmer {
    public static final double FAST_SWIM_SPEED_MODIFIER = 1.5D;
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

    private static final DataParameter<Boolean> DATA_TAME = EntityDataManager.defineId(Gobblefin.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> DATA_SWIMMING_FAST = EntityDataManager.defineId(Gobblefin.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> DATA_IS_VORTEX_ACTIVE = EntityDataManager.defineId(Gobblefin.class, DataSerializers.BOOLEAN);

    private final Inventory inventory = new Inventory(8);
    private int eatStateTimer;
    private boolean manualBoosting;
    private boolean manualVortex;
    private Vortex vortex;
    private int activeVortexTicks;
    private final Map<UUID, SimpleTicker> trappedPassengers = new HashMap<>();

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
                .add(Attributes.ATTACK_DAMAGE, 10.0D); // deals damage through vortex attack
    }

    public static boolean checkGobblefinSpawnRules(EntityType<Gobblefin> type, IWorld world, SpawnReason spawnReason, BlockPos blockPos, Random random) {
        if (blockPos.getY() > 45 && blockPos.getY() < world.getSeaLevel()) {
            return world.getFluidState(blockPos).is(FluidTags.WATER);
        } else {
            return false;
        }
    }

    @Override
    public boolean removeWhenFarAway(double pDistanceToClosestPlayer) {
        return !this.isPersistenceRequired();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_EAT_STATE, EatState.MOUTH_CLOSED);
        this.entityData.define(DATA_EAT_TARGET_ID, 0);
        this.entityData.define(DATA_ID_OWNER_UUID, Optional.empty());
        this.entityData.define(DATA_TAME, false);
        this.entityData.define(DATA_SWIMMING_FAST, false);
        this.entityData.define(DATA_IS_VORTEX_ACTIVE, false);
    }

    @Override
    public void onSyncedDataUpdated(DataParameter<?> pKey) {
        super.onSyncedDataUpdated(pKey);
        if(pKey.equals(DATA_IS_VORTEX_ACTIVE)){
            this.setActiveVortexTicks(0);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT pCompound) {
        super.addAdditionalSaveData(pCompound);
        this.writeInventory(pCompound);
        this.writeOwner(pCompound);
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.readInventory(pCompound);
        this.readOwner(pCompound);
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
        this.goalSelector.addGoal(1, new PanicGoal(this, FAST_SWIM_SPEED_MODIFIER));
        this.goalSelector.addGoal(2, new VortexEatGoal<>(this, 20, 8, 100, FAST_SWIM_SPEED_MODIFIER));
        this.goalSelector.addGoal(4, new RideableRandomSwimmingGoal(this, 1.0D, 10));
        this.goalSelector.addGoal(4, new LookRandomlyGoal(this));
        this.goalSelector.addGoal(5, new LookAtGoal(this, PlayerEntity.class, 6.0F));
        //this.goalSelector.addGoal(5, new DolphinlikeJumpGoal<>(this, 10, 0.6D, 0.7D));
        //this.goalSelector.addGoal(6, new MeleeAttackGoal(this, 1.2F, true));
        //this.goalSelector.addGoal(8, new FollowBoatGoal(this));
        this.goalSelector.addGoal(9, new AvoidEntityGoal<>(this, GuardianEntity.class, 8.0F, 1.0D, FAST_SWIM_SPEED_MODIFIER));
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
    public ActionResultType mobInteract(PlayerEntity pPlayer, Hand pHand) {
        ItemStack itemInHand = pPlayer.getItemInHand(pHand);
        Item item = itemInHand.getItem();
        if (this.level.isClientSide) {
            if (this.isTame() && this.isOwnedBy(pPlayer, this.level)) {
                return ActionResultType.SUCCESS;
            } else {
                return !this.isFood(itemInHand) || !(this.getHealth() < this.getMaxHealth()) && this.isTame() ? ActionResultType.PASS : ActionResultType.SUCCESS;
            }
        } else {
            if (this.isTame()) {
                if(this.isOwnedBy(pPlayer, this.level)){
                    if (this.isFood(itemInHand) && this.getHealth() < this.getMaxHealth()) {
                        if (!pPlayer.abilities.instabuild) {
                            itemInHand.shrink(1);
                        }

                        this.heal(item.isEdible() ? (float)item.getFoodProperties().getNutrition() : 1.0F);
                        return ActionResultType.SUCCESS;
                    }

                    if(!this.isVehicle()){
                        this.doPlayerRide(pPlayer);
                        return ActionResultType.SUCCESS;
                    }

                }
            } else if (this.isFood(itemInHand)) {
                if (!pPlayer.abilities.instabuild) {
                    itemInHand.shrink(1);
                }

                this.tame(pPlayer);
                this.level.broadcastEntityEvent(this, (byte)TameableMob.SUCCESSFUL_TAME_ID);

                this.setPersistenceRequired();
                return ActionResultType.SUCCESS;
            }

            return super.mobInteract(pPlayer, pHand);
        }
    }

    protected void doPlayerRide(PlayerEntity pPlayer) {
        if (!this.level.isClientSide) {
            if(pPlayer.startRiding(this)){
                pPlayer.yRot = this.yRot;
                pPlayer.xRot = this.xRot;
                if(!this.getEatState().isTransitional()) this.setSwallowing();
            }
        }
    }

    @Override
    protected void addPassenger(Entity mountedBy) {
        super.addPassenger(mountedBy);
        if(mountedBy instanceof LivingEntity){
            if(!this.isOwnedBy((LivingEntity) mountedBy, this.level)){
                this.trappedPassengers.put(mountedBy.getUUID(), new SimpleTicker(1200));
                if(this.canBeControlledByRider() && mountedBy instanceof ServerPlayerEntity){
                    LivingEntity controllingPassenger = this.getControllingPassenger();
                    if(controllingPassenger != null){
                        this.changePassengerCamera((ServerPlayerEntity) mountedBy, controllingPassenger);
                    }
                }
            } else if(this.canBeControlledByRider()){
                LivingEntity controllingPassenger = this.getControllingPassenger();
                if(controllingPassenger != null){
                    for(Entity passenger : this.getPassengers()){
                        if(passenger != controllingPassenger && passenger instanceof ServerPlayerEntity){
                            this.changePassengerCamera((ServerPlayerEntity) passenger, controllingPassenger);
                        }
                    }
                }
            }
        }
    }

    private void changePassengerCamera(ServerPlayerEntity passenger, Entity camera) {
        passenger.connection.send(new SCameraPacket(camera));
    }

    @Override
    protected void removePassenger(Entity dismountedBy) {
        super.removePassenger(dismountedBy);
        this.trappedPassengers.remove(dismountedBy.getUUID());
        if(dismountedBy instanceof LivingEntity && this.isOwnedBy((LivingEntity) dismountedBy, this.level)){
            for(Entity passenger : this.getPassengers()){
                if(passenger instanceof ServerPlayerEntity){
                    this.resetPassengerCamera(((ServerPlayerEntity) passenger));
                }
            }
        } else if(dismountedBy instanceof ServerPlayerEntity){
            this.resetPassengerCamera(((ServerPlayerEntity) dismountedBy));
        }
    }

    private void resetPassengerCamera(ServerPlayerEntity passenger) {
        passenger.connection.send(new SCameraPacket(passenger));
    }

    // various method overrides associated with TameableEntity
    @Override
    public boolean canAttack(LivingEntity pTarget) {
        return !this.isOwnedBy(pTarget, this.level) && super.canAttack(pTarget);
    }

    @Override
    public Team getTeam() {
        if (this.isTame()) {
            LivingEntity owner = this.getOwner(this.level);
            if (owner != null) {
                return owner.getTeam();
            }
        }

        return super.getTeam();
    }

    @Override
    public boolean isAlliedTo(Entity pEntity) {
        if (this.isTame()) {
            LivingEntity owner = this.getOwner(this.level);
            if (pEntity == owner) {
                return true;
            }

            if (owner != null) {
                return owner.isAlliedTo(pEntity);
            }
        } else if (pEntity instanceof Gobblefin) {
            return this.getTeam() == null && pEntity.getTeam() == null;
        }
        return super.isAlliedTo(pEntity);
    }

    @Override
    public void die(DamageSource pCause) {
        LivingEntity owner = this.getOwner(this.level);
        if (!this.level.isClientSide && this.level.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES) && owner instanceof ServerPlayerEntity) {
            owner.sendMessage(this.getCombatTracker().getDeathMessage(), Util.NIL_UUID);
        }

        super.die(pCause);
    }

    @Override
    public boolean wantsToPickUp(ItemStack stack) {
        return ForgeEventFactory.getMobGriefingEvent(this.level, this) && this.canPickUpLoot();
    }

    @Override
    public void baseTick() {
        super.baseTick();

        this.getPassengers().forEach(passenger -> {
            if (passenger.getAirSupply() < passenger.getMaxAirSupply()) {
                this.replenishAirSupplyForPassenger(passenger);
            }
        });
    }

    protected void replenishAirSupplyForPassenger(Entity entity){
        // min call recreates logic in LivingEntity#increaseAirSupply
        entity.setAirSupply(Math.min(entity.getAirSupply() + 4, entity.getMaxAirSupply()));
    }

    @Override
    public void tick() {
        super.tick();

        for (UUID trappedPassenger : this.trappedPassengers.keySet()) {
            SimpleTicker ticker = this.trappedPassengers.get(trappedPassenger);
            if (ticker != null) {
                ticker.tick();
                if (!ticker.isActive() || trappedPassenger.equals(this.getOwnerUUID())) {
                    this.trappedPassengers.remove(trappedPassenger);
                }
            }
        }

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
        this.updateEating(this.level, this.manualVortex);
    }

    @Override
    public void customServerAiStep() {
        if (!this.manualBoosting){
            if(!this.canBeControlledByRider() && this.getMoveControl().hasWanted()) {
                double speedModifier = this.getMoveControl().getSpeedModifier();
                this.setSwimmingFast(speedModifier > 1.0F);
            } else{
                this.setSwimmingFast(false);
            }
        } else{
            this.setSwimmingFast(true);
        }
    }

    @Override
    public void handleEntityEvent(byte pId) {
        if (pId == SUCCESSFUL_TAME_ID) {
            this.spawnTamingParticles(true);
        } else if (pId == FAILED_TAME_ID) {
            this.spawnTamingParticles(false);
        } else {
            super.handleEntityEvent(pId);
        }
    }

    protected void spawnTamingParticles(boolean successful) {
        IParticleData particleType = ParticleTypes.HEART;
        if (!successful) {
            particleType = ParticleTypes.SMOKE;
        }

        for(int i = 0; i < 7; ++i) {
            double x = this.random.nextGaussian() * 0.02D;
            double y = this.random.nextGaussian() * 0.02D;
            double z = this.random.nextGaussian() * 0.02D;
            this.level.addParticle(particleType, this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), x, y, z);
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
    public boolean canBeLeashed(PlayerEntity pPlayer) {
        return true;
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        boolean hurt = super.hurt(pSource, pAmount);
        if(hurt && !this.getInventory().isEmpty()){
            if(!this.level.isClientSide){
                this.setThrowingUp();
                this.playSound(SoundEvents.FOX_SPIT, 1.0F, 1.0F);
                List<ItemStack> eatenItems = this.getInventory().removeAllItems();
                Vector3d mouthPos = this.getMouthPosition();
                eatenItems.forEach(eatenItem -> {
                    if (!eatenItem.isEmpty()) {
                        ItemEntity spitOut = new ItemEntity(this.level,
                                mouthPos.x,
                                mouthPos.y,
                                mouthPos.z, eatenItem);
                        spitOut.setPickUpDelay(40);
                        spitOut.setThrower(this.getUUID());
                        this.level.addFreshEntity(spitOut);
                    }
                });
            }
        }
        return hurt;
    }

    // Vanilla horse-like riding method overrides and implementations

    @Override
    protected boolean canRide(Entity pEntity) {
        return true;
    }

    @Override
    protected boolean canAddPassenger(Entity pPassenger) {
        return this.getPassengers().size() < 2;
    }

    @Override
    public void positionRider(Entity pPassenger) {
        int passengerIdx = this.getPassengers().indexOf(pPassenger);
        if (passengerIdx >= 0) {
            boolean isControllingPassenger = passengerIdx == 0;
            float zOffset = 0.5F;
            double yOffset = this.getPassengersRidingOffset() + pPassenger.getMyRidingOffset();
            if (this.getPassengers().size() > 1) {
                if (!isControllingPassenger) {
                    zOffset = -0.7F;
                }

                if (pPassenger instanceof AnimalEntity) {
                    zOffset += 0.2F;
                }
            }

            Vector3d rideOffset = (new Vector3d(0.0D, 0.0D, zOffset)).yRot(-this.yBodyRot * AccessModUtil.TO_RADIANS);
            pPassenger.setPos(this.getX() + rideOffset.x, this.getY() + yOffset, this.getZ() + rideOffset.z);
        }
    }

    @Override
    public boolean canBeControlledByRider() {
        LivingEntity controllingPassenger = this.getControllingPassenger();
        if(controllingPassenger == null) return false;
        return this.isOwnedBy(controllingPassenger, this.level);
    }

    @Nullable
    public LivingEntity getControllingPassenger() {
        if(!this.getPassengers().isEmpty()){
            Entity firstPassenger = this.getPassengers().get(0);
            if(firstPassenger instanceof LivingEntity){
                return (LivingEntity) firstPassenger;
            }
        }
        return null;
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.0D;
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() && this.isVehicle();
    }

    @Override
    protected void doPush(Entity pEntity) {
        if(!this.isVortexActive()){
            super.doPush(pEntity);
        }
    }

    @Override
    public boolean isPushable() {
        return !this.isVehicle();
    }

    // horse-like travel
    @Override
    public void travel(Vector3d pTravelVector) {
        if (this.isAlive()) {
            if (this.isVehicle() && this.canBeControlledByRider()) {
                LivingEntity rider = this.getControllingPassenger();
                this.yRot = rider.yRot;
                this.yRotO = this.yRot;
                this.xRot = rider.xRot;
                this.setRot(this.yRot, this.xRot);
                this.yBodyRot = this.yRot;
                this.yHeadRot = this.yRot;

                float left = rider.xxa * 0.5F;
                float up = -MathHelper.sin(this.xRot * AccessModUtil.TO_RADIANS);
                float forward = rider.zza;
                if (forward <= 0.0F) {
                    forward *= 0.25F;
                }

                if (this.onGround && this.getEatState().isTransitional()) {
                    left = 0.0F;
                    up = 0.0F;
                    forward = 0.0F;
                }

                this.flyingSpeed = this.getSpeed() * 0.1F;
                if (this.isControlledByLocalInstance()) {
                    float movementSpeed = this.getMovementSpeed();
                    if(this.isSwimmingFast()){
                        movementSpeed *= FAST_SWIM_SPEED_MODIFIER;
                    }
                    this.setSpeed(movementSpeed);
                    Vector3d inputVector = new Vector3d(left, up, forward);
                    // need this to mimic what the move controller does, as it does not tick on the client
                    if(this.isInWater()){
                        this.setDeltaMovement(this.getDeltaMovement().add(0.0D, DolphinlikeMoveHelperController.SINK_Y_OFFSET, 0.0D));
                    }
                    this.swimTravel(inputVector, true);
                } else if (rider instanceof PlayerEntity) {
                    this.setDeltaMovement(Vector3d.ZERO); // server does not dictate any movement of the gobblefin if mounted by a player
                }
                this.calculateEntityAnimation(this, false);
            } else {
                this.flyingSpeed = 0.02F; // default
                this.swimTravel(pTravelVector, false);
            }
        }

    }

    private float getMovementSpeed() {
        return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * (this.isInWater() ?
                DolphinlikeMoveHelperController.WATER_SPEED_MODIFIER : DolphinlikeMoveHelperController.LAND_SPEED_MODIFIER);
    }

    // dolphin-like travel
    private void swimTravel(Vector3d pTravelVector, boolean playerControlled){
        if ((playerControlled || this.isEffectiveAi()) && this.isInWater()) {
            this.moveRelative(this.getSpeed(), pTravelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
            if (this.getTarget() == null) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -DolphinlikeMoveHelperController.SINK_Y_OFFSET, 0.0D));
            }
        } else {
            super.travel(pTravelVector);
        }
    }

    @Override
    public Vector3d getDismountLocationForPassenger(LivingEntity passenger) {
        return this.getMouthPosition();
    }

    @Override
    public boolean rideableUnderWater() {
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
            event.getController().setAnimation(this.isSwimmingFast() ? SWIM_ANIM : SWIM_SLOW_ANIM);
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
     * Methods for {@link VortexEater}
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
    public int getEatStateTimer() {
        return this.eatStateTimer;
    }

    @Override
    public void setEatStateTimer(int eatActionTimer) {
        this.eatStateTimer = eatActionTimer;
    }

    @Override
    public int getEatActionPoint(EatState eatState) {
        if(eatState == EatState.SWALLOWING) return SWALLOW_DURATION / 2;
        return 0;
    }

    @Override
    public boolean canEat(Entity target) {
        if(!target.isAlive()) return false;
        if(this.isAlliedTo(target)) return false;
        if(!this.canSee(target)) return false;
        if(!EntityPredicates.NO_CREATIVE_OR_SPECTATOR.test(target)) return false;
        if(this.getPassengers().contains(target)) return false;

        if(target instanceof ItemEntity){
            ItemEntity targetItem = (ItemEntity) target;
            return !targetItem.getItem().isEmpty()
                    && !targetItem.hasPickUpDelay()
                    && this.wantsToPickUp(targetItem.getItem())
                    && this.getInventory().canAddItem(((ItemEntity)target).getItem());
        } else if(target instanceof LivingEntity){
            LivingEntity targetLiving = (LivingEntity) target;
            LivingEntity owner = this.getOwner(this.level);
            if(owner != null){
                if(!this.wantsToAttack(targetLiving, owner, this.level)) return false;
            }
            return this.canAttack(targetLiving);
        } else{
            return true;
        }
    }

    @Override
    public void eat(Entity target) {
        if(!target.isAlive()) return;

        if(target instanceof ItemEntity){
            ItemEntity itemEntity = (ItemEntity) target;
            this.onItemPickup(itemEntity);
            ItemStack toTake = itemEntity.getItem();
            /*
            if(this.getControllingPassenger() instanceof PlayerEntity){
                toTake = this.riderTakeItem((PlayerEntity) this.getControllingPassenger(), toTake);
            }
             */
            ItemStack remainder = this.getInventory().addItem(toTake);
            this.take(itemEntity, toTake.getCount());
            itemEntity.remove();
            if(!remainder.isEmpty()) {
                this.spawnAtLocation(remainder);
            }
            this.setPersistenceRequired();
        } else if(target instanceof PlayerEntity){
            this.doPlayerRide((PlayerEntity) target);
        } else{
            target.hurt(DamageSource.mobAttack(this), Float.MAX_VALUE); // insta-kill mobs
            if (!target.isAlive()) {
                target.remove();
            }
        }
    }

    @Override
    public void setActiveVortexTicks(int activeVortexTicks) {
        this.activeVortexTicks = activeVortexTicks;
    }

    @Override
    public int getActiveVortexTicks() {
        return this.activeVortexTicks;
    }

    private ItemStack riderTakeItem(PlayerEntity player, ItemStack toTake){
        return player.addItem(toTake) ? ItemStack.EMPTY : toTake;
    }

    @Nullable
    @Override
    public Vortex getActiveVortex() {
        return this.vortex;
    }

    @Override
    public void setActiveVortex(@Nullable Vortex vortex) {
        this.vortex = vortex;
        this.entityData.set(DATA_IS_VORTEX_ACTIVE, this.vortex != null);
        if(this.vortex != null){
            this.level.broadcastEntityEvent(this, VortexEater.VORTEX_EVENT_ID);
        }
    }

    @Override
    public boolean isVortexActive() {
        return this.entityData.get(DATA_IS_VORTEX_ACTIVE);
    }

    @Override
    public Vector3d getMouthPosition(){
        Vector3d lookAngle = this.getLookAngle();
        float width = this.getBbWidth();
        double mouthX = this.getX() + (lookAngle.x * width * 0.5F);
        double mouthY = this.getEyeY();
        double mouthZ = this.getZ() + (lookAngle.z * width * 0.5F);
        return new Vector3d(mouthX, mouthY, mouthZ);
    }

    @Override
    public Vortex createDefaultVortex(boolean breakBlocks) {
        return VortexHelper.vortex(this.level, this, this.getMouthPosition(), this.getBbWidth() * 0.5F, breakBlocks ? Vortex.Mode.BREAK : Vortex.Mode.NONE);
    }

    @Override
    public int getEatStateDuration(EatState eatState) {
        switch(eatState){
            case SUCKING_UP:
                return SUCK_UP_DURATION;
            case SWALLOWING:
                return SWALLOW_DURATION;
            case THROWING_UP:
                return THROW_UP_DURATION;
        }
        return 0;
    }

    @Override
    public void playEatSound() {
        this.playSound(SoundEvents.DOLPHIN_EAT, 1.0F, 1.0F);
    }

    @Override
    public int getVortexDuration() {
        return this.getEatStateDuration(EatState.SUCKING_UP) + this.getEatStateDuration(EatState.SWALLOWING);
    }

    @Override
    public boolean isWithinEatRange(Entity target) {
        return target.distanceToSqr(this.getMouthPosition()) < MathHelper.square(2.0F);
    }

    @Override
    public boolean isWithinVortexRange(Entity target) {
        return target.distanceToSqr(this.getMouthPosition()) < MathHelper.square(this.getBbWidth());
    }

    /**
     * Methods for {@link Feedable}
     */

    @Override
    public boolean isFood(ItemStack itemstack) {
        return itemstack.getItem().is(AccessModUtil.GOBBLEFIN_FOOD);
    }

    /**
     * Methods for {@link InventoryHolder}
     */

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    /**
     * Methods for {@link TameableMob}
     */

    @Override
    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_ID_OWNER_UUID).orElse(null);
    }

    @Override
    public void setOwnerUUID(@Nullable UUID pUniqueId) {
        this.entityData.set(DATA_ID_OWNER_UUID, Optional.ofNullable(pUniqueId));
    }

    @Override
    public void setTame(boolean tame) {
        this.entityData.set(DATA_TAME, tame);
    }

    @Override
    public boolean isTame() {
        return this.entityData.get(DATA_TAME);
    }

    @Override
    public boolean isOwnedBy(LivingEntity pEntity, World level) {
        return pEntity == this.getOwner(level);
    }

    /**
     * Methods for {@link DynamicSwimmer}
     */

    @Override
    public void setSwimmingFast(boolean swimmingFast) {
        this.entityData.set(Gobblefin.DATA_SWIMMING_FAST, swimmingFast);
    }

    @Override
    public boolean isSwimmingFast() {
        return this.entityData.get(Gobblefin.DATA_SWIMMING_FAST);
    }

    public void startManualVortex() {
        this.setSuckingUp(true);
        this.manualVortex = true;
    }

    public void stopManualVortex() {
        this.setMouthClosed();
        this.manualVortex = false;
    }

    public void startManualBoosting() {
        this.manualBoosting = true;
    }

    public void stopManualBoosting() {
        this.manualBoosting = false;
    }

    public boolean isTrappedPassenger(Entity entity) {
        if(!this.canBeControlledByRider()) return false;
        SimpleTicker ticker = this.trappedPassengers.get(entity.getUUID());
        return ticker != null && ticker.isActive();
    }
}
