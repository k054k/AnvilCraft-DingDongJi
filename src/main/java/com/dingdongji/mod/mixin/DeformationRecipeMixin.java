package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.item.component.CreateTemplateMode;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   targets = {"dev.dubhe.anvilcraft.recipe.frost.DeformationRecipe"}
)
public abstract class DeformationRecipeMixin {
   @Inject(
      method = {"isTemplate"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void dingdongji$isTemplate(ItemStack template, CallbackInfoReturnable<Boolean> cir) {
      if (ModItems.isCreateTemplate(template)) {
         CreateTemplateMode mode = (CreateTemplateMode)template.get((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get());
         if (mode == null) {
            mode = CreateTemplateMode.DEFAULT;
         }

         if ("zeta".equals(mode.mode()) || "alpha".equals(mode.mode())) {
            cir.setReturnValue(true);
         }
      }
   }
}
