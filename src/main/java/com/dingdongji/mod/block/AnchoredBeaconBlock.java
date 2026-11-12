package com.dingdongji.mod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// 铭化信标：与腐化信标相反，高明度蓝偏一点点黄纯色锥形光柱；获取方式为附魔重质金块/本体附魔金块底座转化（见 BeaconBlockEntityMixin）
public class AnchoredBeaconBlock extends BeaconBlock {
   public AnchoredBeaconBlock(Properties properties) {
      super(properties);
   }

   @Override
   public Integer getBeaconColorMultiplier(BlockState state, LevelReader level, BlockPos pos, BlockPos beaconPos) {
      return 0x9EE0FF;
   }

   /** 同腐化信标：铭化信标没有信标界面，右键无任何反应。 */
   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      return InteractionResult.PASS;
   }
}
