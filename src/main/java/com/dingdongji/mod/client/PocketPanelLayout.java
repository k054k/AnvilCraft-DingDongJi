package com.dingdongji.mod.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * 深口袋（容量 18 / 24）拓展面板的几何与贴图，生存背包界面与创造物品栏
 * 界面共用。槽位坐标由 MixinPocketSlotLayout 对齐，本类只管面板背景与
 * 点击区域：
 * <ul>
 *   <li>18 格：5 行，面板 44×109，生存 topPos+34；</li>
 *   <li>24 格：6 行，面板 44×127，生存 topPos+16；</li>
 *   <li>创造界面槽位整体高 30px（本体 SlotWrapper y-30），面板同步上移。</li>
 * </ul>
 */
public final class PocketPanelLayout {
   public static final int PANEL_WIDTH = 44;
   public static final int CREATIVE_Y_SHIFT = 30;

   private static final ResourceLocation PANEL_5ROWS =
      ResourceLocation.fromNamespaceAndPath("dingdongji", "textures/gui/deep_pockets_panel.png");
   private static final ResourceLocation PANEL_5ROWS_RIGHT =
      ResourceLocation.fromNamespaceAndPath("dingdongji", "textures/gui/deep_pockets_panel_right.png");
   private static final ResourceLocation PANEL_6ROWS =
      ResourceLocation.fromNamespaceAndPath("dingdongji", "textures/gui/deep_pockets_panel_tall.png");

   private PocketPanelLayout() {
   }

   public static int panelHeight(int cap) {
      return cap == 24 ? 127 : 109;
   }

   public static int survivalTopOffset(int cap) {
      return cap == 24 ? 16 : 34;
   }

   public static int creativeTopOffset(int cap) {
      return survivalTopOffset(cap) - CREATIVE_Y_SHIFT;
   }

   public static ResourceLocation panelTexture(int cap) {
      return cap == 24 ? PANEL_6ROWS : PANEL_5ROWS;
   }

   /** 18 格用镜像版（孤格在右列）保证左右对称；24 格与左面板同图。 */
   public static ResourceLocation panelRightTexture(int cap) {
      return cap == 24 ? PANEL_6ROWS : PANEL_5ROWS_RIGHT;
   }

   /** 在界面两侧同一基线上绘制左右高面板。 */
   public static void blitPanels(GuiGraphics graphics, int leftPos, int imageWidth, int topPos, int topOffset, int cap) {
      int h = panelHeight(cap);
      graphics.blit(
         panelTexture(cap),
         leftPos - PANEL_WIDTH - 2, topPos + topOffset,
         0.0F, 0.0F,
         PANEL_WIDTH, h,
         PANEL_WIDTH, h
      );
      graphics.blit(
         panelRightTexture(cap),
         leftPos + imageWidth + 2, topPos + topOffset,
         0.0F, 0.0F,
         PANEL_WIDTH, h,
         PANEL_WIDTH, h
      );
   }

   /** 鼠标是否位于任一侧高面板区域内（用于 hasClickedOutside 放行）。 */
   public static boolean isInsidePanel(
      double mouseX, double mouseY, int guiLeft, int guiTop, int imageWidth, int topOffset, int cap
   ) {
      int h = panelHeight(cap);
      return mouseY >= guiTop + topOffset && mouseY < guiTop + topOffset + h
         && ((mouseX >= guiLeft - PANEL_WIDTH - 2 && mouseX <= guiLeft - 2)
          || (mouseX >= guiLeft + imageWidth + 2 && mouseX <= guiLeft + imageWidth + PANEL_WIDTH + 2));
   }
}
