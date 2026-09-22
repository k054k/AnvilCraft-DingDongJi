package com.dingdongji.mod.mixin;

import com.dingdongji.mod.event.ModArmorSetHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 和幻灵穿墙同套路：在 Player#tick() 里把 {@code noPhysics = isSpectator()}
 * 改成 {@code noPhysics = isSpectator() || isTranscendiumFlightPhasing(self)}。
 *
 * 条件：全套超限合金 + 飞行中（flying=true）。条件比幻灵的 mode>=1 严格，
 * 下地面/关飞行/脱靴子立即失效。noPhysics=true 之后：
 *   - Entity#move 直接 setPos，无碰撞，穿墙成立；
 *   - 玩家的 Camera 也跟着走，第一人称自然能看到墙里面——
 *     观察者视觉不需要额外代码。
 */
@Mixin(Player.class)
public abstract class MixinPlayerTranscendiumFlightPhase {
   @ModifyExpressionValue(
      method = "tick()V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;isSpectator()Z",
         ordinal = 0
      )
   )
   private boolean ddj$transcendiumFlightNoPhysics(boolean spectator) {
      if (spectator) return true;
      Player self = (Player) (Object) this;
      return ModArmorSetHandler.isTranscendiumFlightPhasing(self);
   }
}
