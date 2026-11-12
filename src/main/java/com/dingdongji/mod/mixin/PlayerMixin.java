package com.dingdongji.mod.mixin;

import com.dingdongji.mod.event.ModArmorSetHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 0.1.0 合并类：所有注入原版 Player 的逻辑集中于此。
 * 含：幻灵/超限穿墙 noPhysics、幻灵/超限穿墙姿势、墙内挖掘速度豁免。
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

   /**
    * Player#tick() 开头的 this.noPhysics = this.isSpectator()：
    * 幻灵幻影开启（mode>=1，横向穿墙）或超限相位开启时置 true（恼鬼式穿墙）。
    * 垂直方向由 EntityMixin 的 state 1/2 分支结算，mode 1 保持原版垂直碰撞。
    */
   @ModifyExpressionValue(
      method = "tick()V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;isSpectator()Z",
         ordinal = 0
      )
   )
   private boolean ddj$noPhysics(boolean spectator) {
      if (spectator) {
         return true;
      }
      Player self = (Player) (Object) this;
      return ModArmorSetHandler.isHorizontalPhaseActive(self);
   }

   @Shadow
   private Pose forcedPose;

   /**
    * 穿墙时强制站立姿势：幻灵与超限统一口径——水中不干预（防趴泳由
    * EntityMixin 的 isSwimming/isVisuallySwimming 注入保证）；只有身体
    * 确实穿入方块，或幻灵虚化下潜时才强制站立。地面潜行保留正常蹲伏
    * 姿势与 1.5 格碰撞箱（修复超限相位时潜行始终被强制 STANDING 的问题）。
    * 其他模组 forcedPose 优先。
    */
   @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
   private void ddj$forceStanding(CallbackInfo ci) {
      if (forcedPose != null) {
         return;
      }
      Player self = (Player) (Object) this;
      if (!ModArmorSetHandler.isHorizontalPhaseActive(self)) {
         return;
      }
      if (self.isInWater()) {
         return;
      }
      if (!ModArmorSetHandler.isBodyClippingBlock(self) && !ModArmorSetHandler.isVerticalPhaseActive(self)) {
         return;
      }
      Pose pose;
      if (self.isFallFlying()) {
         pose = Pose.FALL_FLYING;
      } else if (self.isSleeping()) {
         pose = Pose.SLEEPING;
      } else if (self.isAutoSpinAttack()) {
         pose = Pose.SPIN_ATTACK;
      } else {
         pose = Pose.STANDING;
      }
      self.setPose(pose);
      ci.cancel();
   }

   /** 穿墙时 noPhysics 强制 onGround=false，豁免方块内部挖掘的非着地 ÷5 惩罚。 */
   @ModifyExpressionValue(
      method = "getDestroySpeed",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;onGround()Z")
   )
   private boolean ddj$noWallDigPenalty(boolean onGround) {
      if (onGround) {
         return true;
      }
      Player self = (Player) (Object) this;
      return ModArmorSetHandler.isHorizontalPhaseActive(self);
   }
}
