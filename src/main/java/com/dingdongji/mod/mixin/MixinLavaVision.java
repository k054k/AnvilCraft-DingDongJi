package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModItems;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({FogRenderer.class})
public abstract class MixinLavaVision {
   @Redirect(
      method = {"setupFog"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;isSpectator()Z",
         ordinal = 0
      )
   )
   private static boolean ddj$lavaVision(Entity entity) {
      if (entity instanceof Player player) {
         ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
         if (helmet.is((Item)ModItems.EMBER_METAL_HELMET.get())) {
            return true;
         }
      }

      return entity.isSpectator();
   }
}
