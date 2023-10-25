package me.infamous.accessmod.common.entity.gobblefin;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.infamous.accessmod.AccessMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EntityPredicates;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.EntityExplosionContext;
import net.minecraft.world.ExplosionContext;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.*;

public class Vortex {
   private static final ExplosionContext EXPLOSION_DAMAGE_CALCULATOR = new ExplosionContext();
   private static final int CHUNK_SIZE = 16;
   private final Vortex.Mode blockInteraction;
   private final World level;
   @Nullable
   private final Entity source;
   private final float radius;
   private final DamageSource damageSource;
   private final ExplosionContext damageCalculator;
   private final List<BlockPos> blocksToBlow = Lists.newArrayList();
   private final Map<PlayerEntity, Vector3d> hitPlayers = Maps.newHashMap();
   private Vector3d position;
   private List<Entity> hitEntities = Lists.newArrayList();

   public Vortex(World pLevel, @Nullable Entity pSource, Vector3d position, float pRadius, List<BlockPos> pPositions) {
      this(pLevel, pSource, position, pRadius, Vortex.Mode.DESTROY, pPositions);
   }

   public Vortex(World pLevel, @Nullable Entity pSource, Vector3d position, float pRadius, Vortex.Mode pBlockInteraction, List<BlockPos> pPositions) {
      this(pLevel, pSource, position, pRadius, pBlockInteraction);
      this.blocksToBlow.addAll(pPositions);
   }

   public Vortex(World pLevel, @Nullable Entity pSource, Vector3d position, float pRadius, Vortex.Mode pBlockInteraction) {
      this(pLevel, pSource, null, null, position, pRadius, pBlockInteraction);
   }

   public Vortex(World pLevel, @Nullable Entity pSource, @Nullable DamageSource pDamageSource, @Nullable ExplosionContext pDamageCalculator, Vector3d position, float pRadius, Vortex.Mode pBlockInteraction) {
      this.level = pLevel;
      this.source = pSource;
      this.radius = pRadius;
      this.blockInteraction = pBlockInteraction;
      this.damageSource = pDamageSource == null ? vortex(this) : pDamageSource;
      this.damageCalculator = pDamageCalculator == null ? this.makeDamageCalculator(pSource) : pDamageCalculator;
      this.position = position;
   }

   public static DamageSource vortex(@Nullable Vortex vortex) {
      return vortex(vortex != null ? vortex.getVortexOwnerMob() : null);
   }

   public static DamageSource vortex(@Nullable LivingEntity trueSource) {
      return trueSource != null ? (new EntityDamageSource(AccessMod.MODID + ".vortex.player", trueSource)).setScalesWithDifficulty() : (new DamageSource(AccessMod.MODID + ".vortex")).setScalesWithDifficulty();
   }

   private ExplosionContext makeDamageCalculator(@Nullable Entity pEntity) {
      return pEntity == null ? EXPLOSION_DAMAGE_CALCULATOR : new EntityExplosionContext(pEntity);
   }

   /*
      Need to suck in objects from up to `radius` blocks away in the target direction to the origin
    */
   public void tick() {
      Vector3d sourcePos = this.getPosition();
      Vector3d lookAngle = this.getVortexOwner() != null ? this.getVortexOwner().getLookAngle() : Vector3d.ZERO;
      Vector3d searchCenterPos = sourcePos.add(lookAngle.scale(this.radius));

      int xMin = MathHelper.floor(searchCenterPos.x - this.radius - 1.0D);
      int xMax = MathHelper.floor(searchCenterPos.x + this.radius + 1.0D);
      int yMin = MathHelper.floor(searchCenterPos.y - this.radius - 1.0D);
      int yMax = MathHelper.floor(searchCenterPos.y + this.radius + 1.0D);
      int zMin = MathHelper.floor(searchCenterPos.z - this.radius - 1.0D);
      int zMax = MathHelper.floor(searchCenterPos.z + this.radius + 1.0D);
      AxisAlignedBB searchBox = new AxisAlignedBB(xMin, yMin, zMin, xMax, yMax, zMax);
      this.hitEntities = this.level.getEntities(this.source, searchBox, hitEntity -> {
         Entity vortexOwner = this.getVortexOwner();
         return EntityPredicates.NO_SPECTATORS.test(hitEntity)
                 && (vortexOwner == null || !vortexOwner.getPassengers().contains(hitEntity) && vortexOwner.getVehicle() != hitEntity);
      });

      for (Entity hitEntity : this.hitEntities) {
         if (!hitEntity.ignoreExplosion()) {
            double xDist = sourcePos.x - hitEntity.getX();
            double yDist = sourcePos.y - hitEntity.getEyeY();
            double zDist = sourcePos.z - hitEntity.getZ();
            double dist = MathHelper.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);
            if (dist != 0.0D) {
               xDist = xDist / dist;
               yDist = yDist / dist;
               zDist = zDist / dist;

               double velocityScale = 0.025D; // we want to move the target at a speed of one block per 40 ticks
               hitEntity.push(xDist * velocityScale, yDist * velocityScale, zDist * velocityScale);
               if (hitEntity instanceof PlayerEntity) {
                  PlayerEntity hitPlayer = (PlayerEntity) hitEntity;
                  if (!hitPlayer.isSpectator() && (!hitPlayer.isCreative() || !hitPlayer.abilities.flying)) {
                     this.hitPlayers.put(hitPlayer, new Vector3d(xDist * velocityScale, yDist * velocityScale, zDist * velocityScale));
                  }
               }
            }
         }
      }
   }

   public void initVortex() {
      this.collectBlocksToDestroy();
   }

   private void collectBlocksToDestroy() {
      Vector3d sourcePos = this.getPosition();
      Vector3d lookAngle = this.getVortexOwner() != null ? this.getVortexOwner().getLookAngle() : Vector3d.ZERO;
      Vector3d searchCenterPos = sourcePos.add(lookAngle.scale(this.radius));
      Set<BlockPos> blocksToBlow = Sets.newHashSet();

      for(int x = 0; x < CHUNK_SIZE; ++x) {
         for(int y = 0; y < CHUNK_SIZE; ++y) {
            for(int z = 0; z < CHUNK_SIZE; ++z) {
               int maxIdx = CHUNK_SIZE - 1;
               if (x == 0 || x == maxIdx || y == 0 || y == maxIdx || z == 0 || z == maxIdx) {
                  double xDist = (float)x / (maxIdx) * 2.0F - 1.0F;
                  double yDist = (float)y / (maxIdx) * 2.0F - 1.0F;
                  double zDist = (float)z / (maxIdx) * 2.0F - 1.0F;
                  double dist = Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);
                  xDist = xDist / dist;
                  yDist = yDist / dist;
                  zDist = zDist / dist;
                  float power = this.radius * (0.7F + this.level.random.nextFloat() * 0.6F);
                  double targetX = searchCenterPos.x;
                  double targetY = searchCenterPos.y;
                  double targetZ = searchCenterPos.z;

                  for(float shiftScale = 0.3F; power > 0.0F; power -= 0.22500001F) {
                     BlockPos targetPos = new BlockPos(targetX, targetY, targetZ);
                     blocksToBlow.add(targetPos);

                     targetX += xDist * (double)shiftScale;
                     targetY += yDist * (double)shiftScale;
                     targetZ += zDist * (double)shiftScale;
                  }
               }
            }
         }
      }

      this.blocksToBlow.addAll(blocksToBlow);
   }

   public void finalizeVortex(boolean pSpawnParticles) {
      Vector3d currentPosition = this.getPosition();
      if (this.level.isClientSide) {
         //this.level.playLocalSound(currentPosition.x, currentPosition.y, currentPosition.z, SoundEvents.GENERIC_EXPLODE, SoundCategory.BLOCKS, 4.0F, (1.0F + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F) * 0.7F, false);
      }

      boolean destroyBlocks = this.blockInteraction != Vortex.Mode.NONE;
      if (pSpawnParticles) {
         if (!(this.radius < 2.0F) && destroyBlocks) {
            //this.level.addParticle(ParticleTypes.EXPLOSION_EMITTER, currentPosition.x, currentPosition.y, currentPosition.z, 1.0D, 0.0D, 0.0D);
         } else {
            //this.level.addParticle(ParticleTypes.EXPLOSION, currentPosition.x, currentPosition.y, currentPosition.z, 1.0D, 0.0D, 0.0D);
         }
      }

      if (destroyBlocks) {
         ObjectArrayList<Pair<ItemStack, BlockPos>> dropPositions = new ObjectArrayList<>();
         Collections.shuffle(this.blocksToBlow, this.level.random);

         for(BlockPos blockToBlow : this.blocksToBlow) {
            BlockState stateToBlow = this.level.getBlockState(blockToBlow);
            Block block = stateToBlow.getBlock();
            boolean isFluidBlock = block instanceof FlowingFluidBlock;
            if (!stateToBlow.isAir(this.level, blockToBlow) && !isFluidBlock) {
               BlockPos blockToBlowImm = blockToBlow.immutable();
               if (this.level instanceof ServerWorld) {
                  TileEntity tileToBlow = stateToBlow.hasTileEntity() ? this.level.getBlockEntity(blockToBlow) : null;
                  LootContext.Builder lootCtx = (new LootContext.Builder((ServerWorld)this.level))
                          .withRandom(this.level.random)
                          .withParameter(LootParameters.ORIGIN, Vector3d.atCenterOf(blockToBlow))
                          .withParameter(LootParameters.TOOL, ItemStack.EMPTY)
                          .withOptionalParameter(LootParameters.BLOCK_ENTITY, tileToBlow)
                          .withOptionalParameter(LootParameters.THIS_ENTITY, this.source);
                  if (this.blockInteraction == Vortex.Mode.DESTROY) {
                     lootCtx.withParameter(LootParameters.EXPLOSION_RADIUS, this.radius);
                  }

                  stateToBlow.getDrops(lootCtx).forEach((is) -> {
                     addBlockDrops(dropPositions, is, blockToBlowImm);
                  });
               }
               this.level.setBlock(blockToBlow, Blocks.AIR.defaultBlockState(), 3);
            }
         }

         for(Pair<ItemStack, BlockPos> dropPosition : dropPositions) {
            Block.popResource(this.level, dropPosition.getSecond(), dropPosition.getFirst());
         }
      }

   }

   private static void addBlockDrops(ObjectArrayList<Pair<ItemStack, BlockPos>> dropPositions, ItemStack pStack, BlockPos pPos) {
      int numDrops = dropPositions.size();

      for(int dropIdx = 0; dropIdx < numDrops; ++dropIdx) {
         Pair<ItemStack, BlockPos> dropPosition = dropPositions.get(dropIdx);
         ItemStack dropStack = dropPosition.getFirst();
         if (ItemEntity.areMergable(dropStack, pStack)) {
            ItemStack merge = ItemEntity.merge(dropStack, pStack, 16);
            dropPositions.set(dropIdx, Pair.of(merge, dropPosition.getSecond()));
            if (pStack.isEmpty()) {
               return;
            }
         }
      }

      dropPositions.add(Pair.of(pStack, pPos));
   }

   public DamageSource getDamageSource() {
      return this.damageSource;
   }

   public Map<PlayerEntity, Vector3d> getHitPlayers() {
      return this.hitPlayers;
   }

   public Optional<Vector3d> getPlayerHitVec(PlayerEntity player){
      return Optional.ofNullable(this.hitPlayers.get(player));
   }

   @Nullable
   public LivingEntity getVortexOwnerMob() {
      if (this.source == null) {
         return null;
      } else if (this.source instanceof LivingEntity) {
         return (LivingEntity)this.source;
      } else {
         if (this.source instanceof ProjectileEntity) {
            Entity entity = ((ProjectileEntity)this.source).getOwner();
            if (entity instanceof LivingEntity) {
               return (LivingEntity)entity;
            }
         }

         return null;
      }
   }

   public void clearToBlow() {
      this.blocksToBlow.clear();
   }

   public List<BlockPos> getBlocksToBlow() {
      return this.blocksToBlow;
   }

   @Nullable
   public Entity getVortexOwner() {
      return this.source;
   }

   public Vector3d getPosition() {
      return this.position;
   }

   public void setPosition(Vector3d position) {
      this.position = position;
   }

   public List<Entity> getHitEntities() {
      return this.hitEntities;
   }

   public enum Mode {
      NONE,
      BREAK,
      DESTROY
   }
}