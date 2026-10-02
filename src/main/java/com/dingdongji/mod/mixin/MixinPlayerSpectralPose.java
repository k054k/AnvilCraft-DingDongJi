package com.dingdongji.mod.mixin;

import com.dingdongji.mod.event.ModArmorSetHandler;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 幻灵穿墙时强制站立姿势。
 *
 * 根因：Player.updatePlayerPose 在 STANDING 尺寸与方块重叠（穿墙）时，
 * 会把姿势降级为 CROUCHING（第 435-436 行，表现为潜行）或 SWIMMING
 * （第 437-438 行，表现为趴下）。穿过活板门等不规则碰撞箱方块时
 * 该降级反复触发。
 *
 * 修复：仅当幻灵激活（mode>=1）且【真的穿入方块】（身体与方块碰撞箱
 * 相交，即水平穿墙/垂直下潜中）时才强制站立。开放地面（mode 1 被动
 * 或 mode 2 未下潜）一律交还原版 updatePlayerPose，使按住潜行键的
 * 蹲伏姿势/视角降低正常生效，避免"穿全套后地面潜行失效"。
 * 水中不干预，保留原版游泳姿势。
 * 其他模组设置的 forcedPose 优先，不拦截。
 */
@Mixin(Player.class)
public class MixinPlayerSpectralPose {
   @Shadow
   private Pose forcedPose;

   @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
   private void ddj$forceStanding(CallbackInfo ci) {
      if (forcedPose != null) {
         return;
      }
      Player self = (Player) (Object) this;
      if (ModArmorSetHandler.spectralPhaseMode(self) < 1 || self.isInWater()) {
         return;
      }
      // 身体未穿入方块、也未在虚化下潜：开放空间，交给原版（保留潜行蹲伏）
      boolean phasing = ModArmorSetHandler.isBodyClippingBlock(self)
         || ModArmorSetHandler.isVerticalPhaseActive(self);
      if (!phasing) {
         return;
      }
      {
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
   }
}
