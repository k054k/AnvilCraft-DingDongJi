package com.dingdongji.mod.mixin;

import com.dingdongji.mod.client.PocketPanelLayout;
import com.dingdongji.mod.util.AnvilCraftCompat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 容量 > 12（18 或 24）时补齐玩家背包界面的口袋面板。
 * 面板几何/贴图/点击区域统一在 {@link PocketPanelLayout}（创造界面共用）。
 * <p>
 * init 阈值补偿（与铁砧 PocketInventoryScreenMixin 同方法，避开冲突）：
 * 铁砧用 @ModifyConstant 把 widthTooNarrow 阈值 379 对 cap≠12 改为 435
 * （+56）。我方不能再用 @ModifyConstant 改同一常量——同常量只允许一个
 * modifier，铁砧的会被跳过并因 require=1 抛 Critical injection failure
 * （实测复现）。改为 @Redirect 第一次 GETFIELD Screen.width（比较点，
 * ordinal=0）：面板水平占用与 cap=12 相同（44 宽），阈值应同为 379，
 * 故传 width+56 使 {@code (width+56) >= 435} 等价 {@code width>=379}。
 */
@Mixin(value = InventoryScreen.class, priority = 2000)
public abstract class PocketScreenSupportMixin extends AbstractContainerScreen<InventoryMenu> {
   // 铁砧对 cap≠12 实际使用的阈值 = 379 + 56 = 435；cap=12（44宽）阈值=379
   private static final int ANVIL_THRESHOLD = 435;
   private static final int TARGET_THRESHOLD = 379;

   public PocketScreenSupportMixin(InventoryMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
   }

   /**
    * init 中比较点 {@code width >= 435} 的 width 读取（第一次，ordinal=0）。
    * 面板 44 宽（同 cap=12），传 width+56 使实际阈值回到 379。
    */
   @Redirect(
      method = "init",
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/client/gui/screens/Screen;width:I",
         opcode = Opcodes.GETFIELD,
         ordinal = 0
      )
   )
   private int ddj$widenWidthComparison(Screen screen, int width) {
      Player player = this.minecraft.player;
      if (player != null && AnvilCraftCompat.getPocketCapacity(player) > 12) {
         return width + (ANVIL_THRESHOLD - TARGET_THRESHOLD);
      }
      return width;
   }

   /** renderBg TAIL：在铁砧窄面板上覆盖绘制 44 宽高面板。 */
   @Inject(method = "renderBg", at = @At("TAIL"))
   private void ddj$drawDeep(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
      Player player = this.minecraft.player;
      if (player == null) {
         return;
      }
      int cap = AnvilCraftCompat.getPocketCapacity(player);
      if (cap <= 12) {
         return;
      }
      PocketPanelLayout.blitPanels(
         guiGraphics, this.leftPos, this.imageWidth, this.topPos,
         PocketPanelLayout.survivalTopOffset(cap), cap
      );
   }

   /** hasClickedOutside RETURN：高面板区域不算 outside。 */
   @Inject(method = "hasClickedOutside", at = @At("RETURN"), cancellable = true)
   private void ddj$includeDeep(
      double mouseX, double mouseY, int guiLeft, int guiTop, int button,
      CallbackInfoReturnable<Boolean> cir
   ) {
      Player player = this.minecraft.player;
      if (player == null) {
         return;
      }
      int cap = AnvilCraftCompat.getPocketCapacity(player);
      if (cap > 12 && PocketPanelLayout.isInsidePanel(
            mouseX, mouseY, guiLeft, guiTop, this.imageWidth,
            PocketPanelLayout.survivalTopOffset(cap), cap)) {
         cir.setReturnValue(false);
      }
   }
}
