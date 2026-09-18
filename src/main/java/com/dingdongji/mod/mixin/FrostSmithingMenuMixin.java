package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModItems;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   targets = {"dev.dubhe.anvilcraft.inventory.FrostSmithingMenu"}
)
public abstract class FrostSmithingMenuMixin {
   @Inject(
      method = {"isUsableTemplate"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void ddj$usableTemplate(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
      if (ModItems.isCreateTemplate(stack)) {
         cir.setReturnValue(true);
      }
   }
}
