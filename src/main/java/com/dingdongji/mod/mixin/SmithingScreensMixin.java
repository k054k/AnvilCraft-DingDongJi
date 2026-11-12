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

/**
 * 0.1.0 合并类（多目标 @Pseudo）：铁砧工艺相邻锻造台与超限锻造台两个界面
 * 的模板点击选模式 + 禁止收藏，注入点完全一致，合并为一类。
 * 仅超限台菜单有 selectedTemplate 字段：反射按菜单实际类型判定设置，
 * 相邻台菜单类型不匹配时静默跳过。
 */
@Pseudo
@Mixin(
   targets = {
      "dev.dubhe.anvilcraft.client.gui.screen.AdjacentSmithingScreen",
      "dev.dubhe.anvilcraft.client.gui.screen.TranscendenceSmithingScreen"
   },
   remap = false
)
public abstract class SmithingScreensMixin {

   @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, remap = false)
   private void ddj$sendSelectedMode(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      if (button != 0) {
         return;
      }
      try {
         ItemStack hovered = this.ddj$templateAt(mouseX, mouseY);
         if (hovered == null || hovered.isEmpty() || !ModItems.isCreateTemplate(hovered)) {
            return;
         }

         CreateTemplateMode mode = (CreateTemplateMode) hovered.getOrDefault(
            (DataComponentType) ModComponents.CREATE_TEMPLATE_MODE.get(), CreateTemplateMode.DEFAULT
         );
         AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
         int containerId = screen.getMenu().containerId;
         PacketDistributor.sendToServer(new SelectTemplateModePacket(containerId, mode.mode()), new CustomPacketPayload[0]);

         // 仅超限台菜单存在 selectedTemplate 字段；相邻台走到 isInstance 判定即跳过
         try {
            Object menu = screen.getMenu();
            Class<?> tc = Class.forName("dev.dubhe.anvilcraft.inventory.TranscendenceSmithingMenu");
            if (tc.isInstance(menu)) {
               Field f = tc.getDeclaredField("selectedTemplate");
               f.setAccessible(true);
               f.set(menu, hovered.copyWithCount(1));
            }
         } catch (Exception ignored) {
         }

         cir.setReturnValue(true);
      } catch (Exception ignored) {
      }
   }

   @Inject(method = "isFavorite", at = @At("HEAD"), cancellable = true, remap = false)
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
               return (ItemStack) m.invoke(this, x, y);
            } catch (NoSuchMethodException ignored) {
            }
         }
      } catch (Exception ignored) {
      }
      return null;
   }
}
