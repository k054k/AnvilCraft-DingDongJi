package com.dingdongji.mod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PocketSlot$Access.getContainerSize() 硬编码 12，QuickCraftStack 等
 * 容器操作会依据它判断，提升到 24。实际存取直接转发到 PocketInventory
 * （已为 24 格），无需其他改动。
 */
@Mixin(targets = "dev.dubhe.anvilcraft.inventory.PocketSlot$Access", remap = false)
public class MixinPocketSlotAccess {
   @Inject(
      method = "getContainerSize",
      at = @At("HEAD"),
      cancellable = true,
      require = 1
   )
   private void ddj$expandAccessSize(CallbackInfoReturnable<Integer> cir) {
      cir.setReturnValue(24);
   }
}
