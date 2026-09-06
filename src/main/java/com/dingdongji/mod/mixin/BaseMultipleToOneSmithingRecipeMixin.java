package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.item.component.CreateTemplateMode;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.dubhe.anvilcraft.recipe.multiple.BaseMultipleToOneSmithingRecipe")
public abstract class BaseMultipleToOneSmithingRecipeMixin {

    @Inject(method = "isTemplateIngredient", at = @At("HEAD"), cancellable = true, remap = false)
    private void dingdongji$isTemplateIngredient(ItemStack template, CallbackInfoReturnable<Boolean> cir) {
        if (!ModItems.isCreateTemplate(template)) {
            return;
        }
        CreateTemplateMode mode = template.get(ModComponents.CREATE_TEMPLATE_MODE.get());
        if (mode == null) mode = CreateTemplateMode.DEFAULT;

        // α 万能；β/γ/δ 对应二/四/八合一。此处只放行模板识别，
        // 具体槽位数由 EmberSmithingMenuMixin.getInputSize 按 mode 决定。
        if (CreateTemplateMode.ALPHA.equals(mode)
                || CreateTemplateMode.BETA.equals(mode)
                || CreateTemplateMode.GAMMA.equals(mode)
                || CreateTemplateMode.DELTA.equals(mode)) {
            cir.setReturnValue(true);
        }
    }
}
