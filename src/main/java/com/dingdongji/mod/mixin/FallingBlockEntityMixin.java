package com.dingdongji.mod.mixin;

import com.dingdongji.mod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({FallingBlockEntity.class})
public abstract class FallingBlockEntityMixin {
   @Shadow
   public boolean cancelDrop;
   private boolean dingdongji$isJiAnvil = false;

   @Shadow
   public abstract BlockState getBlockState();

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   private void checkJiAnvil(CallbackInfo ci) {
      BlockState bs = this.getBlockState();
      this.dingdongji$isJiAnvil = bs != null && bs.is((Block)ModBlocks.JI_ANVIL.get());
   }

   @Redirect(
      method = {"tick"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
      )
   )
   private boolean silenceAnvilPlaceSound(Level level, BlockPos pos, BlockState state) {
      return this.dingdongji$isJiAnvil ? level.setBlock(pos, state, 18) : level.setBlockAndUpdate(pos, state);
   }

   @Inject(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/block/Fallable;onLand(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/item/FallingBlockEntity;)V"
      )}
   )
   private void onJiAnvilLand(CallbackInfo ci) {
      if (this.dingdongji$isJiAnvil) {
         FallingBlockEntity self = (FallingBlockEntity)(Object)this;
         if (self.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, self.blockPosition(), SoundEvents.CHICKEN_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F);
         }

         this.cancelDrop = true;
      }
   }

   @Redirect(
      method = {"tick"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;levelEvent(ILnet/minecraft/core/BlockPos;I)V"
      )
   )
   private void silenceAnvilLandEvent(Level level, int eventId, BlockPos pos, int data) {
      if (!this.dingdongji$isJiAnvil) {
         level.levelEvent(eventId, pos, data);
      }
   }
}
