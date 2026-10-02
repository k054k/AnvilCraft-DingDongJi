package com.dingdongji.mod.integration.jei;

import com.dingdongji.mod.recipe.ItemPortalConversionRecipe;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.anvilcraft.integration.jei.util.JeiBlockIngredientUtil;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import org.joml.Matrix4f;

/**
 * "物品掉入传送门" 的 JEI 分类展示，物品槽机制与铁砧 AnvilCraft 的
 * {@code PortalConversionCategory}（方块掉入传送门）完全同源——直接复用其
 * 公共工具 {@link JeiBlockIngredientUtil}：
 * <ul>
 *   <li>泛型为 {@link RecipeHolder}：JEI 据此自动显示配方 id（F3+H 高级
 *       提示）、支持加入书签与催化剂关联；</li>
 *   <li>JEI 槽挂的是<b>透明渲染器</b>，槽内物品由 JEI 绘制的部分为空，
 *       实际物品在 {@link #draw} 里手绘到 18×18 槽内的标准 (1,1) 内缩
 *       位置，与槽底图像素级重合，不会再出现物品与 GUI 错位；</li>
 *   <li>悬停时槽位不变白（铁砧同款 NoHover 控件，由
 *       {@code suppressHoverOverlays} 提供），但物品名/概率 tooltip 照常；</li>
 *   <li>概率不画在输出格下方，而是挂在输出槽 rich tooltip 上悬停显示，
 *       文案复用铁砧 gui.anvilcraft.category.chance；</li>
 *   <li>中间末地传送门贴图按原始像素等比缩放（最长边不超过 110×64 且
 *       不放大），在传送门区域内居中；</li>
 *   <li>分类图标与催化剂为末地传送门框架（另加黑曜石），与铁砧一致。</li>
 * </ul>
 */
public class ItemPortalConversionCategory
    implements IRecipeCategory<RecipeHolder<ItemPortalConversionRecipe>> {

   public static final RecipeType<RecipeHolder<ItemPortalConversionRecipe>> RECIPE_TYPE =
      RecipeType.createRecipeHolderType(
         ResourceLocation.fromNamespaceAndPath("dingdongji", "item_portal_conversion"));

   /** 槽位逻辑名（JeiBlockIngredientUtil 会自动加 anvilcraft:block_preview/ 前缀）。 */
   private static final String INPUT_SLOT = "ddj_input";
   private static final String OUTPUT_SLOT = "ddj_output";
   private static final String SLOT_NAME_PREFIX = "anvilcraft:block_preview/";

   /** 复用铁砧 jar 内资源，无需在 DDJ 重复打包纹理。 */
   private static final ResourceLocation ANVIL_SLOT_DEFAULT =
      ResourceLocation.fromNamespaceAndPath("anvilcraft", "textures/gui/jei/slot_default.png");
   private static final ResourceLocation ANVIL_SLOT_PROB =
      ResourceLocation.fromNamespaceAndPath("anvilcraft", "textures/gui/jei/slot_probability.png");
   private static final ResourceLocation END_PORTAL_TEX =
      ResourceLocation.fromNamespaceAndPath("anvilcraft", "textures/gui/jei/portal/end_portal.png");

   private static final int WIDTH = 162;
   private static final int HEIGHT = 64;
   /** 中间传送门展示区（x 起点 26，与铁砧一致）。 */
   private static final int PORTAL_X = 26;
   private static final int PORTAL_WIDTH = 110;
   private static final int PORTAL_HEIGHT = 64;

   private static final DecimalFormat FORMATTER = new DecimalFormat();
   private static final Map<ResourceLocation, int[]> SIZE_CACHE = new HashMap<>();

   private final Component title;
   private final IDrawable icon;
   private final IDrawable slotDefault;
   private final IDrawable slotProbability;

   public ItemPortalConversionCategory(IGuiHelper helper) {
      this.title = Component.translatable("jei.category.dingdongji.item_portal_conversion");
      this.icon = helper.createDrawableIngredient(
         VanillaTypes.ITEM_STACK, new ItemStack(Blocks.END_PORTAL_FRAME));
      this.slotDefault = helper.drawableBuilder(ANVIL_SLOT_DEFAULT, 0, 0, 18, 18)
         .setTextureSize(18, 18).build();
      this.slotProbability = helper.drawableBuilder(ANVIL_SLOT_PROB, 0, 0, 18, 18)
         .setTextureSize(18, 18).build();
   }

   @Override
   public RecipeType<RecipeHolder<ItemPortalConversionRecipe>> getRecipeType() {
      return RECIPE_TYPE;
   }

   @Override
   public Component getTitle() {
      return title;
   }

   @Override
   public IDrawable getIcon() {
      return icon;
   }

   @Override
   public int getWidth() {
      return WIDTH;
   }

   @Override
   public int getHeight() {
      return HEIGHT;
   }

   // --- Layout -----------------------------------------------------------------------
   @Override
   public void setRecipe(
      IRecipeLayoutBuilder builder,
      RecipeHolder<ItemPortalConversionRecipe> holder,
      IFocusGroup focuses
   ) {
      ItemPortalConversionRecipe recipe = holder.value();
      // 铁砧同款透明渲染器槽：JEI 不画物品（物品在 draw 手绘），仅负责命中、
      // 焦点与 tooltip。槽命中区 18×18，与手绘槽底图完全重合。
      JeiBlockIngredientUtil.addSlot(
         builder, RecipeIngredientRole.INPUT, INPUT_SLOT, 4, 4, 18, 18,
         Arrays.asList(recipe.input().getItems()));
      JeiBlockIngredientUtil.addSlot(
         builder, RecipeIngredientRole.OUTPUT, OUTPUT_SLOT, 142, 4, 18, 18,
         List.of(recipe.result().item())
      ).addRichTooltipCallback((slotView, tooltip) -> {
         // 配方 id 无需手写：RecipeHolder 配方 JEI 在 F3+H 下自动追加，
         // 手写会造成同一行重复。这里只补概率，且悬停才显示。
         float chance = recipe.result().chance();
         if (chance < 1.0F) {
            tooltip.add(Component.translatable(
               "gui.anvilcraft.category.chance", FORMATTER.format(chance * 100.0F))
               .withStyle(ChatFormatting.GRAY));
         }
      });
   }

   @Override
   public void createRecipeExtras(
      IRecipeExtrasBuilder builder,
      RecipeHolder<ItemPortalConversionRecipe> holder,
      IFocusGroup focuses
   ) {
      // 铁砧同款：悬停在槽上时不绘制白色高亮覆盖（光标放上去框不变白）。
      JeiBlockIngredientUtil.suppressHoverOverlays(builder);
   }

   // --- Visual -----------------------------------------------------------------------
   @Override
   public void draw(
      RecipeHolder<ItemPortalConversionRecipe> holder,
      IRecipeSlotsView recipeSlotsView,
      GuiGraphics gui,
      double mouseX,
      double mouseY
   ) {
      ItemPortalConversionRecipe recipe = holder.value();

      // 左槽底图（4,4）与槽内物品（标准 16×16，内缩 1px 至 (5,5)）。
      this.slotDefault.draw(gui, 4, 4);
      displayedItem(recipeSlotsView, INPUT_SLOT)
         .or(() -> Optional.of(recipe.input().getItems()[0]))
         .ifPresent(stack -> gui.renderItem(stack, 5, 5));

      // 中间末地传送门：读真实像素尺寸，等比缩放（不放大）后在 110×64 区域居中。
      Minecraft minecraft = Minecraft.getInstance();
      int[] size = resolveSize(minecraft, END_PORTAL_TEX);
      float scale = Math.min(
         Math.min((float) PORTAL_WIDTH / size[0], (float) PORTAL_HEIGHT / size[1]), 1.0F);
      int renderW = Math.max(1, Math.round(size[0] * scale));
      int renderH = Math.max(1, Math.round(size[1] * scale));
      int x = PORTAL_X + (PORTAL_WIDTH - renderW) / 2;
      int y = (PORTAL_HEIGHT - renderH) / 2;
      PoseStack pose = gui.pose();
      pose.pushPose();
      pose.translate(x, y, 0);
      innerBlit(gui, END_PORTAL_TEX, renderW, renderH);
      pose.popPose();

      // 右槽：必定转化用普通槽，否则虚影概率槽；物品同样内缩 1px。
      IDrawable outputSlot = recipe.result().chance() >= 1.0F
         ? this.slotDefault : this.slotProbability;
      outputSlot.draw(gui, 142, 4);
      displayedItem(recipeSlotsView, OUTPUT_SLOT)
         .ifPresent(stack -> gui.renderItem(stack, 143, 5));
   }

   @Override
   public void getTooltip(
      ITooltipBuilder tooltip,
      RecipeHolder<ItemPortalConversionRecipe> holder,
      IRecipeSlotsView recipeSlotsView,
      double mouseX,
      double mouseY
   ) {
      // 中间传送门区域（避开左右槽：左槽止于 22，右槽起于 142）。
      if (mouseX >= 24 && mouseX <= 138 && mouseY >= 0 && mouseY <= 64) {
         tooltip.add(Component.translatable(
            "gui.anvilcraft.category.portal_conversion.fall_through",
            Component.translatable("block.minecraft.end_portal")));
      }
   }

   /** 读取 JEI 当前正在该槽展示的物品（tag 多候选时随 JEI 轮播同步）。 */
   private static Optional<ItemStack> displayedItem(IRecipeSlotsView slotsView, String slotName) {
      return slotsView.findSlotByName(SLOT_NAME_PREFIX + slotName)
         .flatMap(slot -> slot.getDisplayedItemStack());
   }

   /** 任意尺寸四边形贴图（照铁砧 PortalConversionCategory.innerBlit）。 */
   private static void innerBlit(GuiGraphics gui, ResourceLocation texture, int width, int height) {
      RenderSystem.enableBlend();
      RenderSystem.setShaderTexture(0, texture);
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      Matrix4f matrix = gui.pose().last().pose();
      BufferBuilder buffer = Tesselator.getInstance()
         .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      buffer.addVertex(matrix, 0.0F, 0.0F, 0.0F).setUv(0.0F, 0.0F);
      buffer.addVertex(matrix, 0.0F, (float) height, 0.0F).setUv(0.0F, 1.0F);
      buffer.addVertex(matrix, (float) width, (float) height, 0.0F).setUv(1.0F, 1.0F);
      buffer.addVertex(matrix, (float) width, 0.0F, 0.0F).setUv(1.0F, 0.0F);
      BufferUploader.drawWithShader(buffer.buildOrThrow());
      RenderSystem.disableBlend();
   }

   /** 读取贴图原始像素尺寸并缓存；缺失时回退 84×53（末地门实测尺寸）。 */
   private static int[] resolveSize(Minecraft minecraft, ResourceLocation location) {
      int[] cached = SIZE_CACHE.get(location);
      if (cached != null) {
         return cached;
      }
      int width = 84;
      int height = 53;
      try {
         Resource resource = minecraft.getResourceManager().getResource(location).orElse(null);
         if (resource != null) {
            try (NativeImage image = NativeImage.read(resource.open())) {
               width = Math.max(1, image.getWidth());
               height = Math.max(1, image.getHeight());
            }
         }
      } catch (IOException ignored) {
         // 贴图缺失或损坏时使用回退尺寸。
      }
      int[] size = new int[] {width, height};
      SIZE_CACHE.put(location, size);
      return size;
   }
}
