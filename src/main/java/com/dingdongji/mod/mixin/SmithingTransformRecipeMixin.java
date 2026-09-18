package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.item.component.CreateTemplateMode;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({SmithingTransformRecipe.class})
public abstract class SmithingTransformRecipeMixin {
   private static final Logger LOGGER = LoggerFactory.getLogger("dingdongji");

   @Inject(
      method = {"isTemplateIngredient"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void dingdongji$isTemplateIngredient(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
      if (ModItems.isCreateTemplate(stack)) {
         CreateTemplateMode mode = (CreateTemplateMode)stack.get((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get());
         if (mode == null) {
            mode = CreateTemplateMode.DEFAULT;
         }

         cir.setReturnValue(CreateTemplateMode.ALPHA.equals(mode));
      }
   }

   @Redirect(
      method = {"matches"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/item/crafting/Ingredient;test(Lnet/minecraft/world/item/ItemStack;)Z",
         ordinal = 0
      )
   )
   private boolean dingdongji$templateTest(Ingredient ingredient, ItemStack stack) {
      if (!ModItems.isCreateTemplate(stack)) {
         return ingredient.test(stack);
      } else {
         CreateTemplateMode mode = (CreateTemplateMode)stack.get((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get());
         if (mode == null) {
            mode = CreateTemplateMode.DEFAULT;
         }

         if (CreateTemplateMode.ALPHA.equals(mode)) {
            return true;
         } else {
            String targetId = mode.getTargetTemplateId();
            if (targetId == null) {
               return true;
            } else {
               Item targetItem = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.parse(targetId));
               if (targetItem == Items.AIR) {
                  return false;
               } else {
                  for (ItemStack ingredientStack : ingredient.getItems()) {
                     if (ingredientStack.is(targetItem)) {
                        return true;
                     }
                  }

                  return false;
               }
            }
         }
      }
   }
}
