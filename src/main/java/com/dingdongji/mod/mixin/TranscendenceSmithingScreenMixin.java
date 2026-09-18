package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.item.component.CreateTemplateMode;
import com.dingdongji.mod.network.SelectTemplateModePacket;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
   targets = {"dev.dubhe.anvilcraft.client.gui.screen.TranscendenceSmithingScreen"},
   remap = false
)
public abstract class TranscendenceSmithingScreenMixin {
   @Inject(
      method = {"mouseClicked"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void ddj$sendSelectedMode(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      if (button == 0) {
         try {
            ItemStack hovered = this.ddj$templateAt(mouseX, mouseY);
            if (hovered == null || hovered.isEmpty()) {
               return;
            }

            if (!ModItems.isCreateTemplate(hovered)) {
               return;
            }

            CreateTemplateMode mode = (CreateTemplateMode)hovered.getOrDefault(
               (DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get(), CreateTemplateMode.DEFAULT
            );
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>)(Object)this;
            int containerId = screen.getMenu().containerId;
            PacketDistributor.sendToServer(new SelectTemplateModePacket(containerId, mode.mode()), new CustomPacketPayload[0]);

            try {
               Object menu = screen.getMenu();
               Class<?> tc = Class.forName("dev.dubhe.anvilcraft.inventory.TranscendenceSmithingMenu");
               if (tc.isInstance(menu)) {
                  Field f = tc.getDeclaredField("selectedTemplate");
                  f.setAccessible(true);
                  ItemStack clientSel = hovered.copyWithCount(1);
                  f.set(menu, clientSel);
               }
            } catch (Exception var15) {
            }

            cir.setReturnValue(true);
         } catch (Exception var16) {
         }
      }
   }

   @Inject(
      method = {"isFavorite"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void ddj$isFavorite(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
      if (ModItems.isCreateTemplate(stack)) {
         cir.setReturnValue(false);
      }
   }

   private ItemStack ddj$templateAt(double x, double y) {
      try {
         for (Class<?> cls = this.getClass(); cls != null && cls != Object.class; cls = cls.getSuperclass()) {
            try {
               Method m = cls.getDeclaredMethod("templateAt", double.class, double.class);
               m.setAccessible(true);
               return (ItemStack)m.invoke(this, x, y);
            } catch (NoSuchMethodException var7) {
            }
         }
      } catch (Exception var8) {
      }

      return null;
   }
}
