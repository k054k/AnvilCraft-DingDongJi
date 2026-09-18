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
   @Redirect(
      method = {"findAndRemoveUsedSlots"},
      at = @At(
         value = "INVOKE",
         target = "Ljava/util/List;remove(Ljava/lang/Object;)Z",
         ordinal = 0
      ),
      remap = false
   )
   private boolean redirectTemplateSlotRemove(List<Integer> list, Object o) {
      if (o instanceof Integer slotIndex) {
         ItemStack stack = ((ItemCombinerMenu)(Object)this).getSlot(slotIndex).getItem();
         if (stack.is((Item)ModItems.CREATE_TEMPLATE.get())) {
            return false;
         }
      }

      return list.remove(o);
   }
}
