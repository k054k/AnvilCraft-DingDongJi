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
 * 修复：幻灵激活（mode>=1）且非水中时，跳过空间检测，按特殊状态
 * （鞘翅/睡觉/三叉戟旋转）设置姿势，其余一律 STANDING。
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
      if (ModArmorSetHandler.spectralPhaseMode(self) >= 1 && !self.isInWater()) {
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
