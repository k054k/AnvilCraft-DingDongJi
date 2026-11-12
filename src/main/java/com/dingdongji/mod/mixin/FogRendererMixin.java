package com.dingdongji.mod.mixin;

import com.dingdongji.mod.client.ClientArmorChecks;
import com.dingdongji.mod.item.ModItems;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 0.1.0 合并类：余烬头盔熔岩视野（借旁观者判定清熔岩雾）+
 * 超限头盔流体雾清除（水下/岩浆内贴脸雾，保留距离雾与维度背景雾）。
 */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {

   @Redirect(
      method = "setupFog",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;isSpectator()Z",
         ordinal = 0
      )
   )
   private static boolean ddj$lavaVision(Entity entity) {
      if (entity instanceof Player player
            && player.getItemBySlot(EquipmentSlot.HEAD).is((Item) ModItems.EMBER_METAL_HELMET.get())) {
         return true;
      }
      return entity.isSpectator();
   }

   @Redirect(
      method = {"setupFog", "setupColor"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Camera;getFluidInCamera()Lnet/minecraft/world/level/material/FogType;"
      )
   )
   private static FogType ddj$noFluidFog(Camera camera) {
      return ClientArmorChecks.shouldClearFog() ? FogType.NONE : camera.getFluidInCamera();
   }
}
