package me.infamous.accessmod.common.entity.dune;

import me.infamous.accessmod.common.registry.AccessModEntityTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ItemParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;

public class WrathfulDust extends ProjectileItemEntity implements IEntityAdditionalSpawnData {

    private static final byte HIT_EVENT_ID = (byte) 3;

    public WrathfulDust(EntityType<? extends WrathfulDust> type, World world) {
        super(type, world);
    }

    public WrathfulDust(World world, LivingEntity owner) {
        super(AccessModEntityTypes.WRATHFUL_DUST.get(), owner, world);
    }

    @Override
    public Item getDefaultItem() {
        return Items.SAND;
    }

    protected IParticleData getParticle() {
        ItemStack itemRaw = this.getItemRaw();
        return itemRaw.isEmpty() ?
                new ItemParticleData(ParticleTypes.ITEM, this.getDefaultItem().getDefaultInstance()) :
                new ItemParticleData(ParticleTypes.ITEM, itemRaw);
    }

    @Override
    public void handleEntityEvent(byte pId) {
        if (pId == HIT_EVENT_ID) {
            IParticleData particle = this.getParticle();
            for(int i = 0; i < 8; ++i) {
                this.level.addParticle(particle, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityRayTraceResult pResult) {
        super.onHitEntity(pResult);
        if (!this.level.isClientSide) {
            Entity target = pResult.getEntity();
            Entity owner = this.getOwner();
            boolean hurt = target.hurt(DamageSource.thrown(this, owner), 5.0F);
            if (hurt && owner instanceof LivingEntity) {
                if(target instanceof LivingEntity){
                    Dune.addDuneWrathEffect((LivingEntity) owner, (LivingEntity) target);
                }
                this.doEnchantDamageEffects((LivingEntity)owner, target);
            }
        }
    }

    @Override
    protected void onHit(RayTraceResult pResult) {
        super.onHit(pResult);
        if (!this.level.isClientSide) {
            this.level.broadcastEntityEvent(this, HIT_EVENT_ID);
            this.remove();
        }
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    // IEntityAdditionalSpawnData methods
    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        Entity entity = this.getOwner();
        int ownerId = entity == null ? 0 : entity.getId();
        buffer.writeInt(ownerId);
    }
    @Override
    public void readSpawnData(PacketBuffer additionalData) {
        int ownerId = additionalData.readInt();
        Entity owner = this.level.getEntity(ownerId);
        if(owner != null) this.setOwner(owner);
    }
}
