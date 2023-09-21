package me.infamous.accessmod.client.overlay;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import me.infamous.accessmod.AccessMod;
import me.infamous.accessmod.common.capability.SoulsCapability;
import me.infamous.accessmod.common.item.SoulScytheItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class SoulsOverlay{
    public static final ResourceLocation SOUL_ICON_LOCATION = new ResourceLocation(AccessMod.MODID, "textures/gui/soul_9.png");

    public static final ResourceLocation WIDGETS_LOCATION = new ResourceLocation("textures/gui/widgets.png");
    public static final int SOUL_ICON_WIDTH = 16;
    public static final int SOUL_ICON_HEIGHT = 16;
    private static final int SOUL_ICON_U_OFFSET = 0;
    private static final int SOUL_ICON_V_OFFSET = 0;
    public static final int OFFHAND_SLOT_X_OFFSET = 91;
    public static final int OFFHAND_SLOT_Y_OFFSET = 23;
    public static final int OFFHAND_SLOT_ICON_WIDTH = 29;
    public static final int TOTAL_SOULS_COLOR = 0x01a7ac;

    public static void renderSouls(int screenWidth, int screenHeight, MatrixStack mStack)
    {
        PlayerEntity player = getCameraPlayer();
        if(player == null) return;
        ItemStack soulScythe = getSoulScythe(player);
        if(soulScythe.isEmpty()) return;

        SoulsCapability souls = SoulScytheItem.getSouls(soulScythe).orElse(null);

        // pre-render stuff
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        Minecraft.getInstance().getTextureManager().bind(SOUL_ICON_LOCATION); // bind custom gui icons texture

        // Render the Soul Icon
        int blitOffsetO = Minecraft.getInstance().gui.getBlitOffset();
        Minecraft.getInstance().gui.setBlitOffset(-90); // not sure what this is for, but the hotbar does this when rendering, and we are mimicking it
        int iconXPos = screenWidth / 2 + OFFHAND_SLOT_X_OFFSET + OFFHAND_SLOT_ICON_WIDTH;
        int iconYPos = screenHeight - OFFHAND_SLOT_Y_OFFSET;
        AbstractGui.blit(mStack, iconXPos, iconYPos, SOUL_ICON_U_OFFSET, SOUL_ICON_V_OFFSET, SOUL_ICON_WIDTH, SOUL_ICON_HEIGHT, 16, 16);
        Minecraft.getInstance().gui.setBlitOffset(blitOffsetO);

        // Render the total number of souls
        int totalSouls = souls.getTotalSouls();
        String text = "" + totalSouls;
        int totalSoulsXPos = adjustedWidth(screenWidth, text) / 2 + OFFHAND_SLOT_X_OFFSET + OFFHAND_SLOT_ICON_WIDTH;
        int totalSoulsYPos = screenHeight - OFFHAND_SLOT_Y_OFFSET;
        Minecraft.getInstance().font.draw(mStack, text, (float)(totalSoulsXPos + 1), (float)totalSoulsYPos, 0);
        Minecraft.getInstance().font.draw(mStack, text, (float)(totalSoulsXPos - 1), (float)totalSoulsYPos, 0);
        Minecraft.getInstance().font.draw(mStack, text, (float)totalSoulsXPos, (float)(totalSoulsYPos + 1), 0);
        Minecraft.getInstance().font.draw(mStack, text, (float)totalSoulsXPos, (float)(totalSoulsYPos - 1), 0);
        Minecraft.getInstance().font.draw(mStack, text, (float)totalSoulsXPos, (float)totalSoulsYPos, TOTAL_SOULS_COLOR);

        // post-render stuff
        RenderSystem.enableBlend();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getInstance().getTextureManager().bind(WIDGETS_LOCATION); // reset bound gui icons texture
    }

    private static PlayerEntity getCameraPlayer() {
        return !(Minecraft.getInstance().getCameraEntity() instanceof PlayerEntity) ? null : (PlayerEntity)Minecraft.getInstance().getCameraEntity();
    }

    private static int adjustedWidth(int screenWidth, String text) {
        return screenWidth - Minecraft.getInstance().font.width(text);
    }

    private static ItemStack getSoulScythe(PlayerEntity player){
        if(player.isHolding(item -> item instanceof SoulScytheItem)){
            return player.getItemInHand(ProjectileHelper.getWeaponHoldingHand(player, item -> item instanceof SoulScytheItem));
        } else{
            return ItemStack.EMPTY;
        }
    }
}
