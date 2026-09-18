package com.dingdongji.mod.mixin;

import com.dingdongji.mod.client.ClientArmorChecks;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Gui.class})
public abstract class MixinGuiOverlays {
   private static final ResourceLocation POWDER_SNOW_OUTLINE = ResourceLocation.withDefaultNamespace("textures/misc/powder_snow_outline.png");

   @Inject(
      method = {"renderPortalOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ddj$cancelPortalOverlay(GuiGraphics guiGraphics, float alpha, CallbackInfo ci) {
      if (ClientArmorChecks.hasTranscendiumHelmet()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderTextureOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ddj$cancelPowderSnowOverlay(GuiGraphics guiGraphics, ResourceLocation location, float alpha, CallbackInfo ci) {
      if (POWDER_SNOW_OUTLINE.equals(location)) {
         if (ClientArmorChecks.hasTranscendiumHelmet() || ClientArmorChecks.hasFrostHelmet()) {
            ci.cancel();
         }
      }
   }
}
