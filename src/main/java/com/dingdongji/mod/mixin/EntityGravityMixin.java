package com.dingdongji.mod.mixin;

import com.dingdongji.mod.event.ModArmorSetHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 全套超限"无视星球引力"：仅在铁砧工艺太空维度（月球 / 锻星砧星球）
 * 把重力强制为 0；普通维度不干预，保持原版 0.08 重力
 * （铁砧本体的 Entity#getGravity 公式：原重力×维度系数-天体引力，
 * 在主世界/下界/末地系数为 1.0，直接放行即可）。
 */
@Mixin(value = Entity.class, priority = 500)
public abstract class EntityGravityMixin {
   @Inject(method = "getGravity()D", at = @At("RETURN"), cancellable = true)
   private void ddj$zeroGravityInSpace(CallbackInfoReturnable<Double> cir) {
      Entity self = (Entity) (Object) this;
      if (self instanceof Player player
            && ModArmorSetHandler.hasFullTranscendiumSet(player)
            && ModArmorSetHandler.isAnvilSpaceDimension(self)) {
         cir.setReturnValue(0.0);
      }
   }
}
