package com.dingdongji.mod.mixin;

import com.dingdongji.mod.client.ClientAbilityState;
import com.dingdongji.mod.event.ModArmorSetHandler;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Spectral set phase movement (Vex-style wall phasing).
 *
 * Vanilla recipe: Vex / spectator bypass collision via Entity.noPhysics, but
 * that flag ignores every axis at once. The spec wants the full set to ignore
 * HORIZONTAL collision only (gravity, floors and stairs stay intact), and the
 * boots toggle to additionally ignore VERTICAL collision with shift/space
 * controlled motion. So instead of noPhysics, collide() is intercepted:
 * - mode 1 (full set): resolve vertical movement only, horizontal passes free.
 * - mode 2 (full set + boots toggle): both axes free, Y replaced by the
 *   controlled climb/descent velocity so gravity cannot accumulate.
 */
@Mixin(
   value = {Entity.class},
   priority = 500
)
public abstract class MixinEntitySpectralPhase {
   @Inject(
      method = {"collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ddj$spectralPhaseCollide(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
      Entity self = (Entity)(Object)this;
      if (self instanceof Player player && movement.lengthSqr() > 0.0) {
         int mode = ModArmorSetHandler.spectralPhaseMode(player);
         if (mode == 1) {
            AABB box = self.getBoundingBox();
            List<VoxelShape> shapes = self.level().getEntityCollisions(self, box.expandTowards(new Vec3(0.0, movement.y, 0.0)));
            Vec3 yOnly = Entity.collideBoundingBox(self, new Vec3(0.0, movement.y, 0.0), box, self.level(), shapes);
            cir.setReturnValue(new Vec3(movement.x, yOnly.y, movement.z));
         } else if (mode == 2) {
            double dy = self.level().isClientSide
               ? ClientAbilityState.phaseVerticalVelocity()
               : (self.isShiftKeyDown() ? -0.15 : 0.0);
            cir.setReturnValue(new Vec3(movement.x, dy, movement.z));
            player.setDeltaMovement(player.getDeltaMovement().x, 0.0, player.getDeltaMovement().z);
            player.fallDistance = 0.0F;
         }
      }
   }
}
