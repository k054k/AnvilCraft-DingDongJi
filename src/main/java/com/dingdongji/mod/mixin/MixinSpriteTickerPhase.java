package com.dingdongji.mod.mixin;

import com.dingdongji.mod.client.GlowPhaseTracker;
import net.minecraft.client.renderer.texture.SpriteTicker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.texture.SpriteContents$Ticker")
public abstract class MixinSpriteTickerPhase {
   @Shadow
   int frame;

   @Shadow
   int subFrame;

   @Inject(
      method = {"tickAndUpload"},
      at = {@At("TAIL")}
   )
   private void ddj$recordPhase(int x, int y, CallbackInfo ci) {
      GlowPhaseTracker.record((SpriteTicker)(Object)this, this.frame, this.subFrame);
   }
}
