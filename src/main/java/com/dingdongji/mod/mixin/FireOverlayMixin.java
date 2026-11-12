package com.dingdongji.mod.mixin;

import com.dingdongji.mod.client.ClientArmorChecks;
import com.dingdongji.mod.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public abstract class FireOverlayMixin {

   /** 火焰遮罩：余烬全套或超限全套免疫火焰时不再显示（0.1.0 改为全套判定）。 */
   @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
   private static void ddj$suppressFireOverlay(Minecraft minecraft, PoseStack poseStack, CallbackInfo ci) {
      if (ClientArmorChecks.hasFullEmberSet() || ClientArmorChecks.hasFullTranscendiumSet()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderWater", at = @At("HEAD"), cancellable = true)
   private static void ddj$suppressWaterOverlay(Minecraft minecraft, PoseStack poseStack, CallbackInfo ci) {
      Player player = Minecraft.getInstance().player;
      if (player != null
            && player.getItemBySlot(EquipmentSlot.HEAD).is((Item) ModItems.TRANSCENDIUM_HELMET.get())) {
         ci.cancel();
      }
   }

   @Inject(method = "renderTex", at = @At("HEAD"), cancellable = true)
   private static void ddj$suppressInWallOverlay(TextureAtlasSprite sprite, PoseStack poseStack, CallbackInfo ci) {
      Player player = Minecraft.getInstance().player;
      if (player != null
            && player.getItemBySlot(EquipmentSlot.HEAD).is((Item) ModItems.TRANSCENDIUM_HELMET.get())) {
         ci.cancel();
      }
   }
}
