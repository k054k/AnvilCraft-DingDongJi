package com.dingdongji.mod.mixin;

import com.dingdongji.mod.integration.jei.DdjBeaconConversionRecipe;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.integration.jei.category.BeaconConversionCategory;
import dev.dubhe.anvilcraft.integration.jei.recipe.BeaconConversionRecipe;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.util.LevelLike;
import java.util.HashMap;
import java.util.Map;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 接管本体"信标转化"JEI 分类：识别我方 {@link DdjBeaconConversionRecipe}
 * （继承本体配方类）时按本体同款坐标布槽、绘制 3D 金字塔与槽位框后
 * 取消原逻辑；本体配方不受影响。使我方配方完全呈现在本体分类中。
 *
 * <p>{@code @Pseudo}：目标类属于本体的 JEI 集成，JEI 未安装时静默跳过。
 */
@Pseudo
@Mixin(value = BeaconConversionCategory.class, remap = false)
public abstract class BeaconConversionCategoryMixin {
   @Shadow
   @Final
   private IDrawable slotDefault;

   @Shadow
   @Final
   private IDrawable slotChoice;

   @Shadow
   @Final
   protected IDrawable arrowIn;

   @Shadow
   @Final
   protected IDrawable arrowDefault;

   private static final Map<DdjBeaconConversionRecipe, LevelLike> CACHE = new HashMap<>();

   @Inject(method = "setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;"
      + "Ldev/dubhe/anvilcraft/integration/jei/recipe/BeaconConversionRecipe;"
      + "Lmezz/jei/api/recipe/IFocusGroup;)V", at = @At("HEAD"), cancellable = true)
   private void ddj$setRecipe(IRecipeLayoutBuilder builder, BeaconConversionRecipe recipe, IFocusGroup focuses, CallbackInfo ci) {
      if (!(recipe instanceof DdjBeaconConversionRecipe r)) {
         return;
      }
      if (r.anvilTriggered) {
         builder.addSlot(RecipeIngredientRole.INPUT, 48, 8)
            .addItemStack(r.ingot)
            .addRichTooltipCallback((slotView, tooltip) -> tooltip.add(
               Component.translatable("tooltip.dingdongji.jei.beacon_conversion.anvil")
                  .withStyle(ChatFormatting.GRAY)));
      }
      builder.addSlot(RecipeIngredientRole.INPUT, 10, 92)
         .addItemStack(Blocks.BEACON.asItem().getDefaultInstance());
      builder.addSlot(RecipeIngredientRole.CATALYST, 10, 110)
         .addItemStack(r.base)
         .addRichTooltipCallback((slotView, tooltip) -> {
            if (!r.anvilTriggered) {
               tooltip.add(Component.translatable("tooltip.dingdongji.jei.beacon_conversion.auto")
                  .withStyle(ChatFormatting.GRAY));
            }
         });
      // 概率显示与本体完全同款：ChanceItemStack 数量 + NumberProvider 概率 tooltip
      var outputSlot = builder.addSlot(RecipeIngredientRole.OUTPUT, 130, 96);
      outputSlot.addItemStack(r.result);
      JeiRecipeUtil.addTooltips(
         outputSlot,
         r.corruptedBeaconOutput.stack().getCount(),
         r.corruptedBeaconOutput.count()
      );
      if (r.chance < 1.0F) {
         var failSlot = builder.addSlot(RecipeIngredientRole.OUTPUT, 112, 96);
         failSlot.addItemStack(Blocks.BEACON.asItem().getDefaultInstance());
         JeiRecipeUtil.addTooltips(
            failSlot,
            r.beaconOutput.stack().getCount(),
            r.beaconOutput.count()
         );
      }
      ci.cancel();
   }

   @Inject(method = "draw(Ldev/dubhe/anvilcraft/integration/jei/recipe/BeaconConversionRecipe;"
      + "Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;Lnet/minecraft/client/gui/GuiGraphics;DD)V",
      at = @At("HEAD"), cancellable = true)
   private void ddj$draw(BeaconConversionRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics gui,
      double mouseX, double mouseY, CallbackInfo ci) {
      if (!(recipe instanceof DdjBeaconConversionRecipe r)) {
         return;
      }
      LevelLike levelLike = CACHE.get(r);
      if (levelLike == null) {
         levelLike = buildPyramid(r);
         if (levelLike != null) {
            CACHE.put(r, levelLike);
         }
      }
      if (levelLike != null) {
         RenderSupport.renderLevelLike(levelLike, gui, 84, 54, 90.0F);
      }
      this.slotDefault.draw(gui, 47, 7);
      this.slotDefault.draw(gui, 9, 109);
      this.slotDefault.draw(gui, 9, 91);
      if (r.chance < 1.0F) {
         this.slotChoice.draw(gui, 111, 95);
         this.slotChoice.draw(gui, 129, 95);
      } else {
         this.slotDefault.draw(gui, 129, 95);
      }
      this.arrowIn.draw(gui, 66, 14);
      this.arrowDefault.draw(gui, 60, 96);
      ci.cancel();
   }

   /** 与本体一致：角点坐标系（第 i 层为 [i, 2L-i] 方阵），信标放 (L,L,L) 且水平放置。 */
   private static LevelLike buildPyramid(DdjBeaconConversionRecipe recipe) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null || !(recipe.base.getItem() instanceof BlockItem baseItem)) {
         return null;
      }
      LevelLike levelLike = new LevelLike(minecraft.level);
      BlockState baseState = baseItem.getBlock().defaultBlockState();
      int layers = recipe.cursedGoldBlockLayers;
      for (int i = 0; i < layers; i++) {
         for (int x = i; x <= 2 * layers - i; x++) {
            for (int z = i; z <= 2 * layers - i; z++) {
               levelLike.setBlockState(new BlockPos(x, i, z), baseState);
            }
         }
      }
      BlockState beaconState = recipe.result.getItem() instanceof BlockItem beaconItem
         ? beaconItem.getBlock().defaultBlockState().trySetValue(BlockStateProperties.WATERLOGGED, false)
         : Blocks.BEACON.defaultBlockState();
      levelLike.setBlockState(new BlockPos(layers, layers, layers), beaconState);
      return levelLike;
   }
}
