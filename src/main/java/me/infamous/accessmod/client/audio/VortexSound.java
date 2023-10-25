package me.infamous.accessmod.client.audio;

import me.infamous.accessmod.common.entity.ai.eater.VortexEater;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.TickableSound;
import net.minecraft.entity.Entity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;

public class VortexSound<T extends Entity & VortexEater> extends TickableSound {
   private final T vortexEater;

   public VortexSound(T vortexEater) {
      super(SoundEvents.GUARDIAN_ATTACK, SoundCategory.HOSTILE);
      this.vortexEater = vortexEater;
      this.attenuation = ISound.AttenuationType.NONE;
      this.looping = true;
      this.delay = 0;
   }

   public static <T extends Entity & VortexEater> VortexSound<T> hackyCreate(VortexEater vortexEater){
      return new VortexSound<>((T)vortexEater);
   }

   @Override
   public boolean canPlaySound() {
      return !this.vortexEater.isSilent();
   }

   @Override
   public void tick() {
      if (this.vortexEater.isAlive() && this.vortexEater.isSuckingUp()) {
         this.x = (float)this.vortexEater.getX();
         this.y = (float)this.vortexEater.getY();
         this.z = (float)this.vortexEater.getZ();
         float vortexAnimationScale = this.vortexEater.getVortexAnimationScale(0.0F);
         this.volume = 0.0F + 1.0F * vortexAnimationScale * vortexAnimationScale;
         this.pitch = 0.7F + 0.5F * vortexAnimationScale;
      } else {
         this.stop();
      }
   }
}