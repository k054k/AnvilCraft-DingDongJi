package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModItems;
import java.util.List;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
   targets = {"dev.anvilcraft.pigsplus.inventory.ChainSmithingMenu"}
)
public abstract class ChainSmithingMenuMixin {
   // findAndRemoveUsedSlots 内实际只有 List#remove(int)（按下标删），没有 List#remove(Object)，
   // 因此不写 ordinal，重定向该方法内全部 remove(int) 调用点
   @Redirect(
      method = {"findAndRemoveUsedSlots"},
      at = @At(
         value = "INVOKE",
         target = "Ljava/util/List;remove(I)Ljava/lang/Object;"
      ),
      remap = false
   )
   private Object redirectTemplateSlotRemove(List<Integer> list, int index) {
      // remove(int) 的参数是待移除列表的下标，真正的菜单槽位号要先取出
      Integer slotIndex = list.get(index);
      if (slotIndex != null) {
         ItemStack stack = ((ItemCombinerMenu)(Object)this).getSlot(slotIndex).getItem();
         if (stack.is((Item)ModItems.CREATE_TEMPLATE.get())) {
            // 万能模板不消耗：不执行删除，返回该位置元素以匹配原方法返回值语义
            return slotIndex;
         }
      }

      return list.remove(index);
   }
}
