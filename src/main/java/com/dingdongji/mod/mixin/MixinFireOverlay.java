package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ScreenEffectRenderer.class})
public abstract class MixinFireOverlay {
   @Inject(
      method = {"renderFire"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void ddj$suppressFireOverlay(Minecraft minecraft, PoseStack poseStack, CallbackInfo ci) {
      Player player = Minecraft.getInstance().player;
      if (player != null) {
         ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
         if (helmet.is((Item)ModItems.EMBER_METAL_HELMET.get()) || helmet.is((Item)ModItems.TRANSCENDIUM_HELMET.get())) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"renderWater"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void ddj$suppressWaterOverlay(Minecraft minecraft, PoseStack poseStack, CallbackInfo ci) {
      Player player = Minecraft.getInstance().player;
      if (player != null) {
         if (player.getItemBySlot(EquipmentSlot.HEAD).is((Item)ModItems.TRANSCENDIUM_HELMET.get())) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"renderTex"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void ddj$suppressInWallOverlay(TextureAtlasSprite sprite, PoseStack poseStack, CallbackInfo ci) {
      Player player = Minecraft.getInstance().player;
      if (player != null) {
         if (player.getItemBySlot(EquipmentSlot.HEAD).is((Item)ModItems.TRANSCENDIUM_HELMET.get())) {
            ci.cancel();
         }
      }
   }
}
