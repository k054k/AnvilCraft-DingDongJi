package com.dingdongji.mod.integration.jei;

import com.dingdongji.mod.util.AnvilCraftCompat;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

/**
 * 叮咚叽 JEI 适配：为玩家背包界面（{@link InventoryScreen}）声明深口袋
 * 面板的真实避让区域。
 * <p>
 * 铁砧自身的 PocketGuiHandler 已注册过同类 handler，但其 cap≠12 时只
 * 上报 26×73 的窄面板，不知道我方 44×109/127 的竖面板，导致 JEI
 * 物品压在面板上。JEI 内部 GuiContainerHandlers 对同一界面保存
 * handler <b>列表</b>（已实证：Entry.handlers 为 List），多 handler
 * 返回的区域取并集，因此我方补充上报真实矩形即可，互不冲突。
 * <p>
 * 几何与 MixinPocketScreenSupport 完全一致：
 * <ul>
 *   <li>18 格：5 行，y=guiTop+34，44×109；</li>
 *   <li>24 格：6 行，y=guiTop+16，44×127。</li>
 * </ul>
 */
@JeiPlugin
public class DdjJeiPlugin implements IModPlugin {
   private static final ResourceLocation PLUGIN_UID =
      ResourceLocation.fromNamespaceAndPath("dingdongji", "jei_plugin");

   private static final int PANEL_WIDTH = 44;

   @Override
   public ResourceLocation getPluginUid() {
      return PLUGIN_UID;
   }

   @Override
   public void registerGuiHandlers(IGuiHandlerRegistration registration) {
      registration.addGuiContainerHandler(InventoryScreen.class, new Handler());
   }

   private static final class Handler implements IGuiContainerHandler<InventoryScreen> {
      @Override
      public List<Rect2i> getGuiExtraAreas(InventoryScreen screen) {
         var player = Minecraft.getInstance().player;
         if (player == null) {
            return List.of();
         }
         int cap = AnvilCraftCompat.getPocketCapacity(player);
         if (cap <= 12) {
            return List.of();
         }
         int guiLeft = screen.getGuiLeft();
         int guiTop = screen.getGuiTop();
         int topOff = cap == 24 ? 16 : 34;
         int h = cap == 24 ? 127 : 109;
         int leftX = guiLeft - PANEL_WIDTH - 2;
         int rightX = guiLeft + screen.getXSize() + 2;
         return List.of(
            new Rect2i(leftX, guiTop + topOff, PANEL_WIDTH, h),
            new Rect2i(rightX, guiTop + topOff, PANEL_WIDTH, h)
         );
      }
   }
}
