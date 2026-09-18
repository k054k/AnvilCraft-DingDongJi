package com.dingdongji.mod.mixin;

import com.dingdongji.mod.client.ClientArmorChecks;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({GameRenderer.class})
public abstract class MixinNightVisionScale {
   @Inject(
      method = {"getNightVisionScale"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void ddj$fullNightVision(LivingEntity livingEntity, float nanoTime, CallbackInfoReturnable<Float> cir) {
      if (ClientArmorChecks.helmetNightVision()) {
         cir.setReturnValue(1.0F);
      }
   }
}
