package com.dingdongji.mod.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.dingdongji.mod.client.ClientArmorChecks.hasFullFrostSet;
import static com.dingdongji.mod.client.ClientArmorChecks.hasFullTranscendiumSet;

/**
 * 浮霜全套 / 超限全套免疫细雪霜花覆层。
 *
 * <p>过冷浮霜金属块的结霜屏幕不再自写：EntityMixin 把站在其上的生物标记
 * 为"处于细雪"后，原版按 getPercentFrozen() 自行渲染霜花覆层与冰之心。
 */
@Mixin(Gui.class)
public abstract class GuiOverlaysMixin {
   private static final ResourceLocation POWDER_SNOW_OUTLINE =
      ResourceLocation.withDefaultNamespace("textures/misc/powder_snow_outline.png");

   @Inject(method = "renderTextureOverlay", at = @At("HEAD"), cancellable = true)
   private void ddj$cancelPowderSnowOverlay(GuiGraphics guiGraphics, ResourceLocation location, float alpha, CallbackInfo ci) {
      if (POWDER_SNOW_OUTLINE.equals(location)
            && (hasFullFrostSet() || hasFullTranscendiumSet())) {
         ci.cancel();
      }
   }
}
