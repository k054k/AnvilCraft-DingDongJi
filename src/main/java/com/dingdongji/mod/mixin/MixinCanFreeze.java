package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public abstract class MixinCanFreeze {
   @Inject(
      method = {"canFreeze"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ddj$helmetNoFreeze(CallbackInfoReturnable<Boolean> cir) {
      if ((Object)this instanceof Player player) {
         ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
         if (helmet.is((Item)ModItems.FROST_METAL_HELMET.get()) || helmet.is((Item)ModItems.TRANSCENDIUM_HELMET.get())) {
            cir.setReturnValue(false);
         }
      }
   }
}
