package com.dingdongji.mod.mixin;

import com.dingdongji.mod.client.ClientArmorChecks;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 头盔夜视强度。
 *
 * LightTexture#updateLightTexture 中夜视的实际做法是把每个像素颜色向
 * "最大通道归一化到 1.0"（拉满）的方向按 getNightVisionScale 返回值
 * 混合：1.0 时暗处一步拉到全亮，明暗层次全部抹平，观感生硬；降到
 * 0.7 后暗处仍大幅提亮（约 0.05 → 0.72），但不到顶，保留柔和的夜晚
 * 明暗层次。只覆盖头盔夜视，原版药水夜视仍走原版 scale（常态 1.0）。
 */
@Mixin({GameRenderer.class})
public abstract class MixinNightVisionScale {
   @Inject(
      method = {"getNightVisionScale"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void ddj$fullNightVision(LivingEntity livingEntity, float nanoTime, CallbackInfoReturnable<Float> cir) {
      if (ClientArmorChecks.helmetNightVision()) {
         cir.setReturnValue(0.7F);
      }
   }
}
