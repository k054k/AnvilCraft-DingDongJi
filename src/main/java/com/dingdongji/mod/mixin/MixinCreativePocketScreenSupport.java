package com.dingdongji.mod.mixin;

import com.dingdongji.mod.util.AnvilCraftCompat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
 * Creative inventory "INVENTORY" tab support for pocket capacity > 12
 * (18 or 24 slots). The survival InventoryScreen is handled by
 * MixinPocketScreenSupport; this mixin covers the creative screen.
 * <p>
 * Slot geometry: anvilcraft wraps every player.inventoryMenu slot in a
 * SlotWrapper, passing PocketSlot.x/y into the wrapper (y offset -30 for
 * the creative layout). Our MixinPocketSlotLayout already places the extra
 * rows above the basic three. What is missing for cap > 12 is the panel
 * background: anvilcraft only draws a 3-row, 73px tall panel (26px wide
 * narrow variant when cap != 12), leaving the upper rows floating and
 * making the 44px-wide double column overlap the wrong texture. We draw
 * our 44px-wide tall panels over it at TAIL, exactly like the survival
 * variant but shifted 30px up to match the creative wrapper offset.
 * <ul>
 *   <li>18 slots: 5 rows, panel 44x109 at topPos + 4;</li>
 *   <li>24 slots: 6 rows, panel 44x127 at topPos - 14.</li>
 * </ul>
 * hasClickedOutside is extended by the same region so clicks on the upper
 * rows do not close the screen.
 */
// Higher priority (2000 > default 1000) ensures this mixin's renderBg TAIL
// injection runs AFTER anvilcraft's CreativePocketScreenMixin, so our tall
// panel blit covers anvilcraft's 26px-wide narrow panel draw instead of
// vice versa — eliminates the "two overlapping pocket panels" bug in
// creative-mode INVENTORY tab with capacity > 12.
@Mixin(value = CreativeModeInventoryScreen.class, priority = 2000)
public abstract class MixinCreativePocketScreenSupport
   extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
   @Shadow
   private static CreativeModeTab selectedTab;

   private static final ResourceLocation PANEL_5ROWS_TEXTURE =
      ResourceLocation.fromNamespaceAndPath("dingdongji", "textures/gui/deep_pockets_panel.png");
   private static final ResourceLocation PANEL_5ROWS_TEXTURE_RIGHT =
      ResourceLocation.fromNamespaceAndPath("dingdongji", "textures/gui/deep_pockets_panel_right.png");
   private static final ResourceLocation PANEL_6ROWS_TEXTURE =
      ResourceLocation.fromNamespaceAndPath("dingdongji", "textures/gui/deep_pockets_panel_tall.png");
   private static final int PANEL_WIDTH = 44;
   // Creative wrappers sit 30px higher than survival slots (anvilcraft y - 30).
   private static final int CREATIVE_Y_SHIFT = 30;
   // Survival top offsets from MixinPocketScreenSupport.
   private static final int SURVIVAL_TOP_OFFSET_18 = 34;
   private static final int SURVIVAL_TOP_OFFSET_24 = 16;

   public MixinCreativePocketScreenSupport(
      CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory playerInventory, Component title
   ) {
      super(menu, playerInventory, title);
   }

   private static int panelTopOffset(int cap) {
      return (cap == 24 ? SURVIVAL_TOP_OFFSET_24 : SURVIVAL_TOP_OFFSET_18) - CREATIVE_Y_SHIFT;
   }

   private static int panelHeight(int cap) {
      return cap == 24 ? 127 : 109;
   }

   private static ResourceLocation panelTexture(int cap) {
      return cap == 24 ? PANEL_6ROWS_TEXTURE : PANEL_5ROWS_TEXTURE;
   }

   private static ResourceLocation panelRightTexture(int cap) {
      return cap == 24 ? PANEL_6ROWS_TEXTURE : PANEL_5ROWS_TEXTURE_RIGHT;
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
      int topOff = panelTopOffset(cap);
      int h = panelHeight(cap);
      guiGraphics.blit(
         panelTexture(cap),
         this.leftPos - PANEL_WIDTH - 2, this.topPos + topOff,
         0.0F, 0.0F,
         PANEL_WIDTH, h,
         PANEL_WIDTH, h
      );
      guiGraphics.blit(
         panelRightTexture(cap),
         this.leftPos + this.imageWidth + 2, this.topPos + topOff,
         0.0F, 0.0F,
         PANEL_WIDTH, h,
         PANEL_WIDTH, h
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
         int topOff = panelTopOffset(cap);
         int h = panelHeight(cap);
         boolean insidePanel = mouseY >= guiTop + topOff && mouseY < guiTop + topOff + h
            && ((mouseX >= guiLeft - PANEL_WIDTH - 2 && mouseX <= guiLeft - 2)
             || (mouseX >= guiLeft + this.imageWidth + 2
                 && mouseX <= guiLeft + this.imageWidth + PANEL_WIDTH + 2));
         if (insidePanel) {
            cir.setReturnValue(false);
         }
      }
   }
}
