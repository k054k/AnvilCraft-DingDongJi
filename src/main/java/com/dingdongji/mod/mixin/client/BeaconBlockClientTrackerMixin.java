package com.dingdongji.mod.mixin.client;

import com.dingdongji.mod.client.ClientBeaconTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BeaconBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 客户端信标位置记录：玩家右键信标时记录坐标（见 ClientBeaconTracker），
 * 供 BeaconConfirmButtonMixin 在客户端判定 4 层免锭底座。
 */
@Mixin(BeaconBlock.class)
public class BeaconBlockClientTrackerMixin {

   @Inject(
      method = "useWithoutItem(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
      at = @At("HEAD")
   )
   private void ddj$recordBeaconPos(
      net.minecraft.world.level.block.state.BlockState state,
      net.minecraft.world.level.Level level,
      BlockPos pos,
      net.minecraft.world.entity.player.Player player,
      net.minecraft.world.phys.BlockHitResult hitResult,
      CallbackInfoReturnable<net.minecraft.world.InteractionResult> cir
   ) {
      if (level.isClientSide) {
         ClientBeaconTracker.record(pos);
      }
   }
}
