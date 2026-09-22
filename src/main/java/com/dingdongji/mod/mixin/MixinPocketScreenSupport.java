package com.dingdongji.mod.mixin;

import com.dingdongji.mod.util.AnvilCraftCompat;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 容量 > 12（18 或 24）时补齐玩家背包界面的口袋面板：<b>固定 44 宽、
 * 向上加行</b>。
 * <p>
 * 注入点（与铁砧 PocketInventoryScreenMixin 同方法，但<b>避开冲突</b>）：
 * <ol>
 *   <li>{@code init}：铁砧用 @ModifyConstant 把 widthTooNarrow 阈值 379
 *       对 cap≠12 改为 435（+56）。我方<b>不能</b>再用 @ModifyConstant
 *       改同一常量——同常量只允许一个 modifier，铁砧的会被跳过并因
 *       require=1 抛 Critical injection failure 崩溃（实测复现）。
 *       改为 @Redirect 第一次 GETFIELD Screen.width（比较点，ordinal=0，
 *       已实证 init 中 width 共读取 3 次，后两次用于配方书定位不受影响）：
 *       面板水平占用与 cap=12 完全相同（44 宽），阈值应同为 379，
 *       故传 width+56 使 {@code (width+56) >= 435} 等价 {@code width>=379}；</li>
 *   <li>{@code renderBg} TAIL：铁砧按 cap≠12 画了两个 26 宽窄面板，我方
 *       在<b>同一基线上</b>画 44 宽高面板将其完全覆盖（@Inject 可并存）；</li>
 *   <li>{@code hasClickedOutside} RETURN：高面板区域算"界面内部"。</li>
 * </ol>
 * 几何（槽位坐标由 MixinPocketSlotLayout 对齐）：
 * <ul>
 *   <li>18 格：5 行，面板 44×109，topPos+34；</li>
 *   <li>24 格：6 行，面板 44×127，topPos+16。</li>
 * </ul>
 */
@Mixin(value = InventoryScreen.class, priority = 2000)
public abstract class MixinPocketScreenSupport extends AbstractContainerScreen<InventoryMenu> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceLocation PANEL_5ROWS_TEXTURE =
      ResourceLocation.fromNamespaceAndPath("dingdongji", "textures/gui/deep_pockets_panel.png");
   private static final ResourceLocation PANEL_5ROWS_TEXTURE_RIGHT =
      ResourceLocation.fromNamespaceAndPath("dingdongji", "textures/gui/deep_pockets_panel_right.png");
   private static final ResourceLocation PANEL_6ROWS_TEXTURE =
      ResourceLocation.fromNamespaceAndPath("dingdongji", "textures/gui/deep_pockets_panel_tall.png");
   private static final int PANEL_WIDTH = 44;
   // 铁砧对 cap≠12 实际使用的阈值 = 379 + 56 = 435；cap=12（44宽）阈值=379
   private static final int ANVIL_THRESHOLD = 435;
   private static final int TARGET_THRESHOLD = 379;

   public MixinPocketScreenSupport(InventoryMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
   }

   private static int panelTopOffset(int cap) {
      return cap == 24 ? 16 : 34;
   }

   private static int panelHeight(int cap) {
      return cap == 24 ? 127 : 109;
   }

   private static ResourceLocation panelTexture(int cap) {
      return cap == 24 ? PANEL_6ROWS_TEXTURE : PANEL_5ROWS_TEXTURE;
   }

   /**
    * 右面板贴图：24 格（6 行双槽，无孤格）与左面板同图；
    * 18 格用镜像版（孤格在右列），保证左右对称。
    */
   private static ResourceLocation panelRightTexture(int cap) {
      return cap == 24 ? PANEL_6ROWS_TEXTURE : PANEL_5ROWS_TEXTURE_RIGHT;
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
      if (player != null) {
         int cap = AnvilCraftCompat.getPocketCapacity(player);
         if (cap > 12) {
            if (!ddj$thresholdLogged) {
               ddj$thresholdLogged = true;
               LOGGER.info(
                  "[DingDongJi][口袋] 竖面板阈值生效：容量={} 阈值={}（width {} 按 {} 比较）",
                  cap, TARGET_THRESHOLD, width, ANVIL_THRESHOLD
               );
            }
            return width + (ANVIL_THRESHOLD - TARGET_THRESHOLD);
         }
      }
      return width;
   }

   private boolean ddj$thresholdLogged;

   /** renderBg TAIL：在铁砧窄面板上覆盖绘制 44 宽高面板。 */
   @Inject(method = "renderBg", at = @At("TAIL"))
   private void ddj$drawDeep(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
      Player player = this.minecraft.player;
      if (player == null) return;
      int cap = AnvilCraftCompat.getPocketCapacity(player);
      if (cap <= 12) return;
      int topOff = panelTopOffset(cap);
      int h = panelHeight(cap);
      ResourceLocation tex = panelTexture(cap);
      ResourceLocation rightTex = panelRightTexture(cap);
      guiGraphics.blit(
         tex,
         this.leftPos - PANEL_WIDTH - 2, this.topPos + topOff,
         0.0F, 0.0F,
         PANEL_WIDTH, h,
         PANEL_WIDTH, h
      );
      guiGraphics.blit(
         rightTex,
         this.leftPos + this.imageWidth + 2, this.topPos + topOff,
         0.0F, 0.0F,
         PANEL_WIDTH, h,
         PANEL_WIDTH, h
      );
      if (!ddj$drawLogged) {
         ddj$drawLogged = true;
         LOGGER.info("[DingDongJi][口袋] 深口袋竖面板绘制：容量={} 44x{} topOffset={}", cap, h, topOff);
      }
   }

   private boolean ddj$drawLogged;

   /** hasClickedOutside RETURN：高面板区域不算 outside。 */
   @Inject(
      method = "hasClickedOutside",
      at = @At("RETURN"),
      cancellable = true
   )
   private void ddj$includeDeep(
      double mouseX, double mouseY, int guiLeft, int guiTop, int button,
      CallbackInfoReturnable<Boolean> cir
   ) {
      Player player = this.minecraft.player;
      if (player == null) return;
      int cap = AnvilCraftCompat.getPocketCapacity(player);
      if (cap <= 12) return;
      int topOff = panelTopOffset(cap);
      int h = panelHeight(cap);
      boolean insidePanel = mouseY >= guiTop + topOff && mouseY < guiTop + topOff + h
         && ((mouseX >= guiLeft - PANEL_WIDTH - 2 && mouseX <= guiLeft - 2)
          || (mouseX >= guiLeft + this.imageWidth + 2 && mouseX <= guiLeft + this.imageWidth + PANEL_WIDTH + 2));
      if (insidePanel) {
         cir.setReturnValue(false);
      }
   }
}
