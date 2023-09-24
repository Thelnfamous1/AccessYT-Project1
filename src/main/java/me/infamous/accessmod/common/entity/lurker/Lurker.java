package me.infamous.accessmod.common.entity.lurker;

import me.infamous.accessmod.common.AccessModUtil;
import me.infamous.accessmod.common.entity.ai.AttackTurtleEggGoal;
import me.infamous.accessmod.common.entity.ai.ConditionalGoal;
import me.infamous.accessmod.common.entity.ai.attack.AnimatableMeleeAttack;
import me.infamous.accessmod.common.entity.ai.attack.AnimatableMeleeAttackGoal;
import me.infamous.accessmod.common.entity.ai.disguise.AnimatableDisguise;
import me.infamous.accessmod.common.entity.ai.disguise.DisguisingGoal;
import me.infamous.accessmod.common.entity.ai.disguise.RevealingGoal;
import me.infamous.accessmod.common.entity.ai.disguise.StalkWhileDisguisedGoal;
import me.infamous.accessmod.common.entity.ai.sleep.FindShelterGoal;
import me.infamous.accessmod.common.entity.ai.sleep.SleepGoal;
import me.infamous.accessmod.common.entity.ai.sleep.SleepingMob;
import me.infamous.accessmod.common.registry.AccessModDataSerializers;
import me.infamous.accessmod.mixin.LivingEntityAccessor;
import me.infamous.accessmod.mixin.MobAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;

public class Lurker extends MonsterEntity implements IAnimatable, AnimatableMeleeAttack<LurkerAttackType>, AnimatableDisguise, SleepingMob {
    public static final int BASE_BLINDNESS_DURATION = 100;
    public static final int REVEAL_ANIMATION_LENGTH = AccessModUtil.secondsToTicks(1.333D);
    public static final int DISGUISE_ANIMATION_LENGTH = AccessModUtil.secondsToTicks(1.875D);
    private static final int CLOSE_ENOUGH_TO_REVEAL = 8;
    private final AnimationFactory factory = GeckoLibUtil.createFactory(this);
    protected static final AnimationBuilder SLEEP_ANIM = new AnimationBuilder().addAnimation("sleep", true);
    protected static final AnimationBuilder RUN_ANIM = new AnimationBuilder().addAnimation("run", true);
    protected static final AnimationBuilder ATTACK_ANIM = new AnimationBuilder().addAnimation("attack", false);
    protected static final AnimationBuilder HOOK_ANIM = new AnimationBuilder().addAnimation("attack_hook", false);
    protected static final AnimationBuilder IDLE_ANIM = new AnimationBuilder().addAnimation("idle", true);
    protected static final AnimationBuilder DISGUISE_ANIM = new AnimationBuilder().addAnimation("turning_below", false);
    protected static final AnimationBuilder REVEAL_ANIM = new AnimationBuilder().addAnimation("turning_jump", false);
    private static final DataParameter<Byte> DATA_ATTACK_TYPE_ID = EntityDataManager.defineId(Lurker.class, DataSerializers.BYTE);

    private static final DataParameter<AnimatableDisguise.DisguiseState> DATA_DISGUISE_STATE = EntityDataManager.defineId(Lurker.class, AccessModDataSerializers.getSerializer(AccessModDataSerializers.DISGUISE_STATE));

    private static final DataParameter<Boolean> DATA_SLEEPING = EntityDataManager.defineId(Lurker.class, DataSerializers.BOOLEAN);
    private int attackAnimationTick;
    private LurkerAttackType currentAttackType;

    public Lurker(EntityType<? extends Lurker> type, World world) {
        super(type, world);
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

    @Override
    protected void registerGoals() {
        this.addMoveGoals();
        this.addLookGoals();
        this.addTargetGoals();
    }

    protected void addMoveGoals() {
        this.goalSelector.addGoal(0, new RevealingGoal<>(this, REVEAL_ANIMATION_LENGTH));
        this.goalSelector.addGoal(0, new DisguisingGoal<>(this, DISGUISE_ANIMATION_LENGTH, Lurker::getRandomEntityType));
        this.goalSelector.addGoal(1, new StalkWhileDisguisedGoal<>(this, 1.0D, CLOSE_ENOUGH_TO_REVEAL));
        this.goalSelector.addGoal(2, new FindShelterGoal<>(this, 1.0D, 100, false));
        this.goalSelector.addGoal(3, new SleepGoal<>(this, 140));
        this.goalSelector.addGoal(5, new ConditionalGoal<>(Lurker::canUseMelee, this, new AnimatableMeleeAttackGoal<Lurker, LurkerAttackType>(this, Lurker::pickAttackType, 1.0D, false), true));
        this.goalSelector.addGoal(6, new AttackTurtleEggGoal(this, 1.0D, 3));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomWalkingGoal(this, 1.0D));
    }

    private static EntityType<?> getRandomEntityType(Lurker lurker){
        return AccessModUtil.LURKER_DISGUISES_AS.getRandomElement(lurker.getRandom());
    }

    private boolean canUseMelee(){
        return !AnimatableDisguise.entityDisguise(this).isDisguised();
    }

    private static LurkerAttackType pickAttackType(Lurker lurker){
        return lurker.level.random.nextInt(5) == 0 ? LurkerAttackType.HOOK : LurkerAttackType.SLASH;
    }

    private void addLookGoals() {
        this.goalSelector.addGoal(8, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(8, new LookRandomlyGoal(this));
    }

    protected void addTargetGoals() {
        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this, Lurker.class)).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolemEntity.class, true));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, TurtleEntity.class, 10, true, false, TurtleEntity.BABY_ON_LAND_SELECTOR));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ATTACK_TYPE_ID, (byte)0);
        this.entityData.define(DATA_DISGUISE_STATE, DisguiseState.REVEALED);
        this.entityData.define(DATA_SLEEPING, false);
    }

    @Override
    public void onSyncedDataUpdated(DataParameter<?> pKey) {
        super.onSyncedDataUpdated(pKey);
        if(DATA_ATTACK_TYPE_ID.equals(pKey)){
            this.startAttackAnimation(this.getCurrentAttackType());
        }
    }

    private Optional<SoundEvent> getSoundFromDisguise(Function<MobEntity, SoundEvent> soundGetter, @Nullable SoundEvent defaultSound){
        if(AnimatableDisguise.entityDisguise(this).isDisguised()){
            Entity disguiseEntity = AnimatableDisguise.entityDisguise(this).getDisguiseEntity();
            if(disguiseEntity instanceof MobEntity){
                return Optional.ofNullable(soundGetter.apply(((MobEntity) disguiseEntity)));
            }
        }
        return Optional.ofNullable(defaultSound);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.getSoundFromDisguise(m -> ((MobAccessor)m).callGetAmbientSound(), SoundEvents.ZOMBIE_AMBIENT).orElse(null);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return this.getSoundFromDisguise(m -> ((LivingEntityAccessor)m).callGetHurtSound(pDamageSource), SoundEvents.ZOMBIE_HURT).orElse(null);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.getSoundFromDisguise(m -> ((LivingEntityAccessor)m).callGetDeathSound(), SoundEvents.ZOMBIE_DEATH).orElse(null);
    }

    @Override
    protected void playStepSound(BlockPos pPos, BlockState pBlock) {
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
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isEffectiveAi()) {
            boolean inWater = this.isInWater();
            if (inWater || this.getTarget() != null) {
                this.setSleepingMob(false);
            }
        }
        if(!this.level.isClientSide){
            if(this.isDisguised() && this.tickCount % 50 == 0){
                AccessModUtil.sendParticle((ServerWorld) this.level, ParticleTypes.SMOKE, this, 5, 0.15D);
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity pEntity) {
        boolean hurtTarget = super.doHurtTarget(pEntity);
        if (hurtTarget && this.getMainHandItem().isEmpty() && pEntity instanceof LivingEntity
                && this.getCurrentAttackType() == LurkerAttackType.HOOK) {
            addBlindness(this, (LivingEntity) pEntity);
        }

        return hurtTarget;
    }

    static void addBlindness(LivingEntity attacker, LivingEntity target) {
        float effectiveDifficulty = attacker.level.getCurrentDifficultyAt(attacker.blockPosition()).getEffectiveDifficulty();
        target.addEffect(new EffectInstance(Effects.BLINDNESS, BASE_BLINDNESS_DURATION * (int)effectiveDifficulty));
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT pCompound) {
        super.addAdditionalSaveData(pCompound);
        this.writeSleepingNBT(pCompound);
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.readDisguiseState(pCompound);
        this.readSleepingNBT(pCompound);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.isSleepingMob() || this.isImmobile()) {
            this.jumping = false;
            this.xxa = 0.0F;
            this.zza = 0.0F;
        }
    }

    /**
     * Methods for {@link IAnimatable}
     */

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 0, this::animationPredicate));
    }

    private <E extends Lurker> PlayState animationPredicate(AnimationEvent<E> event) {
        if(this.isDisguised()){
            return PlayState.STOP;
        } else if(this.isDisguising()){
            event.getController().setAnimation(DISGUISE_ANIM);
        } else if(this.isRevealing()){
            event.getController().setAnimation(REVEAL_ANIM);
        }
        // We are revealed, so check revealed animations
        else if(this.isSleepingMob()){
            event.getController().setAnimation(SLEEP_ANIM);
        } else if(this.isAttackAnimationInProgress()){
            switch (this.getCurrentAttackType()){
                case SLASH:
                    event.getController().setAnimation(ATTACK_ANIM);
                    break;
                case HOOK:
                    event.getController().setAnimation(HOOK_ANIM);
                    break;
                default: // do nothing
                    break;
            }
        } else if(event.isMoving()){
            event.getController().setAnimation(RUN_ANIM);
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
    public LurkerAttackType getCurrentAttackType() {
        return !this.level.isClientSide ? this.currentAttackType : LurkerAttackType.byId(this.entityData.get(DATA_ATTACK_TYPE_ID));
    }

    @Override
    public void setCurrentAttackType(LurkerAttackType attackType) {
        this.currentAttackType = attackType;
        this.entityData.set(DATA_ATTACK_TYPE_ID, (byte)attackType.getId());
    }

    @Override
    public LurkerAttackType getDefaultAttackType() {
        return LurkerAttackType.NONE;
    }

    /**
     * {@link AnimatableDisguise} methods
     */

    @Override
    public SoundEvent getRevealSound() {
        return SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    public SoundEvent getDisguiseSound() {
        return SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    public DisguiseState getDisguiseState() {
        return this.entityData.get(DATA_DISGUISE_STATE);
    }

    @Override
    public void setDisguiseState(DisguiseState disguiseState) {
        this.entityData.set(DATA_DISGUISE_STATE, disguiseState);
    }

    @Override
    public boolean wantsToDisguise() {
        if(this.getLastHurtByMob() != null) return false;
        return this.getTarget() == null && this.level.canSeeSky(this.blockPosition());
    }

    @Override
    public boolean wantsToReveal() {
        if(this.getLastHurtByMob() != null) return true;
        if(this.getTarget() != null && this.closerThan(this.getTarget(), CLOSE_ENOUGH_TO_REVEAL)) return true;
        return !this.level.canSeeSky(this.blockPosition()) && this.wantsToSleep();
    }

    @Override
    public void setSleepingMob(boolean sleeping) {
        this.entityData.set(DATA_SLEEPING, sleeping);
    }

    @Override
    public boolean isSleepingMob() {
        return this.entityData.get(DATA_SLEEPING);
    }

    @Override
    public boolean wantsToSleep() {
        if (this.level.getBrightness(LightType.SKY, this.blockPosition()) >= 32) {
            return false;
        } else {
            int localBrightness = this.level.isThundering() ? this.level.getMaxLocalRawBrightness(this.blockPosition(), 10) : this.level.getMaxLocalRawBrightness(this.blockPosition());
            return localBrightness < 8;
        }
    }
}
