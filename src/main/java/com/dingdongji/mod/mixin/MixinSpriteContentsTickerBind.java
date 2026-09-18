package com.dingdongji.mod.mixin;

import com.dingdongji.mod.client.GlowPhaseTracker;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteTicker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpriteContents.class)
public abstract class MixinSpriteContentsTickerBind {
   @Inject(
      method = {"createTicker"},
      at = {@At("RETURN")}
   )
   private void ddj$bindTicker(CallbackInfoReturnable<SpriteTicker> cir) {
      SpriteTicker ticker = cir.getReturnValue();
      if (ticker != null) {
         GlowPhaseTracker.bind(ticker, (SpriteContents)(Object)this);
      }
   }
}
