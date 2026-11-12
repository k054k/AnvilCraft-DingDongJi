package com.dingdongji.mod.mixin;

import com.dingdongji.mod.client.PocketPanelLayout;
import com.dingdongji.mod.util.AnvilCraftCompat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 创造物品栏"物品栏"分页的深口袋（cap 18 / 24）面板支持。生存背包界面
 * 由 PocketScreenSupportMixin 处理；面板几何/贴图/点击区域统一在
 * {@link PocketPanelLayout}，创造侧整体上移 {@link PocketPanelLayout#CREATIVE_Y_SHIFT}
 * （本体 SlotWrapper y-30）。
 */
// 较高优先级（2000 > 默认 1000）确保 renderBg TAIL 在本体
// CreativePocketScreenMixin 之后执行，让我方高面板覆盖其 26px 窄面板，
// 消除 cap>12 时"两个口袋面板重叠"的问题。
@Mixin(value = CreativeModeInventoryScreen.class, priority = 2000)
public abstract class CreativePocketScreenSupportMixin
   extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
   @Shadow
   private static CreativeModeTab selectedTab;

   public CreativePocketScreenSupportMixin(
      CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory playerInventory, Component title
   ) {
      super(menu, playerInventory, title);
   }

   private boolean ddj$deepPocketsActive() {
      if (selectedTab == null || selectedTab.getType() != CreativeModeTab.Type.INVENTORY) {
         return false;
      }
      Player player = this.minecraft.player;
      return player != null && AnvilCraftCompat.getPocketCapacity(player) > 12;
   }

   @Inject(method = "renderBg", at = @At("TAIL"))
   private void ddj$drawDeepPocketPanels(
      GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci
   ) {
      if (!ddj$deepPocketsActive()) {
         return;
      }
      Player player = this.minecraft.player;
      int cap = AnvilCraftCompat.getPocketCapacity(player);
      PocketPanelLayout.blitPanels(
         guiGraphics, this.leftPos, this.imageWidth, this.topPos,
         PocketPanelLayout.creativeTopOffset(cap), cap
      );
   }

   @Inject(method = "hasClickedOutside", at = @At("RETURN"), cancellable = true)
   private void ddj$includeDeepPocketPanels(
      double mouseX, double mouseY, int guiLeft, int guiTop, int button,
      CallbackInfoReturnable<Boolean> cir
   ) {
      if (cir.getReturnValueZ() && ddj$deepPocketsActive()) {
         Player player = this.minecraft.player;
         int cap = AnvilCraftCompat.getPocketCapacity(player);
         if (PocketPanelLayout.isInsidePanel(
               mouseX, mouseY, guiLeft, guiTop, this.imageWidth,
               PocketPanelLayout.creativeTopOffset(cap), cap)) {
            cir.setReturnValue(false);
         }
      }
   }
}
