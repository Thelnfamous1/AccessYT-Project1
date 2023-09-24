package me.infamous.accessmod.common.entity.gobblefin;

import me.infamous.accessmod.common.AccessModUtil;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.util.math.MathHelper;

public class DolphinlikeMoveHelperController extends MovementController {
   public DolphinlikeMoveHelperController(MobEntity mob) {
      super(mob);
   }

   @Override
   public void tick() {
      if (this.mob.isInWater()) {
         this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(0.0D, 0.005D, 0.0D));
      }

      if (this.operation == MovementController.Action.MOVE_TO && !this.mob.getNavigation().isDone()) {
         double xDist = this.wantedX - this.mob.getX();
         double yDist = this.wantedY - this.mob.getY();
         double zDist = this.wantedZ - this.mob.getZ();
         double distSqr = xDist * xDist + yDist * yDist + zDist * zDist;
         if (distSqr < (double)2.5000003E-7F) {
            this.mob.setZza(0.0F);
         } else {
            float targetYRot = (float)(MathHelper.atan2(zDist, xDist) * (double)(180F / (float)Math.PI)) - 90.0F;
            this.mob.yRot = this.rotlerp(this.mob.yRot, targetYRot, 10.0F);
            this.mob.yBodyRot = this.mob.yRot;
            this.mob.yHeadRot = this.mob.yRot;
            float movementSpeed = (float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
            if (this.mob.isInWater()) {
               this.mob.setSpeed(movementSpeed * 0.02F);
               float targetXRot = -((float)(MathHelper.atan2(yDist, MathHelper.sqrt(xDist * xDist + zDist * zDist)) * (double)(180F / (float)Math.PI)));
               targetXRot = MathHelper.clamp(MathHelper.wrapDegrees(targetXRot), -85.0F, 85.0F);
               this.mob.xRot = this.rotlerp(this.mob.xRot, targetXRot, 5.0F);
               float cosXRot = MathHelper.cos(this.mob.xRot * AccessModUtil.TO_RADIANS);
               float sinXRot = MathHelper.sin(this.mob.xRot * AccessModUtil.TO_RADIANS);
               this.mob.zza = cosXRot * movementSpeed;
               this.mob.yya = -sinXRot * movementSpeed;
            } else {
               this.mob.setSpeed(movementSpeed * 0.1F);
            }
         }
      } else {
         this.mob.setSpeed(0.0F);
         this.mob.setXxa(0.0F);
         this.mob.setYya(0.0F);
         this.mob.setZza(0.0F);
      }
   }
}