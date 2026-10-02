package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModItems;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
   targets = {"dev.anvilcraft.pigsplus.inventory.ChainSmithingMenu"}
)
public abstract class ChainSmithingMenuMixin {
   @Redirect(
      method = {"findAndRemoveUsedSlots"},
      at = @At(
         value = "INVOKE",
         target = "Ljava/util/List;remove(I)Ljava/lang/Object;",
         ordinal = 0
      ),
      remap = false
   )
   private Object redirectTemplatesRemove(List<ItemStack> list, int index) {
      ItemStack stack = list.get(index);
      if (stack.is((Item)ModItems.CREATE_TEMPLATE.get())) {
         return stack;
      }
      return list.remove(index);
   }

   // mayPickup 的模板清点（hasSelectedTemplatesForRecipes -> removeMatchingTemplate）：
   // 每个被选配方都要从模板副本里删一个匹配模板，创造模板只放1个时链到第2级就清点失败、成品取不出。
   // 对创造模板跳过删除，让一个模板通过所有阶段的清点；普通模板逻辑不变。
   @Redirect(
      method = {"removeMatchingTemplate"},
      at = @At(
         value = "INVOKE",
         target = "Ljava/util/List;remove(I)Ljava/lang/Object;",
         ordinal = 0
      ),
      remap = false
   )
   private static Object redirectMatchingRemove(List<ItemStack> list, int index) {
      ItemStack stack = list.get(index);
      if (stack.is((Item)ModItems.CREATE_TEMPLATE.get())) {
         return stack;
      }
      return list.remove(index);
   }
}
