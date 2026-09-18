package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.component.CreateTemplateMode;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ItemStack.class})
public abstract class ItemStackMixin {
   @Inject(
      method = {"is(Lnet/minecraft/world/item/Item;)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onIsItem(Item item, CallbackInfoReturnable<Boolean> cir) {
      ItemStack self = (ItemStack)(Object)this;
      ResourceLocation targetId = BuiltInRegistries.ITEM.getKey(item);
      if (targetId.equals(ResourceLocation.parse("anvilcraft:eight_to_one_smithing_template"))) {
         ResourceLocation selfId = BuiltInRegistries.ITEM.getKey(self.getItem());
         if (selfId.equals(ResourceLocation.parse("dingdongji:create_template"))) {
            CreateTemplateMode mode = (CreateTemplateMode)self.get((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get());
            if (mode != null && "delta".equals(mode.mode())) {
               cir.setReturnValue(true);
            }
         }
      }
   }
}
