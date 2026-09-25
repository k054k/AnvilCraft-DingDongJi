package com.dingdongji.mod.mixin;

import com.dingdongji.mod.client.ClientArmorChecks;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.FogRenderer.FogMode;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

   @Inject(
      method = {"setupFog"},
      at = {@At("RETURN")}
   )
   private static void ddj$clearAllFog(Camera camera, FogMode fogMode, float farPlaneDistance, boolean shouldCreateFog, float partialTick, CallbackInfo ci) {
      if (ClientArmorChecks.shouldClearFog() && ddj$shouldClearDistanceFog(camera)) {
         RenderSystem.setShaderFogStart(-8.0F);
         RenderSystem.setShaderFogEnd(1000000.0F);
      }
   }

   // Keep vanilla background distance fog in overworld air; fluid fog and
   // nether/end distance fog are still cleared. Camera#getFluidInCamera()
   // called here reports the real fog type (the redirect only affects the
   // call sites inside FogRenderer).
   private static boolean ddj$shouldClearDistanceFog(Camera camera) {
      if (camera.getFluidInCamera() != FogType.NONE) {
         return true;
      }
      Level level = Minecraft.getInstance().level;
      return level == null || !level.dimension().equals(Level.OVERWORLD);
   }
}
