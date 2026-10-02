package com.dingdongji.mod.mixin;

import com.dingdongji.mod.event.ModArmorSetHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 原版 {@link Player#getDestroySpeed} 末尾：{@code if (!this.onGround()) f /= 5.0F;}。
 * 幻灵套/超限合金套穿墙时 noPhysics 强制 onGround=false，导致在方块内部
 * 挖方块必吃非着地 ÷5 惩罚。穿着全套这两套盔甲时把该 onGround 判定
 * 视为 true，仅豁免这一条减速；水下挖掘等其它规则不受影响。
 */
@Mixin(Player.class)
public abstract class MixinPlayerWallDigSpeed {
   @ModifyExpressionValue(
      method = "getDestroySpeed",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;onGround()Z")
   )
   private boolean ddj$noWallDigPenalty(boolean onGround) {
      if (onGround) {
         return true;
      }
      Player self = (Player) (Object) this;
      return ModArmorSetHandler.hasFullSpectralSet(self) || ModArmorSetHandler.hasFullTranscendiumSet(self);
   }
}
