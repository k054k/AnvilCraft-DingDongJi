package com.dingdongji.mod.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 过冷的浮霜金属块物品：过冷态无法以物品形式稳定保存。
 *
 * <p>生存/冒险模式手持放置时，真正落到世界中的是本体常温浮霜金属块
 * （{@code anvilcraft:frost_metal_block}），即"温度不随物品保留，
 * 重新放下即恢复常温"。创造模式放置则保留真正的过冷方块（方便测试与建造）。
 * 破坏过冷方块仍掉落本该物品（见其掉落表）。
 */
public class SupercooledFrostMetalBlockItem extends BlockItem {
   private static final ResourceLocation NORMAL_FROST_METAL_BLOCK =
      ResourceLocation.fromNamespaceAndPath("anvilcraft", "frost_metal_block");

   private static Block normalFrostBlock;

   public SupercooledFrostMetalBlockItem(Block block, Properties properties) {
      super(block, properties);
   }

   @Override
   protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
      Player player = context.getPlayer();
      if (player != null && player.isCreative()) {
         return super.placeBlock(context, state);
      }
      return super.placeBlock(context, normalFrostBlock().defaultBlockState());
   }

   private static Block normalFrostBlock() {
      if (normalFrostBlock == null) {
         normalFrostBlock = BuiltInRegistries.BLOCK.get(NORMAL_FROST_METAL_BLOCK);
      }
      return normalFrostBlock;
   }
}
