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

   // 1.21.1 落地放置实际调用的是 Level#setBlock(BlockPos, BlockState, int)（原版 flags=3），
   // 鸡安铁砧改用 flags=18（同步客户端但不触发邻居更新）
   @Redirect(
      method = {"tick"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
      )
   )
   private boolean silenceAnvilPlaceSound(Level level, BlockPos pos, BlockState state, int flags) {
      return level.setBlock(pos, state, this.dingdongji$isJiAnvil ? 18 : flags);
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
      }
   }

   // 注：鸡安铁砧的落地哐当声无需额外屏蔽——JiAnvilBlock#onLand 为空实现，
   // AnvilBlock 内的 levelEvent(1031) 本来就不会执行。
}
