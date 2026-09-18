package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SmithingMenu.class})
public abstract class SmithingMenuMixin {
   @Unique
   private ItemStack dingdongji$savedCreateTemplate = ItemStack.EMPTY;

   @Inject(
      method = {"onTake"},
      at = {@At("HEAD")}
   )
   private void dingdongji$onTakeHead(Player player, ItemStack stack, CallbackInfo ci) {
      SmithingMenu self = (SmithingMenu)(Object)this;
      ItemStack template = self.getSlot(0).getItem();
      this.dingdongji$savedCreateTemplate = ModItems.isCreateTemplate(template) ? template.copy() : ItemStack.EMPTY;
   }

   @Inject(
      method = {"onTake"},
      at = {@At("TAIL")}
   )
   private void dingdongji$onTakeTail(Player player, ItemStack stack, CallbackInfo ci) {
      if (!this.dingdongji$savedCreateTemplate.isEmpty()) {
         SmithingMenu self = (SmithingMenu)(Object)this;
         self.getSlot(0).set(this.dingdongji$savedCreateTemplate.copy());
      }

      this.dingdongji$savedCreateTemplate = ItemStack.EMPTY;
   }
}
