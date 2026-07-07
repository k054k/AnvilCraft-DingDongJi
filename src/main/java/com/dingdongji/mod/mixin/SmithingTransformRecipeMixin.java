package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 创造模板 Mixin v9 — 力求求简
 *
 * 两个注入点，各司其职：
 * 1. isTemplateIngredient @Inject(HEAD) — 让创造模板能放入模板槽
 * 2. template.test() @Redirect in matches() — 让所有锻造配方接受创造模板
 *
 * matches() 是 SmithingTransformRecipe override Recipe 接口 abstract 方法的方法体，
 * 一定在类自身中，@Redirect 可以匹配。
 * template.test() 是 matches 方法体中第一个 Ingredient.test() 调用（ordinal=0），
 * 后面两个分别是 base.test() 和 addition.test()。
 */
@Mixin(SmithingTransformRecipe.class)
public abstract class SmithingTransformRecipeMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("dingdongji");

    /** 让创造模板能放入锻造台模板槽 */
    @Inject(method = "isTemplateIngredient", at = @At("HEAD"), cancellable = true)
    private void dingdongji$isTemplateIngredient(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (ModItems.isCreateTemplate(stack)) {
            cir.setReturnValue(true);
        }
    }

    /** 让所有锻造配方的 template.test() 对创造模板返回 true */
    @Redirect(
        method = "matches",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/crafting/Ingredient;test(Lnet/minecraft/world/item/ItemStack;)Z",
            ordinal = 0
        )
    )
    private boolean dingdongji$templateTest(Ingredient ingredient, ItemStack stack) {
        if (ModItems.isCreateTemplate(stack)) {
            LOGGER.debug("[CreateTemplate] Redirected template.test for create_template -> true");
            return true;
        }
        return ingredient.test(stack);
    }
}
