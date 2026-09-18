package com.dingdongji.mod.mixin;

import com.dingdongji.mod.event.ModArmorSetHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Entity.class})
public abstract class MixinLivingEntityHurt {
   @Inject(
      method = {"animateHurt(F)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ddj$cancelHurtAnimation(float yaw, CallbackInfo ci) {
      if ((Object)this instanceof Player player && ModArmorSetHandler.wearsHurtAnimationCancelArmor(player)) {
         ci.cancel();
      }
   }
}
