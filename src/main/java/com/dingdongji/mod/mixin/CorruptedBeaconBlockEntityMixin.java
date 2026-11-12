package com.dingdongji.mod.mixin;

import com.dingdongji.mod.block.ModBlocks;
import dev.dubhe.anvilcraft.block.entity.CorruptedBeaconBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 腐化信标基座判定：本体 updateBase 硬编码只认诅咒金块，导致诅咒重质金块
 * 基座不发光柱。这里把那次 is(CURSED_GOLD_BLOCK) 判定扩展为
 * "诅咒金块 或 诅咒重质金块"，其余逻辑保持原版。
 */
@Mixin(value = CorruptedBeaconBlockEntity.class, remap = false)
public abstract class CorruptedBeaconBlockEntityMixin {
   @Redirect(
      method = "updateBase(Lnet/minecraft/world/level/Level;III)I",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"
      ),
      require = 1
   )
   private static boolean dingdongji$cursedHeavyGoldBase(BlockState state, Block block) {
      return state.is(block) || state.is((Block) ModBlocks.CURSED_HEAVY_GOLD_BLOCK.get());
   }
}
