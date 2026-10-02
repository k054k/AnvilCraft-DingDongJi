package com.dingdongji.mod.integration.jei;

import com.dingdongji.mod.init.ModRecipes;
import com.dingdongji.mod.recipe.ItemPortalConversionRecipe;
import com.dingdongji.mod.util.AnvilCraftCompat;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;

/**
 * 叮咚叽 JEI 适配：为玩家背包界面（{@link InventoryScreen}）以及创造模式
 * 物品栏（{@link CreativeModeInventoryScreen} 的 INVENTORY 标签页）声明深口袋
 * 面板的真实避让区域。
 * <p>
 * 铁砧自身的 PocketGuiHandler 已注册过同类 handler，但其 cap≠12 时只
 * 上报 26×73 的窄面板，不知道我方 44×109/127 的竖面板，导致 JEI
 * 物品压在面板上。JEI 内部 GuiContainerHandlers 对同一界面保存
 * handler <b>列表</b>（已实证：Entry.handlers 为 List），多 handler
 * 返回的区域取并集，因此我方补充上报真实矩形即可，互不冲突。
 * <p>
 * 生存界面几何与 MixinPocketScreenSupport 完全一致：
 * <ul>
 *   <li>18 格：5 行，y=guiTop+34，44×109；</li>
 *   <li>24 格：6 行，y=guiTop+16，44×127。</li>
 * </ul>
 * 创造界面与 MixinCreativePocketScreenSupport 一致：y 再上移 30px，且
 * 仅在 INVENTORY 标签页上报。
 */
@JeiPlugin
public class DdjJeiPlugin implements IModPlugin {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceLocation PLUGIN_UID =
      ResourceLocation.fromNamespaceAndPath("dingdongji", "jei_plugin");

   private static final int PANEL_WIDTH = 44;
   private static final int CREATIVE_Y_SHIFT = 30;

   @Override
   public ResourceLocation getPluginUid() {
      return PLUGIN_UID;
   }

   @Override
   public void registerCategories(IRecipeCategoryRegistration registration) {
      registration.addRecipeCategories(
         new ItemPortalConversionCategory(registration.getJeiHelpers().getGuiHelper())
      );
   }

   @Override
   public void registerRecipes(IRecipeRegistration registration) {
      ClientPacketListener connection = Minecraft.getInstance().getConnection();
      if (connection == null) {
         LOGGER.warn("[DingDongJi][JEI] 无法注册物品传送门转换配方：客户端连接为空");
         return;
      }

      // 直接注册 RecipeHolder（不再 .map(value)）：JEI 借此提供配方 id
      // （F3+H）、书签以及催化剂关联——与铁砧 registerRecipes 写法一致。
      List<RecipeHolder<ItemPortalConversionRecipe>> recipes =
         connection.getRecipeManager()
            .getAllRecipesFor(ModRecipes.ITEM_PORTAL_CONVERSION_TYPE.get())
            .stream()
            .toList();
      registration.addRecipes(ItemPortalConversionCategory.RECIPE_TYPE, recipes);
   }

   @Override
   public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
      // 与铁砧 portal_conversion 一致：末地传送门框架、黑曜石。
      registration.addRecipeCatalyst(new ItemStack(Blocks.END_PORTAL_FRAME),
         ItemPortalConversionCategory.RECIPE_TYPE);
      registration.addRecipeCatalyst(new ItemStack(Blocks.OBSIDIAN),
         ItemPortalConversionCategory.RECIPE_TYPE);
   }

   @Override
   public void registerGuiHandlers(IGuiHandlerRegistration registration) {
      registration.addGuiContainerHandler(InventoryScreen.class, new PocketPanelHandler<>(0, false));
      registration.addGuiContainerHandler(
         CreativeModeInventoryScreen.class, new PocketPanelHandler<>(CREATIVE_Y_SHIFT, true)
      );
   }

   private static final class PocketPanelHandler<T extends AbstractContainerScreen<?>>
      implements IGuiContainerHandler<T> {
      private static Field selectedTabField;

      private final int topShift;
      private final boolean creative;

      private PocketPanelHandler(int topShift, boolean creative) {
         this.topShift = topShift;
         this.creative = creative;
      }

      @Override
      public List<Rect2i> getGuiExtraAreas(T screen) {
         if (this.creative && !onInventoryTab()) {
            return List.of();
         }

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
         int topOff = (cap == 24 ? 16 : 34) - this.topShift;
         int h = cap == 24 ? 127 : 109;
         int leftX = guiLeft - PANEL_WIDTH - 2;
         int rightX = guiLeft + screen.getXSize() + 2;
         return List.of(
            new Rect2i(leftX, guiTop + topOff, PANEL_WIDTH, h),
            new Rect2i(rightX, guiTop + topOff, PANEL_WIDTH, h)
         );
      }

      private static boolean onInventoryTab() {
         try {
            if (selectedTabField == null) {
               selectedTabField = CreativeModeInventoryScreen.class.getDeclaredField("selectedTab");
               selectedTabField.setAccessible(true);
            }

            CreativeModeTab tab = (CreativeModeTab)selectedTabField.get(null);
            return tab != null && tab.getType() == CreativeModeTab.Type.INVENTORY;
         } catch (Throwable t) {
            return false;
         }
      }
   }
}
