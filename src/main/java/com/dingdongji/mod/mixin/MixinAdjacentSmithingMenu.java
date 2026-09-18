package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.item.component.CreateTemplateMode;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
   targets = {"dev.dubhe.anvilcraft.inventory.AdjacentSmithingMenu"},
   remap = false
)
public abstract class MixinAdjacentSmithingMenu {
   @Inject(
      method = {"refreshTemplateCatalog"},
      at = {@At("TAIL")}
   )
   private void dingdongji$expandCreateTemplate(CallbackInfo ci) {
      try {
         Class<?> clazz = Class.forName("dev.dubhe.anvilcraft.inventory.AdjacentSmithingMenu");
         Field field = clazz.getDeclaredField("adjacentTemplates");
         field.setAccessible(true);
         List<ItemStack> templates = (List<ItemStack>)field.get(this);
         if (templates == null || templates.isEmpty()) {
            return;
         }

         int createIdx = -1;

         for (int i = 0; i < templates.size(); i++) {
            if (ModItems.isCreateTemplate(templates.get(i))) {
               createIdx = i;
               break;
            }
         }

         if (createIdx < 0) {
            return;
         }

         List<CreateTemplateMode> modes = ddj$modesForTable(this);
         if (modes.isEmpty()) {
            return;
         }

         ItemStack base = templates.get(createIdx).copyWithCount(1);
         List<ItemStack> expansion = new ArrayList<>(modes.size());

         for (CreateTemplateMode m : modes) {
            ItemStack st = base.copy();
            st.set((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get(), m);
            expansion.add(st);
         }

         templates.remove(createIdx);
         templates.addAll(0, expansion);
         Field dirtyField = clazz.getDeclaredField("templateDataDirty");
         dirtyField.setAccessible(true);
         dirtyField.setBoolean(this, true);
      } catch (Exception var12) {
      }
   }

   private static List<CreateTemplateMode> ddj$modesForTable(Object menu) {
      List<CreateTemplateMode> modes = new ArrayList<>();

      try {
         if (Class.forName("dev.dubhe.anvilcraft.inventory.RoyalSmithingMenu").isInstance(menu)) {
            modes.add(CreateTemplateMode.ALPHA);
         } else if (Class.forName("dev.dubhe.anvilcraft.inventory.EmberSmithingMenu").isInstance(menu)) {
            modes.add(CreateTemplateMode.BETA);
            modes.add(CreateTemplateMode.GAMMA);
            modes.add(CreateTemplateMode.DELTA);
         } else if (Class.forName("dev.dubhe.anvilcraft.inventory.FrostSmithingMenu").isInstance(menu)) {
            modes.add(CreateTemplateMode.EPSILON);
            modes.add(CreateTemplateMode.ZETA);
         } else {
            modes.add(CreateTemplateMode.ALPHA);
         }
      } catch (Exception var3) {
         modes.add(CreateTemplateMode.ALPHA);
      }

      return modes;
   }

   @Inject(
      method = {"isBorrowedTemplate"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void ddj$isBorrowedTemplate(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
      try {
         Field f = Class.forName("dev.dubhe.anvilcraft.inventory.AdjacentSmithingMenu").getDeclaredField("borrowedTemplateStack");
         f.setAccessible(true);
         ItemStack borrowed = (ItemStack)f.get(this);
         if (ModItems.isCreateTemplate(stack) && ModItems.isCreateTemplate(borrowed)) {
            CreateTemplateMode m1 = (CreateTemplateMode)stack.getOrDefault(
               (DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get(), CreateTemplateMode.DEFAULT
            );
            CreateTemplateMode m2 = (CreateTemplateMode)borrowed.getOrDefault(
               (DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get(), CreateTemplateMode.DEFAULT
            );
            cir.setReturnValue(m1.mode().equals(m2.mode()));
         }
      } catch (Exception var7) {
      }
   }
}
