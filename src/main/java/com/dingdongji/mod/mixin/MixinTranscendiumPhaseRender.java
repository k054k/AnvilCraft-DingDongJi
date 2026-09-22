package com.dingdongji.mod.mixin;

import com.dingdongji.mod.event.ModArmorSetHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 超限合金套飞行相位穿墙时，让区块渲染剔除规则与观察者模式一致。
 * <p>
 * LevelRenderer.renderLevel 在调用 setupRender 前取 LocalPlayer.isSpectator()
 * 作为第三参（spectator 标志）。观察者模式下该标志为 true，区块剔除更宽松
 * （穿墙时能看到方块外侧/背面内容）。相位激活时伪造为 true，获得相同视野。
 * 仅修改这一处调用的返回值，不影响 isSpectator() 的其他用途。
 */
@Mixin(LevelRenderer.class)
public abstract class MixinTranscendiumPhaseRender {
   @ModifyExpressionValue(
      method = "renderLevel",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z"
      ),
      require = 1
   )
   private boolean ddj$phaseAsSpectatorForRender(boolean spectator) {
      if (spectator) return true;
      Player player = Minecraft.getInstance().player;
      return player != null && ModArmorSetHandler.isTranscendiumFlightPhasing(player);
   }
}
