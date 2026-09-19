package com.dingdongji.mod.mixin;

import com.dingdongji.mod.client.GlowPhaseTracker;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires exactly when the game advances the block atlas animated sprites, so
 * the armor glow clock runs on the same pulse and from the same stitch origin
 * as the metal block outline textures.
 */
@Mixin(TextureAtlas.class)
public class MixinTextureAtlasPulse {
   @Inject(method = "cycleAnimationFrames", at = @At("HEAD"))
   private void ddj$onAnimationPulse(CallbackInfo ci) {
      GlowPhaseTracker.onAtlasPulse((TextureAtlas)(Object)this);
   }
}
