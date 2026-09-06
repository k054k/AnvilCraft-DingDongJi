package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 余烬 / 超限头盔：屏蔽屏幕上的火焰覆盖层。
 * 火焰覆盖层由 ScreenEffectRenderer.renderFire 渲染（并非 Gui.renderTextureOverlay / displayFireAnimation）。
 * 穿着余烬头盔时 cancel 渲染，不影响 fire ticks 本身（余烬套回耐久等功能仍依赖 in_fire）。
 */
@Mixin(ScreenEffectRenderer.class)
public abstract class MixinFireOverlay {

    @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
    private static void ddj$suppressFireOverlay(Minecraft minecraft, PoseStack poseStack, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.is(ModItems.EMBER_METAL_HELMET.get())
                || helmet.is(ModItems.TRANSCENDIUM_HELMET.get())) {
            ci.cancel();
        }
    }
}
