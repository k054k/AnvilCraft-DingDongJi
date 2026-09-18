package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public abstract class EntityCanTrampleMixin {
   @Inject(
      method = {"canTrample"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void ddj$preventTrample(BlockState state, BlockPos pos, float fallDistance, CallbackInfoReturnable<Boolean> cir) {
      if ((Object)this instanceof Player player) {
         ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
         if (boots.is((Item)ModItems.TRANSCENDIUM_BOOTS.get())) {
            cir.setReturnValue(false);
         }
      }
   }
}
