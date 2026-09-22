package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.item.component.PouchCapacityComponent;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 合成返还口袋：带口袋的护腿在工作台合成中被消耗（护腿被移除）时，
 * 在其原合成格遗留一个口袋物品，类似蛋糕配方返还铁桶。
 * <p>
 * 注入点为所有合成剩余物品的唯一收口
 * {@link RecipeManager#getRemainingItemsFor}（ResultSlot#onTake 即调用此方法）：
 * RETURN 时遍历输入，发现带 {@link PouchCapacityComponent} 的 stack，
 * 按组件容量返还对应口袋（容量 6 → 小口袋；≥12 → 深口袋）。
 * 仅在该位置原 remainder 为空时填充，不覆盖配方自身的返还物。
 * <p>
 * 对本模组的 {@code PouchLeggingsRecipe}（单独一个带口袋护腿 → 裸护腿）同样生效：
 * 结果格得到裸护腿、输入格遗留口袋，实现真正的"口袋与护腿分离"。
 */
@Mixin(RecipeManager.class)
public class MixinCraftingPouchRemainder {

   @Inject(
      method = "getRemainingItemsFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Lnet/minecraft/core/NonNullList;",
      at = @At("RETURN"),
      require = 1
   )
   private void ddj$returnPouch(
      RecipeType<?> recipeType, RecipeInput input, Level level,
      CallbackInfoReturnable<NonNullList<ItemStack>> cir
   ) {
      if (recipeType != RecipeType.CRAFTING) {
         return;
      }
      NonNullList<ItemStack> remainders = cir.getReturnValue();
      int count = Math.min(input.size(), remainders.size());
      for (int i = 0; i < count; i++) {
         if (!remainders.get(i).isEmpty()) {
            continue;
         }
         ItemStack material = input.getItem(i);
         PouchCapacityComponent pouch = material.get(ModComponents.POUCH_CAPACITY.get());
         if (pouch == null) {
            continue;
         }
         Item returned = pouch.capacity() >= 12 ? ModItems.BIG_POUCH.get() : ModItems.SMALL_POUCH.get();
         remainders.set(i, new ItemStack(returned));
      }
   }
}
