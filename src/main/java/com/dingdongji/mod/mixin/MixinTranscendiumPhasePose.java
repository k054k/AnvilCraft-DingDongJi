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
 * 超限合金套飞行相位偏移时强制站立姿势——防蹲 + 防游泳/趴下姿势。
 * <p>
 * 根因（已实证 Player.updatePlayerPose）：
 * <ul>
 *   <li>第 410 行：shift 且非飞行 → CROUCHING；相位时正在 flying，本不触发，
 *       但脱飞/落地切换的瞬间仍可能出现；</li>
 *   <li>第 417-423 行：STANDING 尺寸与空间不匹配（穿墙时身体在方块里）
 *       会降级 CROUCHING，再不行 SWIMMING——这是穿墙时反复蹲/趴的来源；</li>
 *   <li>第 406 行：isSwimming() → SWIMMING（水中趴泳）。</li>
 * </ul>
 * 与幻灵 MixinPlayerSpectralPose 同套路，但<b>不排除水中</b>：用户要求
 * 相位时连游泳姿势一并防止。跳过全部空间检测，鞘翅/睡觉/三叉戟旋转等
 * 特殊状态保留，其余一律 STANDING。其他模组设置的 forcedPose 优先。
 */
@Mixin(Player.class)
public class MixinTranscendiumPhasePose {
   @Shadow
   private Pose forcedPose;

   @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
   private void ddj$phaseForceStanding(CallbackInfo ci) {
      if (forcedPose != null) {
         return;
      }
      Player self = (Player) (Object) this;
      if (!ModArmorSetHandler.isTranscendiumFlightPhasing(self)) {
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
}
