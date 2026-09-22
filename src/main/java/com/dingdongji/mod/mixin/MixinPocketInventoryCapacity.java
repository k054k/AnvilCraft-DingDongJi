package com.dingdongji.mod.mixin;

import com.dingdongji.mod.util.AnvilCraftCompat;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 拦截铁砧 PocketInventory.capacity()，统一改由
 * {@link AnvilCraftCompat#getPocketCapacity} 计算（铁砧本体容量
 * 与 DDJ 口袋组件取 max）。
 */
@Mixin(targets = "dev.dubhe.anvilcraft.inventory.PocketInventory", remap = false)
public class MixinPocketInventoryCapacity {
   @Inject(
      method = "capacity",
      at = @At("RETURN"),
      cancellable = true,
      require = 1
   )
   private static void ddj$extendCapacity(Player player, CallbackInfoReturnable<Integer> cir) {
      cir.setReturnValue(AnvilCraftCompat.getPocketCapacity(player));
   }
}
