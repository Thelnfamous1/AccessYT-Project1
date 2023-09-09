package me.infamous.accessmod.common.entity.lurker;

import me.infamous.accessmod.common.entity.ai.AttackTurtleEggGoal;
import me.infamous.accessmod.common.entity.ai.ConditionalGoal;
import me.infamous.accessmod.common.entity.ai.attack.AnimatableMeleeAttack;
import me.infamous.accessmod.common.entity.ai.attack.AnimatableMeleeAttackGoal;
import me.infamous.accessmod.common.entity.ai.disguise.AnimatableDisguise;
import me.infamous.accessmod.common.entity.dune.Dune;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

public class Lurker extends MonsterEntity implements IAnimatable, AnimatableMeleeAttack, AnimatableDisguise {
    private static final int ATTACK_ANIMATION_LENGTH = 13;
    private static final int ATTACK_ANIMATION_ACTION_POINT = 2;
    private final AnimationFactory factory = GeckoLibUtil.createFactory(this);
    protected static final AnimationBuilder SLEEP_ANIM = new AnimationBuilder().addAnimation("sleep", true);
    protected static final AnimationBuilder RUN_ANIM = new AnimationBuilder().addAnimation("run", true);
    protected static final AnimationBuilder ATTACK_ANIM = new AnimationBuilder().addAnimation("attack", false);
    protected static final AnimationBuilder HOOK_ANIM = new AnimationBuilder().addAnimation("attack_hook", false);
    protected static final AnimationBuilder IDLE_ANIM = new AnimationBuilder().addAnimation("idle", true);
    protected static final AnimationBuilder HIDE_ANIM = new AnimationBuilder().addAnimation("turning_below", false);
    protected static final AnimationBuilder RISE_ANIM = new AnimationBuilder().addAnimation("turning_jump", false);
    private int attackAnimationTick;

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
        this.goalSelector.addGoal(5, new ConditionalGoal<>(Lurker::canUseMelee, this, new AnimatableMeleeAttackGoal<>(this, 1.0D, false), true));
        this.goalSelector.addGoal(6, new AttackTurtleEggGoal(this, 1.0D, 3));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomWalkingGoal(this, 1.0D));
    }

    private boolean canUseMelee(){
        return !AnimatableDisguise.cast(this).isDisguised();
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
    }

    @Override
    public void onSyncedDataUpdated(DataParameter<?> pKey) {
        super.onSyncedDataUpdated(pKey);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pPos, BlockState pBlock) {
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
    public void baseTick() {
        super.baseTick();
        this.tickAnimations();
    }

    private void tickAnimations() {
        if(this.attackAnimationTick > 0){
            this.attackAnimationTick--;
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
        if(this.isAttackAnimationInProgress()){
            event.getController().setAnimation(ATTACK_ANIM);
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
    public int getAttackAnimationLength() {
        return ATTACK_ANIMATION_LENGTH;
    }

    @Override
    public int getAttackAnimationActionPoint() {
        return ATTACK_ANIMATION_ACTION_POINT;
    }
}
