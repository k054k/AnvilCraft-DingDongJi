package com.dingdongji.mod.integration.jei;

import com.dingdongji.mod.block.ModBlocks;
import com.dingdongji.mod.init.ModRecipes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import com.dingdongji.mod.recipe.ItemPortalConversionRecipe;
import com.dingdongji.mod.util.AnvilCraftCompat;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
         new ItemPortalConversionCategory(registration.getJeiHelpers().getGuiHelper()),
         new TimeAnchorCategory(registration.getJeiHelpers().getGuiHelper()),
         new AnchoredMobTransformCategory(registration.getJeiHelpers().getGuiHelper())
      );
   }

   @Override
   public void registerRecipes(IRecipeRegistration registration) {
      // 信标转化为客户端静态展示配方（数据载体，不依赖存档配方管理器）；
      // 强制走本体"信标转化"分类（配方子类 + DdjBeaconConversionCategoryMixin 接管布槽/绘制）。
      // 本体 BeaconConversionCategory.registerRecipes 被 MixinBeaconConversionRegisterMixin
      // 取消，改由我方按"先己方、后本体"顺序一次性注册——本体 4 条腐化信标转化
      // 配方因此排在整个分类的最后面。
      List<dev.dubhe.anvilcraft.integration.jei.recipe.BeaconConversionRecipe> beaconConversions =
         new java.util.ArrayList<>(DdjBeaconConversionRecipe.getAll());
      beaconConversions.addAll(dev.dubhe.anvilcraft.integration.jei.recipe.BeaconConversionRecipe.getAllRecipes());
      registration.addRecipes(AnvilCraftJeiPlugin.BEACON_CONVERSION, beaconConversions);

      // 过冷浮霜金属块产超限合金：静态展示配方，走本体 item_inject 分类（tier 多产物渲染）
      registration.addRecipes(AnvilCraftJeiPlugin.ITEM_INJECT, SupercooledTranscendiumJeiRecipes.getAll());

      // 铭化信标"时定"配方（纯静态展示，不依赖存档连接）
      registration.addRecipes(TimeAnchorCategory.RECIPE_TYPE, TimeAnchorRecipe.getAll());

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

      // 铭化信标光柱生物转化：独立注册的配方类型（input=被腐化生物，
      // result=转化前原生物），不复用本体腐化信标的 mob_transform 配方。
      List<RecipeHolder<com.dingdongji.mod.recipe.AnchoredMobTransformRecipe>> mobTransformRecipes =
         connection.getRecipeManager()
            .getAllRecipesFor(ModRecipes.ANCHORED_MOB_TRANSFORM_TYPE.get());
      registration.addRecipes(AnchoredMobTransformCategory.RECIPE_TYPE, mobTransformRecipes);
   }

   @Override
   public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
      // 与铁砧 portal_conversion 一致：末地传送门框架、黑曜石。
      registration.addRecipeCatalyst(new ItemStack(Blocks.END_PORTAL_FRAME),
         ItemPortalConversionCategory.RECIPE_TYPE);
      registration.addRecipeCatalyst(new ItemStack(Blocks.OBSIDIAN),
         ItemPortalConversionCategory.RECIPE_TYPE);
      // 信标转化催化剂（挂本体 RECIPE_TYPE，与本体配方同栏展示）
      registration.addRecipeCatalyst(new ItemStack(Items.BEACON),
         AnvilCraftJeiPlugin.BEACON_CONVERSION);
      registration.addRecipeCatalyst(new ItemStack(ModBlocks.CURSED_HEAVY_GOLD_BLOCK.get()),
         AnvilCraftJeiPlugin.BEACON_CONVERSION);
      registration.addRecipeCatalyst(new ItemStack(ModBlocks.ENCHANTED_HEAVY_GOLD_BLOCK.get()),
         AnvilCraftJeiPlugin.BEACON_CONVERSION);
      // 过冷浮霜金属块作为 item_inject（超限合金）入口催化剂，方便从方块查配方
      registration.addRecipeCatalyst(new ItemStack(ModBlocks.SUPERCOOLED_FROST_METAL_BLOCK.get()),
         AnvilCraftJeiPlugin.ITEM_INJECT);
      // 铭化信标为生物反向转化入口催化剂（独立分类）
      registration.addRecipeCatalyst(new ItemStack(ModBlocks.ANCHORED_BEACON.get()),
         AnchoredMobTransformCategory.RECIPE_TYPE);
      // 铭化信标为"时定"配方入口催化剂
      registration.addRecipeCatalyst(new ItemStack(ModBlocks.ANCHORED_BEACON.get()),
         TimeAnchorCategory.RECIPE_TYPE);
      registration.addRecipeCatalyst(new ItemStack(ModBlocks.SUPERCOOLED_FROST_METAL_BLOCK.get()),
         TimeAnchorCategory.RECIPE_TYPE);
      registration.addRecipeCatalyst(new ItemStack(overheatedEmberItem()),
         TimeAnchorCategory.RECIPE_TYPE);
   }

   /**
    * 仅从 JEI 左侧物品栏隐藏过冷浮霜金属块与本体超温余烬金属块：
    * removeIngredientsAtRuntime 只作用于侧边物品列表（经实证，JEI
    * RecipeManager 不监听该移除事件），时定配方、催化剂、物品查询均不受影响。
    */
   @Override
   public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
      jeiRuntime.getIngredientManager().removeIngredientsAtRuntime(
         VanillaTypes.ITEM_STACK,
         List.of(
            new ItemStack(ModBlocks.SUPERCOOLED_FROST_METAL_BLOCK.get()),
            new ItemStack(overheatedEmberItem())
         )
      );
   }

   private static Item overheatedEmberItem() {
      return BuiltInRegistries.ITEM.get(
         ResourceLocation.fromNamespaceAndPath("anvilcraft", "overheated_ember_metal_block"));
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
