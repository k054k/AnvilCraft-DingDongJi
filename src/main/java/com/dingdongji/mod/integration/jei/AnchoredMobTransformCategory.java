package com.dingdongji.mod.integration.jei;

import com.dingdongji.mod.block.ModBlocks;
import com.dingdongji.mod.client.render.AnchoredBeaconRenderer;
import com.dingdongji.mod.recipe.AnchoredMobTransformRecipe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * "铭化信标内的生物转化"独立 JEI 分类（注册自己的配方类型与配方数据，
 * 完全不复用本体"腐化信标内的生物转化"配方）。
 *
 * <p>渲染与本体 {@code MobTransformCategory} 逐参同源（162×82）：
 * <ul>
 *   <li>中间：铭化信标 3D 渲染（81,48）+ 尖体光柱；</li>
 *   <li>左右各一枚 JEI 配方箭头（54,31 / 92,31）；</li>
 *   <li>左输入实体（27,61）/ 右输出实体（135,61），缩放与偏移使用本体
 *       同一套 EntityPreviewSettings（幻翼 0.55、巨人 3.0/+13/-7 等）；</li>
 *   <li>生物手持物品：实体主手渲染配方指定物品；同时在（53,10）/（91,10）
 *       提供真实 JEI 物品槽，可悬停/查询。</li>
 * </ul>
 * 反向转化为确定性 100%，无概率文字；悬停实体显示其名称。
 */
public class AnchoredMobTransformCategory implements IRecipeCategory<RecipeHolder<AnchoredMobTransformRecipe>> {
   public static final RecipeType<RecipeHolder<AnchoredMobTransformRecipe>> RECIPE_TYPE =
      RecipeType.create("dingdongji", "anchored_mob_transform", (Class)RecipeHolder.class);

   private static final int WIDTH = 162;
   private static final int HEIGHT = 82;
   private static final Quaternionf ENTITY_ANGLE = new Quaternionf().rotationXYZ(0.43633232f, 0f, (float) Math.PI);

   /** 与本体 MobTransformCategory 完全相同的特殊实体预览参数
    *  (缩放倍率, y偏移, 向内偏移)。 */
   private record PreviewSettings(float scaleMultiplier, int yOffset, int inwardOffset) {
   }

   private static final PreviewSettings DEFAULT_SETTINGS = new PreviewSettings(1.0F, 0, 0);
   private static final Map<EntityType<?>, PreviewSettings> PREVIEW_SETTINGS = Map.of(
      EntityType.PHANTOM, new PreviewSettings(0.55F, 0, 0),
      EntityType.ELDER_GUARDIAN, new PreviewSettings(1.0F, 0, 16),
      EntityType.GIANT, new PreviewSettings(3.0F, 13, -7),
      EntityType.ALLAY, new PreviewSettings(1.4F, -6, 0),
      EntityType.VEX, new PreviewSettings(1.4F, -6, 0),
      EntityType.IRON_GOLEM, new PreviewSettings(1.35F, 0, 0),
      EntityType.WARDEN, new PreviewSettings(1.4F, 0, 0)
   );

   private final IDrawable icon;
   private final IDrawable arrow;
   private final IDrawable slotBackground;
   private final Component title;
   private final BlockState beaconState;
   /** 实体缓存（随世界切换清空）：缓存的实体 tickCount 恒为 0，
    *  渲染时 ageInTicks 不随帧推进，幻翼等生物保持静止（同本体做法）。 */
   private final Map<EntityType<?>, LivingEntity> entityCache = new HashMap<>();
   private ClientLevel cachedLevel;

   public AnchoredMobTransformCategory(IGuiHelper helper) {
      this.icon = helper.createDrawableIngredient(
         VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.ANCHORED_BEACON.get()));
      // 与本体腐化信标"生物转化"分类同款的 16×10 箭头（坐标 (54,31)/(92,31) 不变）
      this.arrow = JeiRenderHelper.getArrowDefault(helper);
      // 手持物品槽背景：铁砧工艺其他 JEI 配方同款的 18×18 槽位框
      this.slotBackground = JeiRenderHelper.getSlotDefault(helper);
      this.title = Component.translatable("jei.category.dingdongji.anchored_mob_transform");
      this.beaconState = ModBlocks.ANCHORED_BEACON.get().defaultBlockState();
   }

   @Override
   public RecipeType<RecipeHolder<AnchoredMobTransformRecipe>> getRecipeType() {
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
   public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<AnchoredMobTransformRecipe> holder, IFocusGroup focuses) {
      AnchoredMobTransformRecipe recipe = holder.value();
      // 铭化信标为催化剂（关联入口）
      builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST)
         .addItemStack(new ItemStack(ModBlocks.ANCHORED_BEACON.get()));
      // 生物手持物品槽（与本体同位置），无物品则不布槽；
      // 有手持条件/变体时优先显示条件物品（如巨人的巨型铁砧）
      ItemStack heldDisplay = !recipe.inputHeld().isEmpty() ? recipe.inputHeld()
         : (!recipe.variants().isEmpty() ? recipe.variants().get(0).inputHeld() : recipe.inputItem());
      if (!heldDisplay.isEmpty()) {
         builder.addSlot(RecipeIngredientRole.INPUT, 53, 10)
            .setSlotName("input_item")
            .addItemStack(heldDisplay);
      }
      // 输出物品：多变体配方（如巨人的三条逆向）循环展示全部结果手持
      List<ItemStack> outputs = new java.util.ArrayList<>();
      if (!recipe.variants().isEmpty()) {
         for (AnchoredMobTransformRecipe.Variant variant : recipe.variants()) {
            if (!variant.resultItem().isEmpty()) {
               outputs.add(variant.resultItem());
            }
         }
      } else if (!recipe.resultItem().isEmpty()) {
         outputs.add(recipe.resultItem());
      }
      if (!outputs.isEmpty()) {
         builder.addSlot(RecipeIngredientRole.OUTPUT, 91, 10)
            .addItemStacks(outputs);
      }
   }

   @Override
   public void draw(
      RecipeHolder<AnchoredMobTransformRecipe> holder,
      IRecipeSlotsView view,
      GuiGraphics gui,
      double mouseX,
      double mouseY
   ) {
      AnchoredMobTransformRecipe recipe = holder.value();
      // 光柱先画：信标渲染写入深度，后画会导致 z=100 的光柱被遮挡截断。
      renderBeaconBeam(gui);
      RenderSupport.renderBlock(gui, this.beaconState, 81, 48, 10, 12, RenderSupport.SINGLE_BLOCK);
      this.arrow.draw(gui, 54, 31);
      this.arrow.draw(gui, 92, 31);

      // 手持物品槽背景（与本体同位置 52,9 / 90,9）
      if (!recipe.inputItem().isEmpty() || !recipe.inputHeld().isEmpty() || !recipe.variants().isEmpty()) {
         this.slotBackground.draw(gui, 52, 9);
      }
      // 90,9 输出槽背景同本体：仅有结果物品时绘制（原 resultItem 单值判断已覆盖多变体）
      if (!recipe.resultItem().isEmpty() || !recipe.variants().isEmpty()) {
         this.slotBackground.draw(gui, 90, 9);
      }

      // 多输入/多变体时每 1.5 秒切换显示（与本体多结果循环同节奏），
      // 输入实体手持当前变体的条件物品，输出实体手持对应结果物品
      // （对标本体腐化信标 JEI 的实体手持表现）
      int index = 0;
      if (!recipe.variants().isEmpty()) {
         index = (int) (net.minecraft.Util.getMillis() / 1500L % recipe.variants().size());
      }
      AnchoredMobTransformRecipe.Variant displayedVariant =
         recipe.variants().isEmpty() ? null : recipe.variants().get(index);
      EntityType<?> displayedInput = getDisplayedInput(recipe);
      ItemStack inputHeld = displayedVariant != null ? displayedVariant.inputHeld()
         : (!recipe.inputHeld().isEmpty() ? recipe.inputHeld() : recipe.inputItem());
      ItemStack resultHeld = displayedVariant != null ? displayedVariant.resultItem() : recipe.resultItem();
      renderEntity(gui, displayedInput, 27, 61, 1, inputHeld);
      renderEntity(gui, recipe.result(), 135, 61, -1, resultHeld);
   }

   /** 多输入配方循环显示：每 1.5 秒切换到下一个输入实体（与本体 getDisplayedResult 同机制）。 */
   private static EntityType<?> getDisplayedInput(AnchoredMobTransformRecipe recipe) {
      List<EntityType<?>> inputs = recipe.inputs();
      int index = (int) (net.minecraft.Util.getMillis() / 1500L % inputs.size());
      return inputs.get(index);
   }

   /** 同本体 getEntity：按类型缓存实体，世界切换时重建。 */
   private LivingEntity getEntity(EntityType<?> type) {
      Minecraft mc = Minecraft.getInstance();
      ClientLevel level = mc.level;
      if (level == null) {
         return null;
      }
      if (level != this.cachedLevel) {
         this.entityCache.clear();
         this.cachedLevel = level;
      }
      return this.entityCache.computeIfAbsent(type, t -> {
         net.minecraft.world.entity.Entity entity = t.create(level);
         return entity instanceof LivingEntity living ? living : null;
      });
   }

   /** 与本体 renderBeaconBeam 同款：3D 变换后画尖体光柱（高 4.8、宽缩放 0.65）。 */
   private static void renderBeaconBeam(GuiGraphics gui) {
      Minecraft mc = Minecraft.getInstance();
      MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
      PoseStack pose = gui.pose();
      pose.pushPose();
      pose.translate(81.0F, 48.0F, 100.0F);
      pose.scale(12.0F, -12.0F, 12.0F);
      VertexConsumer buffer = buffers.getBuffer(ModRenderTypes.CORRUPTED_BEACON_BEAM);
      AnchoredBeaconRenderer.renderBeam(buffer, pose.last(), 0.0F, 0.0F, 0.0F, 4.8F, 0.65F);
      buffers.endBatch(ModRenderTypes.CORRUPTED_BEACON_BEAM);
      pose.popPose();
   }

   @Override
   public void getTooltip(
      ITooltipBuilder tooltip,
      RecipeHolder<AnchoredMobTransformRecipe> holder,
      IRecipeSlotsView recipeSlotsView,
      double mouseX,
      double mouseY
   ) {
      if (mouseX >= 12 && mouseX <= 42 && mouseY >= 30 && mouseY <= 82) {
         tooltip.add(getDisplayedInput(holder.value()).getDescription());
      } else if (mouseX >= 120 && mouseX <= 150 && mouseY >= 30 && mouseY <= 82) {
         tooltip.add(holder.value().result().getDescription());
      }
   }

   /**
    * 与本体 MobTransformCategory.renderEntity 完全同源：
    * 基础缩放 clamp(30/最大包围盒尺寸,2,24)，乘实体专属倍率后再 clamp 到 32；
    * 渲染期间把配方物品放到主手，渲染完立即清空（缓存实体跨配方复用）。
    */
   private void renderEntity(GuiGraphics gui, EntityType<?> type, int x, int y, int inwardDirection, ItemStack held) {
      LivingEntity living = getEntity(type);
      if (living == null) {
         return;
      }
      PreviewSettings settings = PREVIEW_SETTINGS.getOrDefault(type, DEFAULT_SETTINGS);
      living.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, held.copy());
      // 冻结动画：tickCount 恒为 0，模型动画（如幻翼扇翅）停留在同一帧
      living.tickCount = 0;
      living.setYRot(205f);
      living.yBodyRot = 205f;
      living.yHeadRot = 205f;
      float maxDimension = Math.max(living.getBbWidth(), living.getBbHeight());
      int baseScale = Mth.clamp(Math.round(30f / maxDimension), 2, 24);
      int scale = Mth.clamp(Math.round(baseScale * settings.scaleMultiplier()), 2, 32);
      float px = x + settings.inwardOffset() * inwardDirection;
      float py = y + settings.yOffset();
      InventoryScreen.renderEntityInInventory(
         gui, px, py, scale, new Vector3f(), ENTITY_ANGLE, null, living);
      living.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
   }
}
