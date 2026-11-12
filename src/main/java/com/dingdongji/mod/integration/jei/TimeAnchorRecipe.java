package com.dingdongji.mod.integration.jei;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * "时定"展示配方：激活的铭化信标（附魔金块底座 + 光柱）上方放置某个方块，
 * 该方块的温度倒计时即被凝滞。仅为 JEI 展示数据；运行时可时定方块以数据包
 * 标签 {@code dingdongji:time_anchorable} 为唯一白名单，二者内容保持一致。
 *
 * 收录本体全部带持续时间（温度等级会随时间冷却）的方块：
 * 钨块与下界合金块各自的 灼热/炽白/赤红/白热 四个温度等级。
 */
public record TimeAnchorRecipe(ResourceLocation id, Block block) {

   /** (材料, 四个温度等级)，顺序即 JEI 展示顺序。 */
   private static final String[][][] TEMPERATURE_BLOCKS = {
      {
         {"anvilcraft", "heated_tungsten_block"},
         {"anvilcraft", "glowing_tungsten_block"},
         {"anvilcraft", "redhot_tungsten_block"},
         {"anvilcraft", "incandescent_tungsten_block"}
      },
      {
         {"anvilcraft", "heated_netherite_block"},
         {"anvilcraft", "glowing_netherite_block"},
         {"anvilcraft", "redhot_netherite_block"},
         {"anvilcraft", "incandescent_netherite_block"}
      },
      // 无温度等级的持续时长方块：各一行（单元素组）
      {
         {"anvilcraft", "overheated_ember_metal_block"}
      },
      {
         {"dingdongji", "supercooled_frost_metal_block"}
      }
   };

   public static List<TimeAnchorRecipe> getAll() {
      List<TimeAnchorRecipe> recipes = new ArrayList<>();
      for (String[][] group : TEMPERATURE_BLOCKS) {
         for (String[] parts : group) {
            Block block = BuiltInRegistries.BLOCK.get(
               ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]));
            recipes.add(new TimeAnchorRecipe(
               ResourceLocation.fromNamespaceAndPath("dingdongji", "time_anchor/" + parts[1]),
               block
            ));
         }
      }
      return recipes;
   }

   public ItemStack stack() {
      return new ItemStack(this.block);
   }

   public net.minecraft.world.level.block.state.BlockState state() {
      return this.block.defaultBlockState();
   }
}
