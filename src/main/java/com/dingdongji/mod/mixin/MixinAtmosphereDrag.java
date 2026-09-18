package com.dingdongji.mod.mixin;

import com.dingdongji.mod.event.ModArmorSetHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   targets = {"dev.dubhe.anvilcraft.util.AtmosphereManager"}
)
public abstract class MixinAtmosphereDrag {
   @Inject(
      method = {"drag(Lnet/minecraft/world/entity/Entity;F)F"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private static void ddj$restoreVanillaHorizontalDrag(Entity entity, float drag, CallbackInfoReturnable<Float> cir) {
      if (entity instanceof Player player && ModArmorSetHandler.hasFullTranscendiumSet(player)) {
         cir.setReturnValue(drag);
      }
   }

   @Inject(
      method = {"drag(Lnet/minecraft/world/entity/Entity;D)D"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private static void ddj$restoreVanillaVerticalDrag(Entity entity, double drag, CallbackInfoReturnable<Double> cir) {
      if (entity instanceof Player player && ModArmorSetHandler.hasFullTranscendiumSet(player)) {
         cir.setReturnValue(drag);
      }
   }
}
