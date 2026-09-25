package com.dingdongji.mod.init;

import com.dingdongji.mod.recipe.ItemPortalConversionRecipe;
import com.dingdongji.mod.recipe.PouchLeggingsRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * DDJ 自定义配方注册。
 * <ul>
 *   <li>{@code RecipeType.CRAFTING} + {@link PouchLeggingsRecipe}（口袋护腿配方）：
 *       复用原版配方类型，只注册 Serializer。</li>
 *   <li>{@code dingdongji:item_portal_conversion} + {@link ItemPortalConversionRecipe}
 *       （物品掉入传送门配方）：独立 RecipeType，用于末地门等传送位置转换幻灵套装。</li>
 * </ul>
 */
public class ModRecipes {

   /** 配方类型注册——原版 CRAFTING / SMELTING 等已由 Minecraft 自带，
    * 这里只注册 DDJ 独有的 item_portal_conversion。 */
   public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
      DeferredRegister.create(Registries.RECIPE_TYPE, "dingdongji");

   /** Serializer 注册：两个自定义配方各占一个条目。 */
   public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
      DeferredRegister.create(Registries.RECIPE_SERIALIZER, "dingdongji");

   // ---------- Item Portal Conversion ----------
   public static final DeferredHolder<RecipeType<?>, RecipeType<ItemPortalConversionRecipe>> ITEM_PORTAL_CONVERSION_TYPE =
      RECIPE_TYPES.register(
         "item_portal_conversion",
         () -> RecipeType.simple(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dingdongji", "item_portal_conversion")
         )
      );

   public static final DeferredHolder<RecipeSerializer<?>, ItemPortalConversionRecipe.Serializer> ITEM_PORTAL_CONVERSION_SERIALIZER =
      RECIPE_SERIALIZERS.register(
         "item_portal_conversion",
         ItemPortalConversionRecipe.Serializer::new
      );

   // ---------- Pouch Leggings（原版 CRAFTING type） ----------
   public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<PouchLeggingsRecipe>> POUCH_LEGGINGS_SERIALIZER =
      RECIPE_SERIALIZERS.register(
         "pouch_leggings",
         () -> new SimpleCraftingRecipeSerializer<>(PouchLeggingsRecipe::new)
      );

   private ModRecipes() {
   }
}
