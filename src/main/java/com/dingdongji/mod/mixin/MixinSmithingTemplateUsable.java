package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModItems;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
   targets = {"dev.dubhe.anvilcraft.inventory.RoyalSmithingMenu", "dev.dubhe.anvilcraft.inventory.EmberSmithingMenu", "dev.dubhe.anvilcraft.inventory.FrostSmithingMenu"},
   remap = false
)
public abstract class MixinSmithingTemplateUsable {
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
