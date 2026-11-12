package com.dingdongji.mod.integration.jei;

import com.dingdongji.mod.block.ModBlocks;
import com.dingdongji.mod.client.render.AnchoredBeaconRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * "时定"JEI 分类：完全复刻本体"时移"（AbstractLiquidCategory）的 3D 渲染，
 * 区别在于——不渲染锅，信标在下、被时定的方块在上方隔 3 格，中间光柱连接；
 * 左侧槽位放被时定方块的物品形式（供玩家确认具体物品）；无任何赘余文字。
 *
 * <p>坐标与本体时移严格同源：RenderSupport.renderBlock(81, y, z, 12, ...)，
 * 一格纵向 10px，信标底部 y=40，时定方块 y=40-3×10=10。
 *
 * <p>时定方块（过冷浮霜/超温余烬金属块）模型自带全罩发的光带描边层
 * （*_outline 贴图），在 JEI 小尺寸等轴渲染下会糊住方块本体；本分类用
 * 自定义渲染函数剔除纹理名含 outline 的面片，只画方块主体。
 */
public class TimeAnchorCategory implements IRecipeCategory<TimeAnchorRecipe> {

   public static final RecipeType<TimeAnchorRecipe> RECIPE_TYPE =
      RecipeType.create("dingdongji", "time_anchor", TimeAnchorRecipe.class);

   private static final int WIDTH = 162;
   private static final int HEIGHT = 64;

   private static final int SLOT_X = 25;
   private static final int SLOT_Y = 22;

   /** 信标中心锚点 y：较上一版下移一格（10px），为光柱让出完整跨度。 */
   private static final int BEACON_Y = 50;
   /** 上方时定方块中心锚点 y；方块底边约在 y=10。 */
   private static final int BEAM_GAP_Y = 10;
   /** 光柱高（缩放单位/格）：(50-10)/12 ≈ 3.34，柱尖正好没入方块底边，
    *  不穿透上方渲染方块。 */
   private static final float BEAM_HEIGHT = (BEACON_Y - BEAM_GAP_Y) / 12.0F;

   /** 剔除 outline 光带面片，只渲染切面主体（避免光带层糊住方块）。 */
   private static final RenderSupport.BlockRenderFunction NO_OUTLINE = (state, pose, buffer) -> {
      Minecraft mc = Minecraft.getInstance();
      BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
      var model = dispatcher.getBlockModel(state);
      RandomSource random = RandomSource.create(42L);
      for (Direction direction : Direction.values()) {
         for (BakedQuad quad : model.getQuads(state, direction, random, ModelData.EMPTY, RenderType.cutout())) {
            putQuadIfNotOutline(quad, pose, buffer);
         }
      }
      for (BakedQuad quad : model.getQuads(state, null, random, ModelData.EMPTY, RenderType.cutout())) {
         putQuadIfNotOutline(quad, pose, buffer);
      }
   };

   private static void putQuadIfNotOutline(BakedQuad quad, PoseStack pose, MultiBufferSource.BufferSource buffer) {
      if (quad.getSprite().contents().name().getPath().contains("outline")) {
         return;
      }
      VertexConsumer consumer = buffer.getBuffer(RenderType.cutout());
      consumer.putBulkData(pose.last(), quad, 1.0F, 1.0F, 1.0F, 1.0F, 15728880, OverlayTexture.NO_OVERLAY);
   }

   private final IDrawable icon;
   private final IDrawable slotDefault;
   private final Component title;
   private final BlockState beaconState;

   public TimeAnchorCategory(IGuiHelper helper) {
      this.icon = helper.createDrawableIngredient(
         VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.ANCHORED_BEACON.get()));
      // 与铁砧工艺其他 JEI 配方同款的 18×18 槽位框
      this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
      this.title = Component.translatable("jei.category.dingdongji.time_anchor");
      this.beaconState = ModBlocks.ANCHORED_BEACON.get().defaultBlockState();
   }

   @Override
   public RecipeType<TimeAnchorRecipe> getRecipeType() {
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

   @Override
   public void setRecipe(IRecipeLayoutBuilder builder, TimeAnchorRecipe recipe, IFocusGroup focuses) {
      // 仅左侧一个输入槽；槽位框在 draw() 中按铁砧工艺样式手画（见 slotDefault）
      builder.addSlot(RecipeIngredientRole.INPUT, SLOT_X, SLOT_Y)
         .addItemStack(recipe.stack());
   }

   @Override
   public void draw(
      TimeAnchorRecipe recipe,
      IRecipeSlotsView recipeSlotsView,
      GuiGraphics gui,
      double mouseX,
      double mouseY
   ) {
      // 铁砧工艺同款槽位框：槽位物品由 JEI 在 draw 之后绘制，自动盖在框上
      this.slotDefault.draw(gui, SLOT_X - 1, SLOT_Y - 1);
      // 光柱先画且不写入深度；后画的方块覆盖重叠区域，柱尖止于上方方块底边。
      renderBeaconBeam(gui);
      // 下方：铭化信标（与本体时移渲染同源）
      RenderSupport.renderBlock(gui, this.beaconState, 81, BEACON_Y, 0, 12, RenderSupport.SINGLE_BLOCK);
      // 上方隔 3 格：被时定的方块（剔除光带层）
      RenderSupport.renderBlock(gui, recipe.state(), 81, BEAM_GAP_Y, 0, 12, NO_OUTLINE);
   }

   /** 与本体 renderBeaconBeam 同款：3D 变换后画尖体光柱，高度收到上方方块底边。 */
   private static void renderBeaconBeam(GuiGraphics gui) {
      Minecraft mc = Minecraft.getInstance();
      MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
      PoseStack pose = gui.pose();
      pose.pushPose();
      pose.translate(81.0F, (float) BEACON_Y, 100.0F);
      pose.scale(12.0F, -12.0F, 12.0F);
      VertexConsumer buffer = buffers.getBuffer(ModRenderTypes.CORRUPTED_BEACON_BEAM);
      AnchoredBeaconRenderer.renderBeam(buffer, pose.last(), 0.0F, 0.0F, 0.0F, BEAM_HEIGHT, 0.65F);
      buffers.endBatch(ModRenderTypes.CORRUPTED_BEACON_BEAM);
      pose.popPose();
   }
}