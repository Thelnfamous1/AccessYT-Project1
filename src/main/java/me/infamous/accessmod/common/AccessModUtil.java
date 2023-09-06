package me.infamous.accessmod.common;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.World;

public class AccessModUtil {
    private static final int BASE_INACCURACY = 14;
    private static final float BASE_VELOCITY = 1.6F;
    private static final int DIFFICULTY_FACTOR = 4;

    public void shootLikeBow(LivingEntity shooter, LivingEntity target, ProjectileEntity projectile, SoundEvent shootSound) {
        double xDist = target.getX() - shooter.getX();
        double yDist = target.getY(0.3333333333333333D) - projectile.getY();
        double zDist = target.getZ() - shooter.getZ();
        double horizDist = MathHelper.sqrt(xDist * xDist + zDist * zDist);
        projectile.shoot(xDist, yDist + horizDist * (double)0.2F, zDist, BASE_VELOCITY, 0.0F);
        shooter.playSound(shootSound, 1.0F, 1.0F / (shooter.getRandom().nextFloat() * 0.4F + 0.8F));
        shooter.level.addFreshEntity(projectile);
    }

    public static void shootLikeSnowball(LivingEntity shooter, LivingEntity target, ProjectileEntity projectile, SoundEvent shootSound){
        double targetY = target.getEyeY() - (double)1.1F;
        double xDist = target.getX() - shooter.getX();
        double yDist = targetY - projectile.getY();
        double zDist = target.getZ() - shooter.getZ();
        float yFactor = MathHelper.sqrt(xDist * xDist + zDist * zDist) * 0.2F;
        projectile.shoot(xDist, yDist + (double)yFactor, zDist, BASE_VELOCITY, 0.0F);
        shooter.playSound(shootSound, 1.0F, 0.4F / (shooter.getRandom().nextFloat() * 0.4F + 0.8F));
        shooter.level.addFreshEntity(projectile);
    }

    private static int getStandardInaccuracy(LivingEntity shooter) {
        return BASE_INACCURACY - shooter.level.getDifficulty().getId() * DIFFICULTY_FACTOR;
    }

    public static void shootLikeCrossbow(LivingEntity shooter, LivingEntity target, ProjectileEntity projectile, SoundEvent shootSound) {
        double xDist = target.getX() - shooter.getX();
        double zDist = target.getZ() - shooter.getZ();
        double horizDist = MathHelper.sqrt(xDist * xDist + zDist * zDist);
        double yDist = target.getY(0.3333333333333333D) - projectile.getY() + horizDist * (double)0.2F;
        float inaccuracy = getStandardInaccuracy(shooter);
        Vector3f shotVector = getCrossbowShotVector(shooter, new Vector3d(xDist, yDist, zDist), 0.0F);
        projectile.shoot(shotVector.x(), shotVector.y(), shotVector.z(), BASE_VELOCITY, 0.0F);
        shooter.level.addFreshEntity(projectile);
        shooter.playSound(shootSound, 1.0F, 1.0F / (shooter.getRandom().nextFloat() * 0.4F + 0.8F));
    }

    public static Vector3f getCrossbowShotVector(LivingEntity shooter, Vector3d distVec, float angle) {
        Vector3d normDistVec = distVec.normalize();
        Vector3d upVector = normDistVec.cross(new Vector3d(0.0D, 1.0D, 0.0D));
        if (upVector.lengthSqr() <= 1.0E-7D) {
            upVector = normDistVec.cross(shooter.getUpVector(1.0F));
        }

        Quaternion upQuaternion = new Quaternion(new Vector3f(upVector), 90.0F, true);
        Vector3f normDistVecF = new Vector3f(normDistVec);
        normDistVecF.transform(upQuaternion);
        Quaternion angledQuaternion = new Quaternion(normDistVecF, angle, true);
        Vector3f shotVector = new Vector3f(normDistVec);
        shotVector.transform(angledQuaternion);
        return shotVector;
    }

    public static BlockRayTraceResult getPlayerPOVHitResult(World pLevel, PlayerEntity pPlayer, RayTraceContext.FluidMode pFluidMode) {
        float f = pPlayer.xRot;
        float f1 = pPlayer.yRot;
        Vector3d vector3d = pPlayer.getEyePosition(1.0F);
        float f2 = MathHelper.cos(-f1 * ((float)Math.PI / 180F) - (float)Math.PI);
        float f3 = MathHelper.sin(-f1 * ((float)Math.PI / 180F) - (float)Math.PI);
        float f4 = -MathHelper.cos(-f * ((float)Math.PI / 180F));
        float f5 = MathHelper.sin(-f * ((float)Math.PI / 180F));
        float f6 = f3 * f4;
        float f7 = f2 * f4;
        double d0 = pPlayer.getAttribute(net.minecraftforge.common.ForgeMod.REACH_DISTANCE.get()).getValue();
        Vector3d vector3d1 = vector3d.add((double)f6 * d0, (double)f5 * d0, (double)f7 * d0);
        return pLevel.clip(new RayTraceContext(vector3d, vector3d1, RayTraceContext.BlockMode.OUTLINE, pFluidMode, pPlayer));
    }
}
