package com.dingdongji.mod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

// 诅咒重质金块：附魔能力 -2.0（负值降低附魔台等级，与附魔重质金块的 +6.0 相反）
public class CursedHeavyGoldBlock extends Block {
   public CursedHeavyGoldBlock(Properties properties) {
      super(properties);
   }

   public float getEnchantPowerBonus(BlockState state, LevelReader level, BlockPos pos) {
      return -2.0F;
   }
}
