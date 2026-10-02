package com.dingdongji.mod.recipe;

import com.dingdongji.mod.init.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * "物品掉入传送门"配方：物品 ItemEntity 进入某类传送门时，按几率转化为另一件物品。
 * 与 AnvilCraft 的 block-portal_conversion（方块→方块）平行，区别在于输入/输出都是物品
 * 而非方块状态，输入由 {@link Ingredient} 驱动（可写 item、tag、items 多候选）。
 * <p>
 * JSON 格式（路径 {@code data/dingdongji/recipe/item_portal_conversion/*.json}），
 * 风格对齐 AnvilCraft 的 {@code anvilcraft:portal_conversion}（其 "block" 对应此处 "id"）：
 * <pre>
 * {
 *   "type": "dingdongji:item_portal_conversion",
 *   "input": {
 *     "item": "minecraft:iron_helmet"
 *   },
 *   "portal": "minecraft:end_portal",
 *   "result": {
 *     "id": "dingdongji:spectral_helmet",
 *     "chance": 0.03
 *   }
 * }
 * </pre>
 * <p>
 * 配方本身不实现 CraftingMenu 的匹配（不进原版配方书），触发由 {@link
 * com.dingdongji.mod.event.ModEndGatewayHandler} 在物品传送事件里主动查询。
 */
public final class ItemPortalConversionRecipe implements Recipe<RecipeInput> {

   /**
    * Chance-block: result item + chance (0.0 ~ 1.0). Serialized flat, in the
    * same style as AnvilCraft's block portal_conversion recipes
    * (their {@code "block"} corresponds to our {@code "id"}):
    * <pre>
    * "result": { "id": "dingdongji:spectral_helmet", "chance": 0.03 }
    * </pre>
    * Only a plain item id is needed (count/components never occur here), so the
    * codec resolves the registry id straight to an ItemStack of size 1 instead
    * of nesting a full ItemStack.CODEC object.
    */
   public record Result(ItemStack item, float chance) {
      public static final Codec<Result> CODEC = RecordCodecBuilder.create(inst ->
         inst.group(
            net.minecraft.core.registries.BuiltInRegistries.ITEM.byNameCodec()
               .fieldOf("id")
               .xmap(ItemStack::new, ItemStack::getItem)
               .forGetter(Result::item),
            Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(Result::chance)
         ).apply(inst, Result::new)
      );

      public static final StreamCodec<RegistryFriendlyByteBuf, Result> STREAM_CODEC = StreamCodec.composite(
         ItemStack.STREAM_CODEC, Result::item,
         net.minecraft.network.codec.ByteBufCodecs.FLOAT, Result::chance,
         Result::new
      );

      public static Result empty() { return new Result(ItemStack.EMPTY, 0.0F); }

      public boolean roll(double random) {
         return random < (double)this.chance;
      }
   }

   public static final MapCodec<ItemPortalConversionRecipe> CODEC = RecordCodecBuilder.mapCodec(inst ->
      inst.group(
         Ingredient.CODEC.fieldOf("input").forGetter(ItemPortalConversionRecipe::input),
         Codec.STRING.fieldOf("portal").forGetter(r -> r.portalId.toString()),
         Result.CODEC.fieldOf("result").forGetter(ItemPortalConversionRecipe::result)
      ).apply(inst, (ingr, portalStr, res) ->
         new ItemPortalConversionRecipe(ingr, ResourceLocation.parse(portalStr), res)
      )
   );

   public static final StreamCodec<RegistryFriendlyByteBuf, ItemPortalConversionRecipe> STREAM_CODEC = StreamCodec.composite(
      Ingredient.CONTENTS_STREAM_CODEC, ItemPortalConversionRecipe::input,
      net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.map(ResourceLocation::parse, ResourceLocation::toString),
      ItemPortalConversionRecipe::portalId,
      Result.STREAM_CODEC, ItemPortalConversionRecipe::result,
      ItemPortalConversionRecipe::new
   );

   private final Ingredient input;
   private final ResourceLocation portalId;
   private final Result result;

   public ItemPortalConversionRecipe(Ingredient input, ResourceLocation portalId, Result result) {
      this.input = input;
      this.portalId = portalId;
      this.result = result;
   }

   /** 供服务端 ModEndGatewayHandler 调用：物品是否匹配此配方的输入 Ingredient。 */
   public boolean matchesItem(ItemStack stack) {
      return this.input.test(stack);
   }

   /** 供 ModEndGatewayHandler 调用：目标传送门 id 是否匹配。空字符串表示"所有传送门"。 */
   public boolean matchesPortal(ResourceLocation id) {
      return id == null || this.portalId.equals(id);
   }

   public Ingredient input()       { return this.input; }
   public ResourceLocation portalId() { return this.portalId; }
   public Result result()          { return this.result; }

   // --- Recipe<RecipeInput> contract ------------------------------------------------
   @Override
   public boolean matches(RecipeInput container, Level level) {
      return container.size() > 0
          && container.getItem(0) != null
          && this.input.test(container.getItem(0));
   }

   @Override
   public ItemStack assemble(RecipeInput container, HolderLookup.Provider registries) {
      return this.result.item.copy();
   }

   @Override
   public boolean canCraftInDimensions(int width, int height) { return true; }

   @Override
   public ItemStack getResultItem(HolderLookup.Provider registries) {
      return this.result.item.copy();
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return ModRecipes.ITEM_PORTAL_CONVERSION_SERIALIZER.get();
   }

   @Override
   public RecipeType<?> getType() {
      return ModRecipes.ITEM_PORTAL_CONVERSION_TYPE.get();
   }

   @Override
   public boolean isSpecial() { return true; }

   // --- Serializer -------------------------------------------------------------------
   public static final class Serializer implements RecipeSerializer<ItemPortalConversionRecipe> {
      @Override
      public MapCodec<ItemPortalConversionRecipe> codec() { return CODEC; }

      @Override
      public StreamCodec<RegistryFriendlyByteBuf, ItemPortalConversionRecipe> streamCodec() {
         return STREAM_CODEC; }
   }
}
