package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.item.component.CreateTemplateMode;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   targets = {"dev.dubhe.anvilcraft.inventory.EmberSmithingMenu"}
)
public abstract class EmberSmithingMenuMixin {
   @Redirect(
      method = {"*"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/core/Holder;)Z"
      )
   )
   private boolean redirectIsHolder(ItemStack stack, Holder<Item> holder) {
      boolean original = stack.getItem() == holder.value();
      if (!original) {
         ResourceLocation holderId = BuiltInRegistries.ITEM.getKey((Item)holder.value());
         if (holderId.equals(ResourceLocation.parse("anvilcraft:eight_to_one_smithing_template")) && ModItems.isCreateTemplate(stack)) {
            CreateTemplateMode mode = (CreateTemplateMode)stack.get((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get());
            if (mode != null && "delta".equals(mode.mode())) {
               return true;
            }
         }
      }

      return original;
   }

   @Inject(
      method = {"isUsableTemplate"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void ddj$usableTemplate(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
      if (ModItems.isCreateTemplate(stack)) {
         cir.setReturnValue(true);
      }
   }

   @Inject(
      method = {"getInputSize"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void onGetInputSize(CallbackInfoReturnable<Integer> cir) {
      ItemStack t = ((AbstractContainerMenu)(Object)this).getSlot(0).getItem();
      if (t.is((Item)ModItems.CREATE_TEMPLATE.get())) {
         CreateTemplateMode m = (CreateTemplateMode)t.get((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get());
         if (m != null) {
            String var4 = m.mode();

            cir.setReturnValue(switch (var4) {
               case "delta" -> 8;
               case "gamma" -> 4;
               case "beta" -> 2;
               default -> 0;
            });
         }
      }
   }

   @Inject(
      method = {"canCreateResult"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void onCanCreateResult(CallbackInfoReturnable<Boolean> cir) {
      AbstractContainerMenu self = (AbstractContainerMenu)(Object)this;
      ItemStack t = self.getSlot(0).getItem();
      if (t.is((Item)ModItems.CREATE_TEMPLATE.get())) {
         if (self.getSlot(1).getItem().isEmpty()) {
            cir.setReturnValue(false);
         } else {
            CreateTemplateMode m = (CreateTemplateMode)t.get((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get());
            if (m == null) {
               cir.setReturnValue(false);
            } else {
               String i = m.mode();

               int size = switch (i) {
                  case "delta" -> 8;
                  case "gamma" -> 4;
                  case "beta" -> 2;
                  default -> 0;
               };
               if (size == 0) {
                  cir.setReturnValue(false);
               } else {
                  for (int j = 0; j < size; j++) {
                     if (self.getSlot(2 + j).getItem().isEmpty()) {
                        cir.setReturnValue(false);
                        return;
                     }
                  }

                  cir.setReturnValue(true);
               }
            }
         }
      }
   }
}
