package com.dingdongji.mod.integration.jei;

import com.dingdongji.mod.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.jei.recipe.BeaconConversionRecipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * 信标转化的 JEI 展示配方（数据载体，非真实配方）：继承本体
 * {@link BeaconConversionRecipe}，借助其 cursedGoldBlockLayers/Count
 * 驱动本体分类的槽位数量与 3D 金字塔绘制；底座/产物/金锭由
 * DdjBeaconConversionCategoryMixin 按本体同款坐标布槽。
 *
 * <p>分类内顺序（本体的 4 条诅咒金块→腐化信标配方由本体插件注册，恒在最前）：
 * <ol>
 *   <li>重质诅咒金块金字塔 → 腐化信标：1~3 层铁砧 + 诅咒金锭（18%/45%/100%，
 *       金锭必消耗），4 层放上即刻转化（免铁砧免金锭）；</li>
 *   <li>本体附魔金块金字塔 → 铭化信标：1~4 层铁砧（2%/5%/20%/100%），
 *       见 EnchantedBeaconConversionBehavior；</li>
 *   <li>重质附魔金块金字塔 → 铭化信标：1~3 层铁砧（18%/45%/100%），4 层即刻。</li>
 * </ol>
 */
public final class DdjBeaconConversionRecipe extends BeaconConversionRecipe {
   public final ItemStack ingot;
   public final ItemStack base;
   public final ItemStack result;
   /** true = 铁砧砸落概率转化（显示金锭槽）；false = 放置自动转化。 */
   public final boolean anvilTriggered;

   private DdjBeaconConversionRecipe(ItemStack ingot, Block base, int layers, float chance, ItemStack result, boolean anvilTriggered) {
      super(layers, chance);
      this.ingot = ingot;
      this.base = new ItemStack(base, this.cursedGoldBlockCount);
      this.result = result;
      this.anvilTriggered = anvilTriggered;
   }

   public static List<BeaconConversionRecipe> getAll() {
      List<BeaconConversionRecipe> recipes = new ArrayList<>();
      Block enchantedGold = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("anvilcraft", "enchanted_gold_block"));
      Item enchantedGoldIngot = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("anvilcraft", "enchanted_gold_ingot"));
      Item cursedGoldIngot = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("anvilcraft", "cursed_gold_ingot"));
      ItemStack inscribedBeacon = new ItemStack(ModBlocks.ANCHORED_BEACON.get());
      ItemStack corruptedBeacon = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("anvilcraft", "corrupted_beacon")));
      Block cursedHeavy = (Block) ModBlocks.CURSED_HEAVY_GOLD_BLOCK.get();
      Block enchantedHeavy = (Block) ModBlocks.ENCHANTED_HEAVY_GOLD_BLOCK.get();

      float[] enchantedChances = {0.02F, 0.05F, 0.2F, 1.0F};
      float[] heavyChances = {0.18F, 0.45F, 1.0F};
      // 重质诅咒金块 → 腐化信标：1~3 层铁砧 + 诅咒金锭（18%/45%/100%），4 层即刻
      for (int layers = 1; layers <= 3; layers++) {
         recipes.add(new DdjBeaconConversionRecipe(
            new ItemStack(cursedGoldIngot), cursedHeavy, layers, heavyChances[layers - 1], corruptedBeacon.copy(), true
         ));
      }
      recipes.add(new DdjBeaconConversionRecipe(
         ItemStack.EMPTY, cursedHeavy, 4, 1.0F, corruptedBeacon.copy(), false
      ));
      // 本体附魔金块 → 铭化信标：1~4 层全部走铁砧
      for (int layers = 1; layers <= 4; layers++) {
         recipes.add(new DdjBeaconConversionRecipe(
            new ItemStack(enchantedGoldIngot), enchantedGold, layers, enchantedChances[layers - 1], inscribedBeacon.copy(), true
         ));
      }
      // 重质附魔金块 → 铭化信标：1~3 层铁砧 + 附魔金锭（18%/45%/100%），4 层即刻
      for (int layers = 1; layers <= 3; layers++) {
         recipes.add(new DdjBeaconConversionRecipe(
            new ItemStack(enchantedGoldIngot), enchantedHeavy, layers, heavyChances[layers - 1], inscribedBeacon.copy(), true
         ));
      }
      recipes.add(new DdjBeaconConversionRecipe(
         ItemStack.EMPTY, enchantedHeavy, 4, 1.0F, inscribedBeacon.copy(), false
      ));
      return recipes;
   }
}
