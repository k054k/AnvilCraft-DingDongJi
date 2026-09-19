package com.dingdongji.mod.mixin;

import com.dingdongji.mod.event.ModArmorSetHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets players in surface-walking boots land on a walkable fluid surface
 * without the visual one-tick dip: the landing clamp runs at the tail of the
 * self movement that crosses the surface, exactly like solid-block collision.
 */
@Mixin(Entity.class)
public class MixinEntityFluidSurfaceLand {
   @Inject(
      method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
      at = @At("TAIL")
   )
   private void ddj$landOnFluidSurface(MoverType type, Vec3 movement, CallbackInfo ci) {
      if (type == MoverType.SELF && (Object)this instanceof Player player) {
         ModArmorSetHandler.landOnWalkableFluid(player);
      }
   }
}
