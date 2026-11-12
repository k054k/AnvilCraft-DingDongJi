package com.dingdongji.mod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

// 附魔重金块：附魔能力 6.0 = 本体附魔金块（3.0）的 2 倍
public class EnchantedHeavyGoldBlock extends Block {
   public EnchantedHeavyGoldBlock(Properties properties) {
      super(properties);
   }

   public float getEnchantPowerBonus(BlockState state, LevelReader level, BlockPos pos) {
      return 6.0F;
   }
}
