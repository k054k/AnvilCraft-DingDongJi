package com.dingdongji.mod.mixin;

import com.dingdongji.mod.event.ModArmorSetHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public abstract class MixinEntityBlockSlowdown {
   private static final double BOUNCE_WALK_THRESHOLD = -0.25;

   @Inject(
      method = {"makeStuckInBlock"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ddj$cancelStuckInBlock(BlockState state, Vec3 motionMultiplier, CallbackInfo ci) {
      if (ModArmorSetHandler.ignoresBlockSlowdown((Entity)(Object)this)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"getBlockSpeedFactor"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ddj$cancelBlockSpeedFactor(CallbackInfoReturnable<Float> cir) {
      if (ModArmorSetHandler.ignoresBlockSlowdown((Entity)(Object)this)) {
         cir.setReturnValue(1.0F);
      }
   }

   @Inject(
      method = {"getBlockJumpFactor"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ddj$cancelBlockJumpFactor(CallbackInfoReturnable<Float> cir) {
      if (ModArmorSetHandler.ignoresBlockSlowdown((Entity)(Object)this)) {
         cir.setReturnValue(1.0F);
      }
   }

   @Inject(
      method = {"isSuppressingBounce"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ddj$conditionalBounceSuppress(CallbackInfoReturnable<Boolean> cir) {
      Entity self = (Entity)(Object)this;
      if (ModArmorSetHandler.ignoresBlockSlowdown(self) && self.getDeltaMovement().y >= -0.25) {
         cir.setReturnValue(true);
      }
   }
}
