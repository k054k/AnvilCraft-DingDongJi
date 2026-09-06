package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.component.CreateTemplateMode;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让创造模板的 ζ（形变）模式和 α（通用）模式能通过 DeformationRecipe 的模板检查。
 */
@Mixin(targets = "dev.dubhe.anvilcraft.recipe.frost.DeformationRecipe")
public abstract class DeformationRecipeMixin {

    @Inject(method = "isTemplate", at = @At("HEAD"), cancellable = true, remap = false)
    private void dingdongji$isTemplate(ItemStack template, CallbackInfoReturnable<Boolean> cir) {
        if (!ModItems.isCreateTemplate(template)) return;

        CreateTemplateMode mode = template.get(ModComponents.CREATE_TEMPLATE_MODE.get());
        if (mode == null) mode = CreateTemplateMode.DEFAULT;

        // ζ: 替代形变模板；α: 万能模板
        if ("zeta".equals(mode.mode()) || "alpha".equals(mode.mode())) {
            cir.setReturnValue(true);
        }
    }
}
