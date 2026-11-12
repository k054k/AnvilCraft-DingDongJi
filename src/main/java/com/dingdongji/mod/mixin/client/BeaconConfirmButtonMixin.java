package com.dingdongji.mod.mixin.client;

import com.dingdongji.mod.client.ClientBeaconTracker;
import com.dingdongji.mod.util.BeaconBaseHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

/**
 * 4 层免锭特典的客户端放行：原版确认按钮 active = hasPayment() && primary != null，
 * 支付槽为空时按钮永远禁用。这里在支付槽为空时读取最后交互的信标位置——
 * 诅咒金块/重质诅咒金块铺满 4 层则放行确认按钮（与服务端免锭转化判定一致）。
 * BeaconMenu 客户端 access 为 NULL 无法 execute，改用 BeaconBlockClientTrackerMixin
 * 记录的交互坐标直接查客户端世界。
 */
@Mixin(targets = "net.minecraft.client.gui.screens.inventory.BeaconScreen$BeaconConfirmButton")
public abstract class BeaconConfirmButtonMixin {

   @ModifyExpressionValue(
      method = "updateStatus(I)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/BeaconMenu;hasPayment()Z")
   )
   private boolean ddj$allowFreeTier4(boolean original) {
      if (original) {
         return true;
      }
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         return false;
      }
      net.minecraft.core.BlockPos pos = ClientBeaconTracker.getLastInteractedBeacon();
      if (pos == null || !mc.level.getBlockState(pos).is(Blocks.BEACON)) {
         return false;
      }
      return BeaconBaseHelper.countCursedOrHeavyLayers(mc.level, pos) >= 4;
   }
}
