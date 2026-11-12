package com.dingdongji.mod.recipe;

import com.dingdongji.mod.init.ModRecipes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import java.util.List;

/**
 * "铭化信标内的生物转化"配方（独立注册，不复用本体腐化信标的 mob_transform 配方）。
 *
 * <p>铭化信标是腐化信标的逆过程：光柱内的"被腐化生物"（输入）被确定性转回
 * "转化前的原生物"（输出）。每个配方可指定多个输入实体（inputs 列表），
 * 运行时只要命中其中任一即转化；概率恒为 100%（无概率字段）。
 *
 * <p>数据包可自由添加生物；双方均可指定手持物品（与本体 mob_transform_with_item
 * 的展示/生成逻辑一致）：
 * <pre>
 * {
 *   "type": "dingdongji:anchored_mob_transform",
 *   "inputs": ["minecraft:pillager", "minecraft:vindicator", "minecraft:evoker"],
 *   "input_item": { "id": "anvilcraft:giant_anvil", "count": 1 },
 *   "result": "minecraft:zombie",
 *   "result_item": { "id": "minecraft:anvil", "count": 1 }
 * }
 * </pre>
 * 两个物品字段均可省略（默认为空手）。{@code result_item} 会在运行时转化
 * 生成的生物主手上；物品字段仅用于 JEI 展示与生成物手持，不参与转化条件。
 *
 * 运行时由 {@code BeaconBlockEntityMixin.dingdongji$reverseTransformEntity} 查询；
 * JEI 由 {@code AnchoredMobTransformCategory} 展示（多输入时循环切换显示）。
 */
public final class AnchoredMobTransformRecipe implements Recipe<RecipeInput> {

   public static final MapCodec<AnchoredMobTransformRecipe> CODEC = RecordCodecBuilder.mapCodec(inst ->
      inst.group(
         BuiltInRegistries.ENTITY_TYPE.byNameCodec().listOf().fieldOf("inputs").forGetter(AnchoredMobTransformRecipe::inputs),
         ItemStack.CODEC.optionalFieldOf("input_item", ItemStack.EMPTY).forGetter(AnchoredMobTransformRecipe::inputItem),
         BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("result").forGetter(AnchoredMobTransformRecipe::result),
         ItemStack.CODEC.optionalFieldOf("result_item", ItemStack.EMPTY).forGetter(AnchoredMobTransformRecipe::resultItem),
         // 可选的手持物品条件：非空时仅当输入实体主手持有该物品才转化
         // （用于区分本体三条僵尸→巨人的逆向：巨型铁砧/巨型独石核心/巨型独石线）
         ItemStack.CODEC.optionalFieldOf("input_held", ItemStack.EMPTY).forGetter(AnchoredMobTransformRecipe::inputHeld),
         // 可选的多变体：同一输入实体按手持物品区分结果手持（如巨人的三条逆向）
         Variant.CODEC.codec().listOf().optionalFieldOf("variants", List.of()).forGetter(AnchoredMobTransformRecipe::variants)
      ).apply(inst, AnchoredMobTransformRecipe::new)
   );

   public static final StreamCodec<RegistryFriendlyByteBuf, AnchoredMobTransformRecipe> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.registry(Registries.ENTITY_TYPE).apply(ByteBufCodecs.list()), AnchoredMobTransformRecipe::inputs,
      ItemStack.OPTIONAL_STREAM_CODEC, AnchoredMobTransformRecipe::inputItem,
      ByteBufCodecs.registry(Registries.ENTITY_TYPE), AnchoredMobTransformRecipe::result,
      ItemStack.OPTIONAL_STREAM_CODEC, AnchoredMobTransformRecipe::resultItem,
      ItemStack.OPTIONAL_STREAM_CODEC, AnchoredMobTransformRecipe::inputHeld,
      ByteBufCodecs.collection(java.util.ArrayList::new, Variant.STREAM_CODEC), AnchoredMobTransformRecipe::variants,
      AnchoredMobTransformRecipe::new
   );

   private final List<EntityType<?>> inputs;
   private final ItemStack inputItem;
   private final EntityType<?> result;
   private final ItemStack resultItem;
   /** 可选的手持物品条件（空 = 不限制）。 */
   private final ItemStack inputHeld;
   /** 可选的多变体列表（空 = 单一结果）。 */
   private final List<Variant> variants;

   public AnchoredMobTransformRecipe(List<EntityType<?>> inputs, ItemStack inputItem, EntityType<?> result, ItemStack resultItem, ItemStack inputHeld, List<Variant> variants) {
      this.inputs = inputs;
      this.inputItem = inputItem;
      this.result = result;
      this.resultItem = resultItem;
      this.inputHeld = inputHeld;
      this.variants = variants;
   }

   /** 同一配方的按手持区分的变体：手持与 input_held 匹配时，生成物改持 result_item。 */
   public record Variant(ItemStack inputHeld, ItemStack resultItem) {
      public static final MapCodec<Variant> CODEC = RecordCodecBuilder.mapCodec(inst ->
         inst.group(
            ItemStack.CODEC.fieldOf("input_held").forGetter(Variant::inputHeld),
            ItemStack.CODEC.optionalFieldOf("result_item", ItemStack.EMPTY).forGetter(Variant::resultItem)
         ).apply(inst, Variant::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, Variant> STREAM_CODEC = StreamCodec.composite(
         ItemStack.OPTIONAL_STREAM_CODEC, Variant::inputHeld,
         ItemStack.OPTIONAL_STREAM_CODEC, Variant::resultItem,
         Variant::new
      );
   }

   /** 输入实体列表（多个时任一命中即转化）。 */
   public List<EntityType<?>> inputs() {
      return this.inputs;
   }

   /** 兼容旧调用：取第一个输入实体。 */
   public EntityType<?> input() {
      return this.inputs.get(0);
   }

   public ItemStack inputItem() {
      return this.inputItem;
   }

   public EntityType<?> result() {
      return this.result;
   }

   public ItemStack resultItem() {
      return this.resultItem;
   }

   /** 手持物品条件：空 = 匹配任意手持。 */
   public ItemStack inputHeld() {
      return this.inputHeld;
   }

   /** 变体列表（空 = 单一结果）。 */
   public List<Variant> variants() {
      return this.variants;
   }

   /** 运行时判定：手持条件是否满足（按物品类型比对，不比对数量/NBT）。 */
   public boolean matchesHeld(LivingEntity entity) {
      return this.inputHeld.isEmpty() || entity.getMainHandItem().is(this.inputHeld.getItem());
   }

   /**
    * 整体匹配：自身 input_held 条件满足，且（若有变体）至少一个变体的
    * 手持条件命中。
    */
   public boolean matchesRecipe(LivingEntity entity) {
      if (!this.matchesHeld(entity)) {
         return false;
      }
      if (!this.variants.isEmpty()) {
         boolean any = false;
         for (Variant variant : this.variants) {
            if (variant.inputHeld().isEmpty() || entity.getMainHandItem().is(variant.inputHeld().getItem())) {
               any = true;
               break;
            }
         }
         if (!any) {
            return false;
         }
      }
      return true;
   }

   /**
    * 主手命中的变体（无命中返回 null）；生成物手持取
    * {@code selectVariant(entity) != null ? variant.resultItem() : resultItem}。
    */
   public Variant selectVariant(LivingEntity entity) {
      for (Variant variant : this.variants) {
         if (!variant.inputHeld().isEmpty() && entity.getMainHandItem().is(variant.inputHeld().getItem())) {
            return variant;
         }
      }
      return null;
   }

   // --- Recipe<RecipeInput> contract（不进配方书，仅数据载体） ---
   @Override
   public boolean matches(RecipeInput container, Level level) {
      return false;
   }

   @Override
   public ItemStack assemble(RecipeInput container, HolderLookup.Provider registries) {
      return ItemStack.EMPTY;
   }

   @Override
   public boolean canCraftInDimensions(int width, int height) {
      return true;
   }

   @Override
   public ItemStack getResultItem(HolderLookup.Provider registries) {
      return ItemStack.EMPTY;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return ModRecipes.ANCHORED_MOB_TRANSFORM_SERIALIZER.get();
   }

   @Override
   public RecipeType<?> getType() {
      return ModRecipes.ANCHORED_MOB_TRANSFORM_TYPE.get();
   }

   @Override
   public boolean isSpecial() {
      return true;
   }

   public static final class Serializer implements RecipeSerializer<AnchoredMobTransformRecipe> {
      @Override
      public MapCodec<AnchoredMobTransformRecipe> codec() {
         return CODEC;
      }

      @Override
      public StreamCodec<RegistryFriendlyByteBuf, AnchoredMobTransformRecipe> streamCodec() {
         return STREAM_CODEC;
      }
   }
}
