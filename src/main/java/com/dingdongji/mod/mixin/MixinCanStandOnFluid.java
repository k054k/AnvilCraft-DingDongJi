package com.dingdongji.mod.mixin;

import com.dingdongji.mod.event.ModArmorSetHandler;
import com.dingdongji.mod.item.ModItems;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntity.class})
public abstract class MixinCanStandOnFluid {
   @Inject(
      method = {"canStandOnFluid"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ddj$canStandOnFluid(FluidState state, CallbackInfoReturnable<Boolean> cir) {
      if (!state.isEmpty()) {
         if ((Object)this instanceof Player player) {
            if (!ModArmorSetHandler.isFluidSwimming(player)) {
               ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
               if (boots.is((Item)ModItems.TRANSCENDIUM_BOOTS.get())) {
                  cir.setReturnValue(true);
               } else if (boots.is((Item)ModItems.EMBER_METAL_BOOTS.get()) && state.is(FluidTags.LAVA)) {
                  cir.setReturnValue(true);
               }
            }
         }
      }
   }
}
