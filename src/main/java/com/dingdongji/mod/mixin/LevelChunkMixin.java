package com.dingdongji.mod.mixin;

import com.dingdongji.mod.util.BeamLockTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 铭化信标光柱锁定方块的状态冻结（服务端权威）。
 *
 * <p>仅取消右键/挖掘事件无法阻止红石传导、活塞推动、液体流动等引起的
 * “非玩家交互”方块状态变化（这些走 {@link Level#setBlock} 最终进入区块写状态）。
 * 因此在区块写状态的唯一底层入口拦截：被光柱锁定的位置，除变为 AIR（玩家挖掘）
 * 外，任何方块状态写入一律取消（返回 null 表示未发生改变），使方块彻底冻结，
 * 直到光柱解除（BeamLockTracker 对账移除锁定）。
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
   @Inject(method = "setBlockState", at = @At("HEAD"), cancellable = true)
   private void dingdongji$freezeBeamLockedBlock(BlockPos pos, BlockState newState, boolean isMoving,
      CallbackInfoReturnable<BlockState> cir) {
      Level level = ((LevelChunk) (Object) this).getLevel();
      if (!(level instanceof ServerLevel)) {
         return;
      }
      if (newState.isAir()) {
         // 方块被挖掉：立即解锁，使玩家能立刻在原位补放方块（否则等 reconcile
         // 的 40 tick 窗口内补放会被下方冻结拦截，表现为放置回弹）。
         BeamLockTracker.unlockNow(level, pos);
         return;
      }
      if (BeamLockTracker.isLocked(level, pos)) {
         cir.setReturnValue(null);
      }
   }
}
