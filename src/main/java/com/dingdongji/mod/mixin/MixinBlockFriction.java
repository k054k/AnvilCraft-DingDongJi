package com.dingdongji.mod.mixin;

import com.dingdongji.mod.event.ModArmorSetHandler;
import com.dingdongji.mod.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.extensions.IBlockStateExtension;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({IBlockStateExtension.class})
public interface MixinBlockFriction {
   @Inject(
      method = {"getFriction(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)F"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ddj$bootsFriction(LevelReader level, BlockPos pos, @Nullable Entity entity, CallbackInfoReturnable<Float> cir) {
      if (entity instanceof Player player) {
         ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
         if (boots.is((Item)ModItems.FROST_METAL_BOOTS.get())) {
            boolean slideOn = ModArmorSetHandler.isFrostSlideEnabled(player);
            if (slideOn && !player.isShiftKeyDown()) {
               BlockState bs = level.getBlockState(pos);
               if (bs.is(Blocks.BLUE_ICE)) {
                  cir.setReturnValue(1.0F);
               } else if (!bs.is(Blocks.PACKED_ICE) && !bs.is(Blocks.ICE)) {
                  cir.setReturnValue(0.98F);
               } else {
                  cir.setReturnValue(0.999F);
               }
            } else if (!slideOn || !player.isShiftKeyDown()) {
               cir.setReturnValue(0.6F);
            }
         } else if (boots.is((Item)ModItems.TRANSCENDIUM_BOOTS.get())) {
            cir.setReturnValue(0.6F);
         }
      }
   }
}
