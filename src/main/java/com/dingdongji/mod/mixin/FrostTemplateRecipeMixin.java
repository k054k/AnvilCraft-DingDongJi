package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.item.component.CreateTemplateMode;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 浮霜系两个锻造配方对万用锻造模板的模式判定（双目标合并）：
 * <ul>
 *   <li>PermutationRecipe（置换）：epsilon / alpha；</li>
 *   <li>DeformationRecipe（形变）：zeta / alpha。</li>
 * </ul>
 * 两个目标的 isTemplate(ItemStack) 方法签名与判定结构完全一致，仅模式集合
 * 不同，故合并为一个 @Pseudo mixin，运行时按目标类简名区分专属模式。
 */
@Pseudo
@Mixin(
   targets = {
      "dev.dubhe.anvilcraft.recipe.frost.PermutationRecipe",
      "dev.dubhe.anvilcraft.recipe.frost.DeformationRecipe"
   },
   remap = false
)
public abstract class FrostTemplateRecipeMixin {
   @Inject(
      method = {"isTemplate"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void dingdongji$isTemplate(ItemStack template, CallbackInfoReturnable<Boolean> cir) {
      if (!ModItems.isCreateTemplate(template)) {
         return;
      }
      CreateTemplateMode mode = (CreateTemplateMode) template.get((DataComponentType) ModComponents.CREATE_TEMPLATE_MODE.get());
      if (mode == null) {
         mode = CreateTemplateMode.DEFAULT;
      }
      String m = mode.mode();
      if ("alpha".equals(m)) {
         cir.setReturnValue(true);
         return;
      }
      String required = this.getClass().getSimpleName().contains("Permutation") ? "epsilon" : "zeta";
      if (required.equals(m)) {
         cir.setReturnValue(true);
      }
   }
}
