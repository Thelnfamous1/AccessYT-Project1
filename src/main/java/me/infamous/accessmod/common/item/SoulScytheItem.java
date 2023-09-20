package me.infamous.accessmod.common.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import me.infamous.accessmod.common.AccessModUtil;
import me.infamous.accessmod.common.capability.SoulsCapability;
import me.infamous.accessmod.common.capability.SoulsCapabilityProvider;
import me.infamous.accessmod.common.network.AccessModNetwork;
import me.infamous.accessmod.common.network.ClientboundSoulScythePacket;
import net.minecraft.block.BlockState;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;

public class SoulScytheItem extends Item {
    private static final String BASE_NBT_TAG = "base";
    private static final String CAPABILITY_NBT_TAG = "cap";
    private final Multimap<Attribute, AttributeModifier> defaultModifiers;

    private static final int SUMMON_DAMAGE = 3;

    public SoulScytheItem(Properties pProperties) {
        super(pProperties);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Scythe modifier", 5.5D, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Scythe modifier", -2.4F, AttributeModifier.Operation.ADDITION));
        this.defaultModifiers = builder.build();
    }

    public static boolean canHarvestSoul(LivingEntity entity){
        return entity.getType().is(AccessModUtil.SCYTHE_CAN_HARVEST_SOUL) || entity.getType().is(AccessModUtil.SCYTHE_CAN_HARVEST_SOUL_LIMITED);
    }

    public static LazyOptional<SoulsCapability> getSouls(ItemStack stack){
        return stack.getCapability(SoulsCapabilityProvider.INSTANCE);
    }

    @Override
    public boolean hurtEnemy(ItemStack pStack, LivingEntity pTarget, LivingEntity pAttacker) {
        pStack.hurtAndBreak(1, pAttacker, (e) -> {
            e.broadcastBreakEvent(EquipmentSlotType.MAINHAND);
        });
        return true;
    }

    @Override
    public boolean canAttackBlock(BlockState pState, World pLevel, BlockPos pPos, PlayerEntity pPlayer) {
        return !pPlayer.isCreative();
    }

    @Override
    public boolean mineBlock(ItemStack pStack, World pLevel, BlockState pState, BlockPos pPos, LivingEntity pEntityLiving) {
        if ((double)pState.getDestroySpeed(pLevel, pPos) != 0.0D) {
            pStack.hurtAndBreak(2, pEntityLiving, (e) -> e.broadcastBreakEvent(EquipmentSlotType.MAINHAND));
        }

        return true;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlotType pEquipmentSlot) {
        return pEquipmentSlot == EquipmentSlotType.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(pEquipmentSlot);
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }

    @Override
    public ActionResultType useOn(ItemUseContext pContext) {
        World world = pContext.getLevel();
        if (!(world instanceof ServerWorld)) {
            return ActionResultType.SUCCESS;
        } else {
            ItemStack itemstack = pContext.getItemInHand();
            BlockPos blockpos = pContext.getClickedPos();
            Direction direction = pContext.getClickedFace();
            BlockState blockstate = world.getBlockState(blockpos);
            PlayerEntity player = pContext.getPlayer();
            Hand hand = pContext.getHand();

            BlockPos spawnPos;
            if (blockstate.getCollisionShape(world, blockpos).isEmpty()) {
                spawnPos = blockpos;
            } else {
                spawnPos = blockpos.relative(direction);
            }

            if(player != null && spawnSummon(player, hand, world, itemstack, spawnPos)){
                AccessModUtil.sendParticle((ServerWorld) world, ParticleTypes.SOUL, player);
                itemstack.hurtAndBreak(SUMMON_DAMAGE, player, (p) -> p.broadcastBreakEvent(hand));
            }

            return ActionResultType.CONSUME;
        }
    }

    private static boolean spawnSummon(PlayerEntity summoner, Hand hand, World world, ItemStack itemstack, BlockPos spawnPos) {
        LazyOptional<SoulsCapability> maybeSouls = getSouls(itemstack);
        if(maybeSouls.isPresent()){
            SoulsCapability souls = maybeSouls.orElse(null);
            Entity summon = souls.summon(summoner, world);

            if(summoner instanceof ServerPlayerEntity){
                ServerPlayerEntity summonerPlayer = (ServerPlayerEntity) summoner;
                int slot = hand == Hand.MAIN_HAND ? summonerPlayer.inventory.selected : 40;
                AccessModNetwork.SYNC_CHANNEL.send(PacketDistributor.PLAYER.with(() -> summonerPlayer), new ClientboundSoulScythePacket(itemstack, slot));
            }

            if(summon != null){
                summon.moveTo((double) spawnPos.getX() + 0.5D, spawnPos.getY(), (double) spawnPos.getZ() + 0.5D, MathHelper.wrapDegrees(world.random.nextFloat() * 360.0F), 0.0F);
                ((ServerWorld) world).addFreshEntityWithPassengers(summon);
            }
            return summon != null;
        }
        return false;
    }

    @Override
    public ActionResult<ItemStack> use(World pLevel, PlayerEntity pPlayer, Hand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        RayTraceResult raytraceresult = getPlayerPOVHitResult(pLevel, pPlayer, RayTraceContext.FluidMode.SOURCE_ONLY);
        if (raytraceresult.getType() != RayTraceResult.Type.BLOCK) {
            return ActionResult.pass(itemstack);
        } else if (!(pLevel instanceof ServerWorld)) {
            return ActionResult.success(itemstack);
        } else {
            BlockRayTraceResult blockraytraceresult = (BlockRayTraceResult)raytraceresult;
            BlockPos spawnPos = blockraytraceresult.getBlockPos();
            if (!(pLevel.getBlockState(spawnPos).getBlock() instanceof FlowingFluidBlock)) {
                return ActionResult.pass(itemstack);
            } else if (pLevel.mayInteract(pPlayer, spawnPos) && pPlayer.mayUseItemAt(spawnPos, blockraytraceresult.getDirection(), itemstack)) {
                if (!spawnSummon(pPlayer, pHand, pLevel, itemstack, spawnPos)) {
                    return ActionResult.pass(itemstack);
                } else {
                    AccessModUtil.sendParticle((ServerWorld) pLevel, ParticleTypes.SOUL, pPlayer);
                    itemstack.hurtAndBreak(SUMMON_DAMAGE, pPlayer, (p) -> p.broadcastBreakEvent(pHand));
                    pPlayer.awardStat(Stats.ITEM_USED.get(this));
                    return ActionResult.consume(itemstack);
                }
            } else {
                return ActionResult.fail(itemstack);
            }
        }
    }

    @Nullable
    @Override
    public CompoundNBT getShareTag(ItemStack stack) {
        CompoundNBT baseTag = stack.getTag();
        CompoundNBT shareTag = super.getShareTag(stack);
        SoulsCapability souls = getSouls(stack).orElse(null);
        CompoundNBT capabilityTag = souls.serializeNBT();
        CompoundNBT combinedTag = new CompoundNBT();
        if (baseTag != null) {
            combinedTag.put(BASE_NBT_TAG, baseTag);
        }
        if (capabilityTag != null) {
            combinedTag.put(CAPABILITY_NBT_TAG, capabilityTag);
        }
        return shareTag;
    }

    @Override
    public void readShareTag(ItemStack stack, @Nullable CompoundNBT nbt) {
        if (nbt == null) {
            stack.setTag(null);
            return;
        }
        CompoundNBT baseTag = nbt.getCompound(BASE_NBT_TAG);              // empty if not found
        CompoundNBT capabilityTag = nbt.getCompound(CAPABILITY_NBT_TAG); // empty if not found
        stack.setTag(baseTag);
        SoulsCapability souls = getSouls(stack).orElse(null);
        souls.deserializeNBT(capabilityTag);
    }
}
