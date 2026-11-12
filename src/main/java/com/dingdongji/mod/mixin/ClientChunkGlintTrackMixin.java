package com.dingdongji.mod.mixin;

import com.dingdongji.mod.block.ModBlocks;
import com.dingdongji.mod.client.EnchantedHeavyGoldGlints;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 客户端：附魔重质金块写入区块的同帧即收录进光效位置集合——放置预测与服务端
 * 同步都走 ClientLevel → LevelChunk.setBlockState，本帧渲染即可见附魔光效。
 * （仅客户端 mixin，不引用任何服务端不存在的类。）
 */
@Mixin(LevelChunk.class)
public abstract class ClientChunkGlintTrackMixin {
   @Inject(method = "setBlockState", at = @At("HEAD"))
   private void ddj$trackHeavyGoldPlacement(
      BlockPos pos, BlockState newState, boolean isMoving, CallbackInfoReturnable<BlockState> cir
   ) {
      if (((LevelChunk) (Object) this).getLevel() instanceof ClientLevel clientLevel
         && newState.is(ModBlocks.ENCHANTED_HEAVY_GOLD_BLOCK.get())) {
         EnchantedHeavyGoldGlints.addPosition(clientLevel, pos);
      }
   }
}
