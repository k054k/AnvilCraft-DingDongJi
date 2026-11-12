package com.dingdongji.mod.mixin;

import mezz.jei.api.registration.IRecipeRegistration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 取消本体"信标转化"分类的配方自注册：改由 DdjJeiPlugin 按"我方配方在前、
 * 本体 4 条腐化信标转化配方在最后"的顺序统一注册，使整个分类的最终顺序
 * 符合要求（JEI 同类目配方顺序按插件注册顺序，本体插件恒先注册无法后置）。
 *
 * <p>{@code @Pseudo}：目标类属于本体的 JEI 集成，JEI 未安装时该类无法解析，
 * 此时整个 mixin 静默跳过；JEI 存在时注入器按全局默认 require=1 严格生效。
 */
@Pseudo
@Mixin(
   value = dev.dubhe.anvilcraft.integration.jei.category.BeaconConversionCategory.class,
   remap = false,
   priority = 1000
)
public abstract class BeaconConversionRegisterMixin {
   @Inject(
      method = "registerRecipes(Lmezz/jei/api/registration/IRecipeRegistration;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private static void ddj$reorderRegistration(IRecipeRegistration registration, CallbackInfo ci) {
      ci.cancel();
   }
}
