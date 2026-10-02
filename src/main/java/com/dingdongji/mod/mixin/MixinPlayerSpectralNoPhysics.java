package com.dingdongji.mod.mixin;

import com.dingdongji.mod.event.ModArmorSetHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Player#tick() 开头强制执行 {@code this.noPhysics = this.isSpectator();}
 * （第 255 行），把幻灵逻辑设置的 noPhysics 每帧覆盖。
 *
 * 穿墙采用恼鬼方式：幻灵激活（mode>=1）时 noPhysics=true 全方位豁免
 * 碰撞；垂直方向的限制（mode 1 不纵向穿墙、地面正常行走）由
 * MixinSpectralMoveCollision 在 Entity.move 内部对 y 分量单独做原版
 * 碰撞实现。mode 2 身体处于方块内部时 y 分量不碰撞（真正垂直穿透）。
 *
 * 水中同样保持 noPhysics=true（恼鬼式）：游泳的浮力、划水、转向由
 * travel() 流体逻辑独立结算，不依赖 move 的碰撞分支；若在水中退回
 * noPhysics=false，身体已处于方块内部时原版 collide 会把任何移动钳到
 * 零（先分离才能移动），玩家会永久卡死在方块里——实测复现过。
 * ordinal=0 只匹配首次 isSpectator() 写入，不影响其后的旁观分支判定。
 */
@Mixin(Player.class)
public abstract class MixinPlayerSpectralNoPhysics {
   @ModifyExpressionValue(
      method = "tick()V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;isSpectator()Z",
         ordinal = 0
      )
   )
   private boolean ddj$spectralNoPhysics(boolean spectator) {
      if (spectator) {
         return true;
      }
      Player self = (Player) (Object) this;
      return ModArmorSetHandler.spectralPhaseMode(self) >= 1;
   }
}
