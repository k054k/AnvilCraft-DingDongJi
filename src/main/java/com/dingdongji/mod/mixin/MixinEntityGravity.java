package com.dingdongji.mod.mixin;

import com.dingdongji.mod.event.ModArmorSetHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {Entity.class},
   priority = 500
)
public abstract class MixinEntityGravity {
   @Inject(
      method = {"getGravity()D"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void ddj$restoreGravity(CallbackInfoReturnable<Double> cir) {
      Entity self = (Entity)(Object)this;
      if (self instanceof Player player && ModArmorSetHandler.hasFullTranscendiumSet(player)) {
         cir.setReturnValue(0.08);
      }
   }
}
