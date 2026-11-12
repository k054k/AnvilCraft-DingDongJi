package com.dingdongji.mod.integration.jei;

import com.dingdongji.mod.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemInjectRecipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;

/**
 * “充能中子锭压入过冷浮霜金属块”超限合金转化的 JEI 展示配方。
 *
 * <p>运行时转化由 {@code SupercooledTranscendiumBehavior} 实现（与本体超温余烬分支
 * 完全一致），此处仅为 JEI item_inject 分类提供<b>展示数据</b>，不注册进存档配方管理器。
 *
 * <p>逐档镜像本体 {@code ItemInjectCategory.getTranscendiumRecipes()}（底座原本是
 * 超温余烬金属块），唯一区别是底座换为过冷浮霜金属块；{@link RecipeHolder} 的 id
 * path 必须保持 {@code item_inject/transcendium_N} 前缀，本体分类据此识别 tier 并
 * 渲染多产物槽与“附魔条目数”提示。
 */
public final class SupercooledTranscendiumJeiRecipes {

   private SupercooledTranscendiumJeiRecipes() {
   }

   public static List<RecipeHolder<ItemInjectRecipe>> getAll() {
      Block inputBlock = (Block) ModBlocks.SUPERCOOLED_FROST_METAL_BLOCK.get();
      Item chargedNeutronium = acItem("charged_neutronium_ingot");
      Item neutronium = acItem("neutronium_ingot");
      Item transcendiumIngot = acItem("transcendium_ingot");
      Item transcendiumNugget = acItem("transcendium_nugget");
      Block transcendiumBlock = acBlock("transcendium_block");

      List<RecipeHolder<ItemInjectRecipe>> recipes = new ArrayList<>(5);
      for (int tier = 0; tier < 5; tier++) {
         ItemInjectRecipe.Builder builder = ItemInjectRecipe.builder()
            .inputBlock(inputBlock)
            .requires(chargedNeutronium);
         switch (tier) {
            case 0 -> builder.result(transcendiumIngot, 4);
            case 1 -> {
               builder.result(neutronium);
               builder.result(transcendiumIngot, 4);
               builder.result(transcendiumNugget, 3);
            }
            case 2 -> {
               builder.result(neutronium);
               builder.result(transcendiumIngot, 4);
               builder.result(transcendiumNugget, 33);
            }
            case 3 -> {
               builder.result(neutronium);
               builder.resultBlock(transcendiumBlock);
            }
            default -> {
               builder.result(neutronium);
               builder.result(transcendiumNugget, 16);
               builder.resultBlock(transcendiumBlock);
            }
         }
         ItemInjectRecipe recipe = builder.buildRecipe();
         ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
            "dingdongji", "item_inject/transcendium_" + tier);
         recipes.add(new RecipeHolder<>(id, recipe));
      }
      return recipes;
   }

   private static Item acItem(String path) {
      return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("anvilcraft", path));
   }

   private static Block acBlock(String path) {
      return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("anvilcraft", path));
   }
}
