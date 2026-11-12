package com.dingdongji.mod.mixin;

import com.dingdongji.mod.event.ModArmorSetHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 全套超限无视铁砧工艺天体源引力（黑洞/白洞/锻星砧重力场/金叽星球等）。
 * 对齐铁砧护符 IgnoreGravityAmuletEffect 的口径：不限维度，只要穿全套超限
 * 就把天体引力向量置零；普通维度的原版重力（getGravity）不受影响，
 * 仅太空维度（mun/void_planet/overworld_like）的维度重力由 EntityGravityMixin 清零。
 */
@Mixin(targets = "dev.dubhe.anvilcraft.util.GravityManager", remap = false)
public abstract class GravityManagerMixin {

   @Inject(
      method = "getGravityVector(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/Vec3;",
      at = @At("HEAD"),
      cancellable = true,
      remap = false
   )
   private static void dingdongji$cancelGravity1(Entity entity, CallbackInfoReturnable<Vec3> cir) {
      if (entity instanceof Player player
            && ModArmorSetHandler.hasFullTranscendiumSet(player)) {
         cir.setReturnValue(Vec3.ZERO);
      }
   }

   @Inject(
      method = "getGravityVector(Lnet/minecraft/world/entity/Entity;D)Lnet/minecraft/world/phys/Vec3;",
      at = @At("HEAD"),
      cancellable = true,
      remap = false
   )
   private static void dingdongji$cancelGravity2(Entity entity, double movement, CallbackInfoReturnable<Vec3> cir) {
      if (entity instanceof Player player
            && ModArmorSetHandler.hasFullTranscendiumSet(player)) {
         cir.setReturnValue(Vec3.ZERO);
      }
   }

   /**
    * 1.6.0 的 deferVerticalGravity 直接走 GravitySourceManager 计算，绕开了 getGravityVector。
    * 全套超限直接返回原始 verticalMotion（不含天体引力的竖直分量）。
    */
   @Inject(
      method = "deferVerticalGravity(Lnet/minecraft/world/entity/Entity;DD)D",
      at = @At("HEAD"),
      cancellable = true,
      remap = false
   )
   private static void dingdongji$cancelVerticalGravity(Entity entity, double verticalMotion, double gravity,
         CallbackInfoReturnable<Double> cir) {
      if (entity instanceof Player player
            && ModArmorSetHandler.hasFullTranscendiumSet(player)) {
         cir.setReturnValue(verticalMotion);
      }
   }

   /**
    * 同上，横向引力：返回传入的 movement，即不叠加天体引力的水平分量。
    */
   @Inject(
      method = "deferHorizontalGravity(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
      at = @At("HEAD"),
      cancellable = true,
      remap = false
   )
   private static void dingdongji$cancelHorizontalGravity(Entity entity, Vec3 movement,
         CallbackInfoReturnable<Vec3> cir) {
      if (entity instanceof Player player
            && ModArmorSetHandler.hasFullTranscendiumSet(player)) {
         cir.setReturnValue(movement);
      }
   }
}
