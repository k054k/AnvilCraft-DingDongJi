package com.dingdongji.mod.init;

import com.dingdongji.mod.recipe.PouchLeggingsRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * DDJ 自定义配方注册。
 * <p>
 * 口袋护腿配方复用原版 crafting 类型（{@link RecipeType#CRAFTING}，由
 * CraftingRecipe#getType 默认提供），故无需自定义 RecipeType；只需注册
 * 一个 {@link SimpleCraftingRecipeSerializer}，JSON 中 type 指向它。
 */
public class ModRecipes {
   public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
      DeferredRegister.create(Registries.RECIPE_SERIALIZER, "dingdongji");

   // Factory#create(CraftingBookCategory) ↔ PouchLeggingsRecipe(CraftingBookCategory)
   public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<PouchLeggingsRecipe>> POUCH_LEGGINGS_SERIALIZER =
      RECIPE_SERIALIZERS.register(
         "pouch_leggings",
         () -> new SimpleCraftingRecipeSerializer<>(PouchLeggingsRecipe::new)
      );

   private ModRecipes() {
   }
}
