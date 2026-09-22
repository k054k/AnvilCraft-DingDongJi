package com.dingdongji.mod.recipe;

import com.dingdongji.mod.init.ModRecipes;
import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.item.component.PouchCapacityComponent;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * 护腿 + 口袋 → 带口袋的护腿（保留原有所有组件）。
 * <p>
 * 匹配条件：恰好一个护腿（腿护甲）+ 恰好一个口袋物品。
 * 输出：复制原护腿，追加 {@link PouchCapacityComponent} 组件（绝对容量），
 * 附魔、耐久、其他自定义组件全部保留。
 * <p>
 * 铁砧本体的护腿（编译期不在类路径）用注册 ID 字符串识别：
 * <ul>
 *   <li>普通护腿 + 小口袋 → 6 栏</li>
 *   <li>普通护腿 + 大口袋 → 12 栏</li>
 *   <li>anvilcraft:pockets_leggings（基础 6）+ 大口袋 → 12 栏（翻倍）</li>
 *   <li>anvilcraft:weatherproof_spacesuit_leggings（基础 12）+ 大口袋 → 24 栏（翻倍）</li>
 * </ul>
 * 铁砧本体护腿 + 小口袋不叠加（基础容量已 ≥ 小口袋）。
 * <p>
 * 拆除分支：网格中单独放入一个带口袋组件的护腿，结果格得到移除口袋组件的同款护腿
 * （附魔、耐久、其他组件全部保留）；输入格同时由 {@code MixinCraftingPouchRemainder}
 * 遗留一个对应口袋物品，即"口袋与护腿分离"。工作台和 2×2 合成格均生效。
 */
public class PouchLeggingsRecipe implements CraftingRecipe {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final CraftingBookCategory category;

   public PouchLeggingsRecipe(CraftingBookCategory category) {
      this.category = category;
   }

   @Override
   public CraftingBookCategory category() {
      return this.category;
   }

   @Override
   public boolean matches(CraftingInput input, Level level) {
      int nonEmptyCount = 0;
      int leggingsCount = 0;
      int pouchCount = 0;
      ItemStack loneLeggings = ItemStack.EMPTY;
      for (int i = 0; i < input.size(); i++) {
         ItemStack stack = input.getItem(i);
         if (stack.isEmpty()) continue;
         nonEmptyCount++;
         if (isLegArmor(stack)) {
            leggingsCount++;
            loneLeggings = stack;
         } else if (isPouch(stack)) {
            pouchCount++;
         } else {
            return false;
         }
      }
      // 合成分支：护腿 + 口袋；拆除分支：单独一个带口袋组件的护腿
      return leggingsCount == 1 && pouchCount == 1
         || nonEmptyCount == 1 && leggingsCount == 1 && hasPouchComponent(loneLeggings);
   }

   @Override
   public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
      ItemStack leggings = ItemStack.EMPTY;
      ItemStack pouch = ItemStack.EMPTY;
      for (int i = 0; i < input.size(); i++) {
         ItemStack stack = input.getItem(i);
         if (stack.isEmpty()) continue;
         if (isLegArmor(stack)) leggings = stack;
         else if (isPouch(stack)) pouch = stack;
      }
      if (leggings.isEmpty() || pouch.isEmpty()) {
         // 拆除分支：单独一个带口袋组件的护腿（matches 已保证）
         // 结果格为裸护腿；对应口袋由 MixinCraftingPouchRemainder 遗留在输入格
         if (!leggings.isEmpty() && hasPouchComponent(leggings)) {
            ItemStack stripped = leggings.copy();
            stripped.remove((net.minecraft.core.component.DataComponentType) ModComponents.POUCH_CAPACITY.get());
            LOGGER.info(
               "[DingDongJi][口袋] 拆除分支：{} → 移除口袋组件（口袋经合成返还遗留）",
               BuiltInRegistries.ITEM.getKey(leggings.getItem())
            );
            return stripped;
         }
         return ItemStack.EMPTY;
      }

      ItemStack result = leggings.copy();
      boolean isBigPouch = pouch.is(ModItems.BIG_POUCH.get());
      // 口袋容量固定：小口袋+6，大口袋+12。基础容量由 AnvilCraftCompat.getPocketCapacity 叠加。
      int capacity = isBigPouch ? 12 : 6;
      result.set(
         (net.minecraft.core.component.DataComponentType) ModComponents.POUCH_CAPACITY.get(),
         new PouchCapacityComponent(capacity)
      );
      LOGGER.info(
         "[DingDongJi][口袋] 合成分支：护腿={} + {} → 口袋组件容量={}",
         BuiltInRegistries.ITEM.getKey(leggings.getItem()),
         BuiltInRegistries.ITEM.getKey(pouch.getItem()),
         capacity
      );
      return result;
   }

   @Override
   public boolean canCraftInDimensions(int width, int height) {
      return width >= 2 && height >= 2;
   }

   @Override
   public ItemStack getResultItem(HolderLookup.Provider registries) {
      return ItemStack.EMPTY;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return ModRecipes.POUCH_LEGGINGS_SERIALIZER.get();
   }

   private static boolean isLegArmor(ItemStack stack) {
      return stack.getItem() instanceof ArmorItem armor
         && armor.getType().getSlot() == EquipmentSlot.LEGS;
   }

   private static boolean isPouch(ItemStack stack) {
      return stack.is(ModItems.SMALL_POUCH.get())
         || stack.is(ModItems.BIG_POUCH.get());
   }

   private static boolean hasPouchComponent(ItemStack stack) {
      return !stack.isEmpty()
         && stack.has((net.minecraft.core.component.DataComponentType) ModComponents.POUCH_CAPACITY.get());
   }
}
