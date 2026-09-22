package com.dingdongji.mod.mixin;

import com.dingdongji.mod.event.ModArmorSetHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 飞行相位穿墙时玩家会处在方块里，原版 {@code isInWall} 返回 true 会导致
 * 窒息（damageSources.IN_WALL 伤害）。
 * 和幻灵套的 {@link MixinSpectralMoveCollision#ddj$noSuffocation} 同套路：
 * isInWall HEAD 取消，飞行相位激活时强制返回 false。
 */
@Mixin(Entity.class)
public class MixinTranscendiumNoSuffocation {
   @Inject(method = "isInWall", at = @At("HEAD"), cancellable = true)
   private void ddj$transcendiumFlightNoSuffocation(CallbackInfoReturnable<Boolean> cir) {
      Entity self = (Entity) (Object) this;
      if (self instanceof Player player && ModArmorSetHandler.isTranscendiumFlightPhasing(player)) {
         cir.setReturnValue(false);
      }
   }
}
