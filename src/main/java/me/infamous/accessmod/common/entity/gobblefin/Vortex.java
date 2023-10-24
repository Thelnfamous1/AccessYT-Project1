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
import net.minecraft.fluid.FluidState;
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
import net.minecraft.world.Explosion;
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

   public void tick() {
      Vector3d origin = this.getPosition();
      float diameter = this.radius * 2.0F;
      int xMin = MathHelper.floor(origin.x - (double)diameter - 1.0D);
      int xMax = MathHelper.floor(origin.x + (double)diameter + 1.0D);
      int yMin = MathHelper.floor(origin.y - (double)diameter - 1.0D);
      int yMax = MathHelper.floor(origin.y + (double)diameter + 1.0D);
      int zMin = MathHelper.floor(origin.z - (double)diameter - 1.0D);
      int zMax = MathHelper.floor(origin.z + (double)diameter + 1.0D);
      this.hitEntities = this.level.getEntities(this.source, new AxisAlignedBB(xMin, yMin, zMin, xMax, yMax, zMax), hitEntity -> {
         Entity vortexOwner = this.getVortexOwner();
         return EntityPredicates.NO_SPECTATORS.test(hitEntity)
                 && (vortexOwner == null || !vortexOwner.getPassengers().contains(hitEntity ) && vortexOwner.getVehicle() != hitEntity);
      });
      //net.minecraftforge.event.ForgeEventFactory.onExplosionDetonate(this.level, this, hitEntities, diameter);

      for(int entIdx = 0; entIdx < hitEntities.size(); ++entIdx) {
         Entity hitEntity = hitEntities.get(entIdx);
         if (!hitEntity.ignoreExplosion()) {
            double distFactor = MathHelper.sqrt(hitEntity.distanceToSqr(origin)) / diameter;
            if (distFactor <= 1.0D) {
               double xDist = origin.x - hitEntity.getX();
               double yDist = origin.y - hitEntity.getEyeY();
               double zDist = origin.z - hitEntity.getZ();
               double dist = MathHelper.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);
               if (dist != 0.0D) {
                  xDist = xDist / dist;
                  yDist = yDist / dist;
                  zDist = zDist / dist;
                  double seenPercent = Explosion.getSeenPercent(origin, hitEntity);
                  double damageFactor = (1.0D - distFactor) * seenPercent;
                  //hitEntity.hurt(this.getDamageSource(), (float)((int)((damageFactor * damageFactor + damageFactor) / 2.0D * 7.0D * (double)diameter + 1.0D)));
                  double knockbackFactor = damageFactor;
                  if (hitEntity instanceof LivingEntity) {
                     //knockbackFactor = ProtectionEnchantment.getExplosionKnockbackAfterDampener((LivingEntity)hitEntity, damageFactor);
                  }

                  //hitEntity.setDeltaMovement(hitEntity.getDeltaMovement().add(xDist * knockbackFactor, yDist * knockbackFactor, zDist * knockbackFactor));
                  hitEntity.push(xDist * 0.025D, yDist * 0.025D, zDist * 0.025D); // we want to move at a speed of one block per 40 ticks
                  if (hitEntity instanceof PlayerEntity) {
                     PlayerEntity hitPlayer = (PlayerEntity)hitEntity;
                     if (!hitPlayer.isSpectator() && (!hitPlayer.isCreative() || !hitPlayer.abilities.flying)) {
                        this.hitPlayers.put(hitPlayer, new Vector3d(xDist * damageFactor, yDist * damageFactor, zDist * damageFactor));
                     }
                  }
               }
            }
         }
      }
   }

   public void initVortex() {
      this.blowUpBlocks();
      //this.blowUpEntities();
   }

   private void blowUpBlocks() {
      Vector3d origin = this.getPosition();
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
                  float f = this.radius * (0.7F + this.level.random.nextFloat() * 0.6F);
                  double targetX = origin.x;
                  double targetY = origin.y;
                  double targetZ = origin.z;

                  for(float f1 = 0.3F; f > 0.0F; f -= 0.22500001F) {
                     BlockPos blockpos = new BlockPos(targetX, targetY, targetZ);
                     BlockState blockstate = this.level.getBlockState(blockpos);
                     FluidState fluidstate = this.level.getFluidState(blockpos);
                     Optional<Float> optional = Optional.empty(); //this.damageCalculator.getBlockExplosionResistance(this, this.level, blockpos, blockstate, fluidstate);
                     if (optional.isPresent()) {
                        f -= (optional.get() + 0.3F) * 0.3F;
                     }

                     if (f > 0.0F /*&& this.damageCalculator.shouldBlockExplode(this, this.level, blockpos, blockstate, f)*/) {
                        blocksToBlow.add(blockpos);
                     }

                     targetX += xDist * (double)0.3F;
                     targetY += yDist * (double)0.3F;
                     targetZ += zDist * (double)0.3F;
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
               //this.level.getProfiler().push("explosion_blocks");
               if (/*stateToBlow.canDropFromExplosion(this.level, blockToBlow, this) &&*/ this.level instanceof ServerWorld) {
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

               //stateToBlow.onBlockExploded(this.level, blockToBlow, this);
               // can't use above, so need to recreate the explosion-independent logic in the base impl here
               this.level.setBlock(blockToBlow, Blocks.WATER.defaultBlockState(), 3);

               //this.level.getProfiler().pop();
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