package me.infamous.accessmod.common.entity.dune;

import me.infamous.accessmod.common.entity.ai.AttackTurtleEggGoal;
import me.infamous.accessmod.common.entity.ai.ConditionalGoal;
import me.infamous.accessmod.common.entity.ai.attack.AnimatableMeleeAttack;
import me.infamous.accessmod.common.entity.ai.attack.AnimatableMeleeAttackGoal;
import me.infamous.accessmod.common.entity.ai.digger.*;
import me.infamous.accessmod.common.entity.ai.magic.AnimatableMagic;
import me.infamous.accessmod.common.entity.ai.magic.MagicCooldownTracker;
import me.infamous.accessmod.common.entity.ai.magic.UsingMagicGoal;
import me.infamous.accessmod.common.registry.AccessModDataSerializers;
import me.infamous.accessmod.common.registry.AccessModEffects;
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
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
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

public class Dune extends MonsterEntity implements IAnimatable, AnimatableMeleeAttack<DuneAttackType>, IEntityAdditionalSpawnData, Digger, AnimatableMagic<DuneMagicType> {
    public static final int DIG_ANIMATION_LENGTH = 38;
    public static final int BASE_WRATH_DURATION = 100;
    public static final int PREFERRED_RANGED_DISTANCE = 8;
    private final AnimationFactory factory = GeckoLibUtil.createFactory(this);
    protected static final AnimationBuilder IDLE_ANIM = new AnimationBuilder().addAnimation("idle", true);
    protected static final AnimationBuilder WALK_ANIM = new AnimationBuilder().addAnimation("walk", true);
    protected static final AnimationBuilder DRAG_ANIM = new AnimationBuilder().addAnimation("dragging", false);
    protected static final AnimationBuilder RANGED_ANIM = new AnimationBuilder().addAnimation("attack_distance", false);
    protected static final AnimationBuilder SPAWN_ANIM = new AnimationBuilder().addAnimation("spawn", false);
    protected static final AnimationBuilder MELEE_ANIM = new AnimationBuilder().addAnimation("attack", false);
    protected static final AnimationBuilder BURIED_ANIM = new AnimationBuilder().addAnimation("buried", ILoopType.EDefaultLoopTypes.HOLD_ON_LAST_FRAME);

    private static final DataParameter<Digger.DigState> DATA_DIG_STATE = EntityDataManager.defineId(Dune.class, AccessModDataSerializers.getSerializer(AccessModDataSerializers.DIG_STATE));
    private static final DataParameter<Byte> DATA_ATTACK_TYPE_ID = EntityDataManager.defineId(Dune.class, DataSerializers.BYTE);
    private static final DataParameter<Byte> DATA_MAGIC_TYPE_ID = EntityDataManager.defineId(Dune.class, DataSerializers.BYTE);

    private int attackAnimationTick;
    private int magicUseTicks;
    private DuneMagicType currentMagicType;
    private DuneAttackType currentAttackType;
    private final MagicCooldownTracker<Dune, DuneMagicType> magicCooldowns = new MagicCooldownTracker<>(this);

    public Dune(EntityType<? extends Dune> entityType, World world) {
        super(entityType, world);
        this.xpReward = 20;
    }

    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return MonsterEntity.createMonsterAttributes()
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35F)
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
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
        this.goalSelector.addGoal(2, new UsingMagicGoal<>(this));
        this.goalSelector.addGoal(3, new ConditionalGoal<>(Dune::canUseDrag, this, new DuneDragGoal(this), true));
        this.goalSelector.addGoal(4, new ConditionalGoal<>(Dune::canUseRanged, this, new DuneRangedGoal(this), false));
        this.goalSelector.addGoal(5, new ConditionalGoal<>(Dune::canUseMelee, this, new AnimatableMeleeAttackGoal<>(this, DuneAttackType.SWIPE, 1.0D, false), true));
        this.goalSelector.addGoal(6, new AttackTurtleEggGoal(this, 1.0D, 3));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomWalkingGoal(this, 1.0D));
    }

    private boolean canUseDrag(){
        return !this.isAttackAnimationInProgress();
    }

    private boolean canUseMelee(){
        return !this.isMagicAnimationInProgress()
                && this.getTarget() != null
                && (this.closerThan(this.getTarget(), PREFERRED_RANGED_DISTANCE)
                    || this.getMagicCooldowns().isOnCooldown(DuneMagicType.RANGED));
    }

    private boolean canUseRanged(){
        return !this.isAttackAnimationInProgress()
                && this.getTarget() != null
                && !this.closerThan(this.getTarget(), PREFERRED_RANGED_DISTANCE)
                && this.getSensing().canSee(this.getTarget());
    }

    private void addLookGoals() {
        this.goalSelector.addGoal(8, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(8, new LookRandomlyGoal(this));
    }

    protected void addTargetGoals() {
        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this, Dune.class)).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolemEntity.class, true));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, TurtleEntity.class, 10, true, false, TurtleEntity.BABY_ON_LAND_SELECTOR));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_DIG_STATE, DigState.SURFACED);
        this.entityData.define(DATA_ATTACK_TYPE_ID, (byte)0);
        this.entityData.define(DATA_MAGIC_TYPE_ID, (byte)0);
    }

    @Override
    public void onSyncedDataUpdated(DataParameter<?> pKey) {
        super.onSyncedDataUpdated(pKey);
        if (DATA_DIG_STATE.equals(pKey)) {
            this.refreshDimensions();
        } else if(DATA_ATTACK_TYPE_ID.equals(pKey)){
            this.startAttackAnimation(this.getCurrentAttackType());
        } else if(DATA_MAGIC_TYPE_ID.equals(pKey)){
            this.startMagicAnimation(this.getCurrentMagicType());
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
        if(!this.level.isClientSide && this.attackAnimationTick <= 0){
            this.resetAttackType();
        }
    }

    private void tickAnimations() {
        if(this.attackAnimationTick > 0){
            this.attackAnimationTick--;
        }
        if(this.magicUseTicks > 0){
            this.magicUseTicks--;
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.magicCooldowns.tick();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
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

    @Override
    public boolean doHurtTarget(Entity pEntity) {
        boolean hurtTarget = super.doHurtTarget(pEntity);
        if (hurtTarget && this.getMainHandItem().isEmpty() && pEntity instanceof LivingEntity) {
            addDuneWrathEffect(this, (LivingEntity) pEntity);
        }

        return hurtTarget;
    }

    static void addDuneWrathEffect(LivingEntity attacker, LivingEntity target) {
        float effectiveDifficulty = attacker.level.getCurrentDifficultyAt(attacker.blockPosition()).getEffectiveDifficulty();
        target.addEffect(new EffectInstance(AccessModEffects.DUNE_WRATH.get(), BASE_WRATH_DURATION * (int)effectiveDifficulty));
    }

    /***
     * Methods for {@link IAnimatable}
     */

    @Override
    public void registerControllers(AnimationData animationData) {
        animationData.addAnimationController(new AnimationController<>(this, "controller", 0, this::animationPredicate));
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
        else if(this.isMagicAnimationInProgress()){
            switch (this.getCurrentMagicType()){
                case RANGED:
                    event.getController().setAnimation(RANGED_ANIM);
                    break;
                case DRAG:
                    event.getController().setAnimation(DRAG_ANIM);
                    break;
                default: // do nothing
                    break;
            }
        } else if(this.isAttackAnimationInProgress()){
            if (this.getCurrentAttackType() == DuneAttackType.SWIPE) {
                event.getController().setAnimation(MELEE_ANIM);
            }
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
    public DuneAttackType getCurrentAttackType() {
        return !this.level.isClientSide ? this.currentAttackType : DuneAttackType.byId(this.entityData.get(DATA_ATTACK_TYPE_ID));
    }

    @Override
    public void setCurrentAttackType(DuneAttackType attackType) {
        this.currentAttackType = attackType;
        this.entityData.set(DATA_ATTACK_TYPE_ID, (byte)attackType.getId());
    }

    @Override
    public DuneAttackType getDefaultAttackType() {
        return DuneAttackType.NONE;
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

    /**
     * {@link AnimatableMagic} methods
     */

    @Override
    public int getMagicAnimationTick() {
        return this.magicUseTicks;
    }

    @Override
    public void setMagicAnimationTick(int magicAnimationTick) {
        this.magicUseTicks = magicAnimationTick;
    }

    @Override
    public SoundEvent getUseMagicSound() {
        return SoundEvents.EVOKER_CAST_SPELL;
    }

    @Override
    public DuneMagicType getCurrentMagicType() {
        return !this.level.isClientSide ? this.currentMagicType : DuneMagicType.byId(this.entityData.get(DATA_MAGIC_TYPE_ID));
    }

    @Override
    public void setCurrentMagicType(DuneMagicType magicType) {
        this.currentMagicType = magicType;
        this.entityData.set(DATA_MAGIC_TYPE_ID, (byte)magicType.getId());
    }

    @Override
    public DuneMagicType getDefaultMagicType() {
        return DuneMagicType.NONE;
    }

    @Override
    public MagicCooldownTracker<Dune, DuneMagicType> getMagicCooldowns() {
        return this.magicCooldowns;
    }
}
