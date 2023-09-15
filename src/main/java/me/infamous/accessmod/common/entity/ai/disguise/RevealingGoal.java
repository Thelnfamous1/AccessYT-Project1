package me.infamous.accessmod.common.entity.ai.disguise;

import me.infamous.accessmod.common.network.AccessModNetwork;
import me.infamous.accessmod.common.network.ClientboundRevealPacket;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.EnumSet;

public class RevealingGoal<T extends MobEntity & AnimatableDisguise> extends Goal {

    protected final T mob;
    private final int duration;
    private int revealingTicks;

    public RevealingGoal(T mob, int duration){
        this.mob = mob;
        this.duration = duration;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.mob.isDisguised() && this.mob.wantsToReveal();
    }

    @Override
    public void start() {
        this.mob.setRevealing();
        AccessModNetwork.SYNC_CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this.mob),
                new ClientboundRevealPacket<>(this.mob));
        this.mob.playSound(this.mob.getRevealSound(), 5.0F, 1.0F);
        this.revealingTicks = this.duration;
    }

    @Override
    public void tick() {
        super.tick();
        this.revealingTicks--;
    }

    @Override
    public boolean canContinueToUse() {
        return this.revealingTicks > 0 && this.mob.isAlive();
    }

    @Override
    public void stop() {
        this.mob.setRevealed();
    }
}
