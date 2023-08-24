package me.infamous.accessmod.common.entity;

import me.infamous.accessmod.common.entity.ai.*;
import me.infamous.accessmod.common.registry.AccessModDataSerializers;
import me.infamous.accessmod.datagen.AccessModTags;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.LookController;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;
import software.bernie.geckolib3.core.AnimationState;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.Random;

public class Dune extends MonsterEntity implements IAnimatable, AnimatableMeleeAttack, IEntityAdditionalSpawnData, Digger {
    public static final int ATTACK_ANIMATION_LENGTH = 18;
    public static final int ATTACK_ANIMATION_ACTION_POINT = 16;
    public static final int DIG_ANIMATION_LENGTH = 38;
    private final AnimationFactory factory = GeckoLibUtil.createFactory(this);
    protected static final AnimationBuilder IDLE_ANIM = new AnimationBuilder().addAnimation("idle", true);
    protected static final AnimationBuilder WALK_ANIM = new AnimationBuilder().addAnimation("walk", true);
    protected static final AnimationBuilder DRAG_ANIM = new AnimationBuilder().addAnimation("dragging", false);
    protected static final AnimationBuilder RANGED_ANIM = new AnimationBuilder().addAnimation("attack_distance", false);
    protected static final AnimationBuilder SPAWN_ANIM = new AnimationBuilder().addAnimation("spawn", false);
    protected static final AnimationBuilder MELEE_ANIM = new AnimationBuilder().addAnimation("attack", false);
    protected static final AnimationBuilder BURIED_ANIM = new AnimationBuilder().addAnimation("buried", ILoopType.EDefaultLoopTypes.HOLD_ON_LAST_FRAME);

    private static final DataParameter<Digger.DigState> DATA_DIG_STATE = EntityDataManager.defineId(Dune.class, AccessModDataSerializers.getSerializer(AccessModDataSerializers.DIG_STATE));
    private int attackAnimationTick;

    public Dune(EntityType<? extends Dune> entityType, World world) {
        super(entityType, world);
    }

    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return MonsterEntity.createMonsterAttributes()
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35F)
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D);
    }

    public static boolean checkDuneSpawnRules(EntityType<Dune> type, IServerWorld world, SpawnReason spawnReason, BlockPos blockPos, Random random) {
        return checkMonsterSpawnRules(type, world, spawnReason, blockPos, random)
                && (spawnReason != SpawnReason.NATURAL || world.getBlockState(blockPos.below()).is(AccessModTags.DUNES_SPAWN_ON))
                && (spawnReason == SpawnReason.SPAWNER || world.canSeeSky(blockPos));
    }

    @Override
    protected void registerGoals() {
        this.addMoveGoals();
        this.addLookGoals();
        this.addTargetGoals();
    }

    protected void addMoveGoals() {
        this.goalSelector.addGoal(0, new EmergingGoal<>(this, DIG_ANIMATION_LENGTH));
        this.goalSelector.addGoal(0, new DiggingGoal<>(this, DIG_ANIMATION_LENGTH));
        this.goalSelector.addGoal(1, new BuriedGoal<>(this));
        this.goalSelector.addGoal(2, new RestrictSunGoal(this));
        //this.goalSelector.addGoal(3, new FleeSunGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new AnimatableMeleeAttackGoal<>(this, 1.0D, false));
        this.goalSelector.addGoal(5, new AttackTurtleEggGoal(this, 1.0D, 3));
        //this.goalSelector.addGoal(6, new MoveThroughVillageGoal(this, 1.0D, true, 4, this::canBreakDoors));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomWalkingGoal(this, 1.0D));
    }

    private void addLookGoals() {
        this.goalSelector.addGoal(8, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(8, new LookRandomlyGoal(this));
    }

    protected void addTargetGoals() {
        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolemEntity.class, true));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, TurtleEntity.class, 10, true, false, TurtleEntity.BABY_ON_LAND_SELECTOR));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_DIG_STATE, DigState.SURFACED);
    }

    @Override
    public void onSyncedDataUpdated(DataParameter<?> pKey) {
        super.onSyncedDataUpdated(pKey);
        if (DATA_DIG_STATE.equals(pKey)) {
            this.refreshDimensions();
        }
    }

    @Override
    public void handleEntityEvent(byte pId) {
        if (pId == AnimatableMeleeAttack.START_ATTACK_EVENT) {
            this.startAttackAnimation();
        } else {
            super.handleEntityEvent(pId);
        }
    }

    @Override
    public LookController getLookControl() {
        return new DiggerLookController<>(this);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return !this.isNotSurfaced() ? SoundEvents.HUSK_AMBIENT : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.HUSK_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.HUSK_DEATH;
    }

    protected SoundEvent getStepSound() {
        return SoundEvents.HUSK_STEP;
    }

    @Override
    protected void playStepSound(BlockPos pPos, BlockState pBlock) {
        this.playSound(this.getStepSound(), 0.15F, 1.0F);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        this.tickAnimations();
    }

    private void tickAnimations() {
        if(this.attackAnimationTick > 0){
            this.attackAnimationTick--;
        }
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        /*
        if (this.isNotSurfaced()) {
            this.setTarget(null);
            this.getNavigation().stop();
            this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0D);
        }
         */
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return this.isNotSurfaced() && !source.isBypassInvul() || super.isInvulnerableTo(source);
    }

    private boolean isNotSurfaced() {
        return !this.isSurfaced();
    }

    @Override
    public boolean checkSpawnObstruction(IWorldReader pLevel) {
        return super.checkSpawnObstruction(pLevel) && pLevel.noCollision(this, this.getType().getDimensions().makeBoundingBox(this.position()));
    }

    @Override
    public boolean ignoreExplosion() {
        return this.isNotSurfaced();
    }

    @Override
    public EntitySize getDimensions(Pose pose) {
        EntitySize dimensions = super.getDimensions(pose);
        switch (this.getDigState()){
            case DIGGING:
            case EMERGING:
                return EntitySize.fixed(dimensions.width, dimensions.height / 2);
            case BURIED:
                return SLEEPING_DIMENSIONS;
            default:
                return dimensions;
        }
    }

    @Override
    public boolean isPushable() {
        return !this.isNotSurfaced() && super.isPushable();
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.readDigState(pCompound);
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT pCompound) {
        super.addAdditionalSaveData(pCompound);
        this.writeDigState(pCompound);
    }

    @Nullable
    @Override
    public ILivingEntityData finalizeSpawn(IServerWorld pLevel, DifficultyInstance pDifficulty, SpawnReason pReason, @Nullable ILivingEntityData pSpawnData, @Nullable CompoundNBT pDataTag) {
        if (pReason == SpawnReason.NATURAL) {
            this.setBuried();
        }

        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    /***
     * Methods for {@link IAnimatable}
     */

    @Override
    public void registerControllers(AnimationData animationData) {
        animationData.addAnimationController(new AnimationController<>(this, "controller", 5, this::animationPredicate));
    }

    protected <E extends Dune> PlayState animationPredicate(final AnimationEvent<E> event){
        if(this.isBuried()){
            event.getController().setAnimation(BURIED_ANIM);
        } else if(this.isDigging()){
            event.getController().setAnimation(SPAWN_ANIM);
            if(event.getController().getCurrentAnimation() != null)
                this.addDigParticles(event.getController().getAnimationState(), event.getAnimationTick(), event.getController().getCurrentAnimation().animationLength);
        } else if(this.isEmerging()){
            event.getController().setAnimation(SPAWN_ANIM);
            if(event.getController().getCurrentAnimation() != null)
                this.addDigParticles(event.getController().getAnimationState(), event.getAnimationTick(), event.getController().getCurrentAnimation().animationLength);
        }
        // We are surfaced, so check surfaced animations
        else if(this.isAttackAnimationInProgress()){
            event.getController().setAnimation(MELEE_ANIM);
        } else if(event.isMoving()){
            event.getController().setAnimation(WALK_ANIM);
        } else{
            event.getController().setAnimation(IDLE_ANIM);
        }

        return PlayState.CONTINUE;
    }

    private void addDigParticles(AnimationState animationState, double animationTick, double animationLength) {
        if (animationState == AnimationState.Running && animationTick / animationLength < 0.9D) {
            Random random = this.getRandom();
            BlockState blockStateOn = this.getBlockStateOn();
            if (blockStateOn.getRenderShape() != BlockRenderType.INVISIBLE) {
                for (int i = 0; i < 30; i++) {
                    double x = this.getX() + (double) MathHelper.nextFloat(random, -0.7F, 0.7F);
                    double y = this.getY();
                    double z = this.getZ() + (double)MathHelper.nextFloat(random, -0.7F, 0.7F);
                    this.level.addParticle(new BlockParticleData(ParticleTypes.BLOCK, blockStateOn), x, y, z, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }


    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    /***
     * Methods for {@link AnimatableMeleeAttack}
     */

    @Override
    public int getAttackAnimationTick() {
        return this.attackAnimationTick;
    }

    @Override
    public void setAttackAnimationTick(int attackAnimationTick) {
        this.attackAnimationTick = attackAnimationTick;
    }

    @Override
    public int getAttackAnimationLength() {
        return ATTACK_ANIMATION_LENGTH;
    }

    @Override
    public int getAttackAnimationActionPoint() {
        return ATTACK_ANIMATION_ACTION_POINT;
    }

    /***
     * Methods for {@link IEntityAdditionalSpawnData}
     */

    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        buffer.writeBoolean(this.isBuried());
    }

    @Override
    public void readSpawnData(PacketBuffer additionalData) {
        if(additionalData.readBoolean()){
            this.setBuried();
        }
    }

    /***
     * Methods for {@link Digger}
     */

    @Override
    public SoundEvent getEmergeSound() {
        return SoundEvents.HUSK_AMBIENT;
    }

    @Override
    public SoundEvent getDigSound() {
        return SoundEvents.HUSK_AMBIENT;
    }

    @Override
    public SoundEvent getAgitatedSound() {
        return SoundEvents.HUSK_HURT;
    }

    @Override
    public DigState getDigState() {
        return this.entityData.get(DATA_DIG_STATE);
    }

    @Override
    public void setDigState(DigState digState) {
        this.entityData.set(DATA_DIG_STATE, digState);
    }

    @Override
    public boolean wantsToDig() {
        return false;
        //return this.getTarget() == null && this.level.isDay() && this.level.canSeeSky(this.blockPosition());
    }

    @Override
    public boolean wantsToEmerge() {
        return this.getTarget() != null;
    }

}
