package com.dingdongji.mod.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 原版 BeaconBlockEntity 的 BlockEntityType 只认 minecraft:beacon，
 * 铭化信标复用原版信标 BE（锥形光柱见 AnchoredBeaconRenderer），这里放行校验。
 */
@Mixin(BlockEntityType.class)
public abstract class BlockEntityTypeMixin {
   private static final ResourceLocation ANCHORED_BEACON_ID = ResourceLocation.fromNamespaceAndPath("dingdongji", "anchored_beacon");

   @Inject(method = "isValid", at = @At("HEAD"), cancellable = true)
   private void dingdongji$allowAnchoredBeacon(BlockState state, CallbackInfoReturnable<Boolean> cir) {
      if ((Object)this == BlockEntityType.BEACON) {
         Block block = (Block)BuiltInRegistries.BLOCK.get(ANCHORED_BEACON_ID);
         if (state.is(block)) {
            cir.setReturnValue(true);
         }
      }
   }
}
