package com.dingdongji.mod.mixin;

import com.dingdongji.mod.client.ClientArmorChecks;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 超限合金头盔（适应）：仅去掉流体雾遮挡（水下/岩浆内的贴脸雾），
 * 保留原版距离雾与维度背景雾。
 */
@Mixin({FogRenderer.class})
public abstract class MixinTranscendiumFog {
   @Redirect(
      method = {"setupFog", "setupColor"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Camera;getFluidInCamera()Lnet/minecraft/world/level/material/FogType;"
      )
   )
   private static FogType ddj$noFluidFog(Camera camera) {
      return ClientArmorChecks.shouldClearFog() ? FogType.NONE : camera.getFluidInCamera();
   }
}
