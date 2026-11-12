package com.dingdongji.mod.mixin;

import com.dingdongji.mod.util.BeamLockTracker;
import dev.dubhe.anvilcraft.api.heat.HeatTierLine;
import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 超温余烬金属块（本体 HeatableBlockEntity 体系）的冷却由 HeaterManager
 * 每 tick 统一结算（duration 递减，归零退档）。在结算入口拦截：方块正被
 * 铭化信标"时定"（光柱照到且在时定配方白名单内）时整个冷却 tick 跳过，
 * duration 不递减；下 tick BlockEntity 会重新把位置注册回 heatableBlocks。
 */
@Mixin(value = HeaterManager.class, remap = false)
public abstract class HeaterManagerMixin {
   @Shadow
   @Final
   private Level level;

   @Inject(
      method = "tickHeatableBlock(Lnet/minecraft/core/BlockPos;Ldev/dubhe/anvilcraft/api/heat/HeatTierLine$Point;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void ddj$timeAnchorFreeze(BlockPos pos, HeatTierLine.Point point, CallbackInfo ci) {
      if (this.level instanceof ServerLevel && BeamLockTracker.isLocked(this.level, pos)) {
         ci.cancel();
      }
   }
}
