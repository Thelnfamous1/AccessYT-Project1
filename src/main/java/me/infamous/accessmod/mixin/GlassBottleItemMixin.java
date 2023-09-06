package me.infamous.accessmod.mixin;

import me.infamous.accessmod.common.AccessModUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.GlassBottleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtils;
import net.minecraft.potion.Potions;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlassBottleItem.class)
public class GlassBottleItemMixin {

    @Inject(at = @At("RETURN"), method = "use")
    private void use$handleDesertWell(World pLevel, PlayerEntity pPlayer, Hand pHand, CallbackInfoReturnable<ActionResult<ItemStack>> cir){
        ItemStack object = cir.getReturnValue().getObject();
        if(object.getItem() == Items.POTION && PotionUtils.getPotion(object) == Potions.WATER){
            AccessModUtil.handleDesertWellFillBottle(object, pLevel, pPlayer);
        }
    }
}
