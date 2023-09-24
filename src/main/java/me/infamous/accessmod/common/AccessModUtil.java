package me.infamous.accessmod.common;

import me.infamous.accessmod.AccessMod;
import me.infamous.accessmod.common.entity.ai.digger.Digger;
import me.infamous.accessmod.common.entity.dune.Dune;
import me.infamous.accessmod.common.registry.AccessModEntityTypes;
import me.infamous.accessmod.common.registry.AccessModPOITypes;
import me.infamous.accessmod.mixin.EntityAccessor;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.brain.BrainUtil;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.IParticleData;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ITag;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AccessModUtil {
    public static final String FROM_DESERT_WELL_TAG = "FromDesertWell";


    public static final ITag.INamedTag<EntityType<?>> LURKER_DISGUISES_AS = EntityTypeTags.createOptional(new ResourceLocation(AccessMod.MODID, "lurker_disguises_as"));
    public static final ITag.INamedTag<EntityType<?>> SCYTHE_CAN_HARVEST_SOUL = EntityTypeTags.createOptional(new ResourceLocation(AccessMod.MODID, "scythe_can_harvest_soul"));
    public static final ITag.INamedTag<EntityType<?>> SCYTHE_CAN_HARVEST_SOUL_LIMITED = EntityTypeTags.createOptional(new ResourceLocation(AccessMod.MODID, "scythe_can_harvest_soul_limited"));
    public static final float TO_RADIANS = ((float) Math.PI / 180F);

    public static void handleDesertWellFillBottle(ItemStack stack, World world, PlayerEntity player) {
        if(world instanceof ServerWorld){
            ServerWorld serverWorld = (ServerWorld) world;
            BlockRayTraceResult playerPOVHitResult = AccessModUtil.getPlayerPOVHitResult(serverWorld, player, RayTraceContext.FluidMode.SOURCE_ONLY);
            if(playerPOVHitResult.getType() == RayTraceResult.Type.BLOCK){
                BlockPos blockPos = playerPOVHitResult.getBlockPos();
                if(serverWorld.getPoiManager().existsAtPosition(AccessModPOITypes.DESERT_WELL.get(), blockPos)){
                    stack.getOrCreateTag().putBoolean(FROM_DESERT_WELL_TAG, true);
                }
            }
        }
    }

    public static boolean isFromDesertWell(ItemStack bottleStack){
        return bottleStack.hasTag() && bottleStack.getTag().getBoolean(FROM_DESERT_WELL_TAG);
    }

    public static int secondsToTicks(double seconds) {
        return (int) Math.ceil(seconds * 20);
    }

    public static BlockRayTraceResult getPlayerPOVHitResult(World pLevel, PlayerEntity pPlayer, RayTraceContext.FluidMode pFluidMode) {
        float f = pPlayer.xRot;
        float f1 = pPlayer.yRot;
        Vector3d vector3d = pPlayer.getEyePosition(1.0F);
        float f2 = MathHelper.cos(-f1 * TO_RADIANS - (float)Math.PI);
        float f3 = MathHelper.sin(-f1 * TO_RADIANS - (float)Math.PI);
        float f4 = -MathHelper.cos(-f * TO_RADIANS);
        float f5 = MathHelper.sin(-f * TO_RADIANS);
        float f6 = f3 * f4;
        float f7 = f2 * f4;
        double d0 = pPlayer.getAttribute(net.minecraftforge.common.ForgeMod.REACH_DISTANCE.get()).getValue();
        Vector3d vector3d1 = vector3d.add((double)f6 * d0, (double)f5 * d0, (double)f7 * d0);
        return pLevel.clip(new RayTraceContext(vector3d, vector3d1, RayTraceContext.BlockMode.OUTLINE, pFluidMode, pPlayer));
    }

    public static void summonDune(LivingEntity summoner, ServerWorld serverWorld){
        Dune dune = AccessModEntityTypes.DUNE.get().create(serverWorld);
        BlockPos spawnPos = summoner.blockPosition();
        dune.moveTo(spawnPos, 0.0F, 0.0F);
        for(int i = 0; i < 16; ++i) {
            double xRandom = summoner.getX() + (summoner.getRandom().nextDouble() - 0.5D) * 16.0D;
            double yRandom = MathHelper.clamp(summoner.getY() + (double)(summoner.getRandom().nextInt(16) - 8), 0.0D, serverWorld.getHeight() - 1);
            double zRandom = summoner.getZ() + (summoner.getRandom().nextDouble() - 0.5D) * 16.0D;
            if(dune.randomTeleport(xRandom, yRandom, zRandom, false)){
                spawnPos = new BlockPos(xRandom, yRandom, zRandom);
                break;
            }
        }
        dune.setDigState(Digger.DigState.BURIED);
        dune.finalizeSpawn(serverWorld, serverWorld.getCurrentDifficultyAt(spawnPos), SpawnReason.MOB_SUMMONED, null, null);
        serverWorld.addFreshEntityWithPassengers(dune);
    }

    public static void sendParticle(ServerWorld world, IParticleData particleType, Entity entity){
        Vector3d deltaMovement = entity.getDeltaMovement();
        EntityAccessor accessor = (EntityAccessor) entity;
        world.sendParticles(particleType,
                entity.getX() + (accessor.accessmod_getRandom().nextDouble() - 0.5D) * (double)entity.getBbWidth(),
                entity.getY() + 0.1D,
                entity.getZ() + (accessor.accessmod_getRandom().nextDouble() - 0.5D) * (double)entity.getBbWidth(),
                0,
                deltaMovement.x * -0.2D,
                0.1D,
                deltaMovement.z * -0.2D,
                1.0D);
    }

    public static Vector3d getRandomNearbyPos(CreatureEntity creature) {
        Vector3d vector3d = RandomPositionGenerator.getLandPos(creature, 4, 2);
        return vector3d == null ? creature.position() : vector3d;
    }

    public static void throwItemsTowardRandomPos(CreatureEntity mob, List<ItemStack> items) {
        throwItemsTowardPos(mob, items, getRandomNearbyPos(mob));
    }

    public static void throwItemsTowardPos(LivingEntity mob, List<ItemStack> items, Vector3d throwTargetPos) {
        if (!items.isEmpty()) {
            mob.swing(Hand.OFF_HAND);

            for(ItemStack item : items) {
                BrainUtil.throwItem(mob, item, throwTargetPos.add(0.0D, 1.0D, 0.0D));
            }
        }
    }

    public static ItemStack removeOneItemFromItemEntity(ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getItem();
        ItemStack split = stack.split(1);
        if (stack.isEmpty()) {
            itemEntity.remove();
        } else {
            itemEntity.setItem(stack);
        }

        return split;
    }

    public static void dropItems(LivingEntity mob, ItemStack item){
        dropItems(mob, Collections.singletonList(item));
    }

    public static void dropItems(LivingEntity mob, Collection<ItemStack> items) {
        if (!items.isEmpty()) {
            for(ItemStack item : items){
                double targetYPos = mob.getEyeY() - (double)0.3F;
                ItemEntity drop = new ItemEntity(mob.level, mob.getX(), targetYPos, mob.getZ(), item);
                drop.setPickUpDelay(40);
                drop.setThrower(mob.getUUID());
                float dropSpeed = 0.3F;
                float f1 = mob.getRandom().nextFloat() * ((float)Math.PI * 2F);
                float f2 = 0.02F * mob.getRandom().nextFloat();
                drop.setDeltaMovement(
                        dropSpeed * -MathHelper.sin(mob.yRot * TO_RADIANS) * MathHelper.cos(mob.xRot * TO_RADIANS) + MathHelper.cos(f1) * f2,
                        dropSpeed * MathHelper.sin(mob.xRot * TO_RADIANS) * 1.5F,
                        dropSpeed * MathHelper.cos(mob.yRot * TO_RADIANS) * MathHelper.cos(mob.xRot * TO_RADIANS) + MathHelper.sin(f1) * f2);
                mob.level.addFreshEntity(drop);
            }
        }
    }
}
