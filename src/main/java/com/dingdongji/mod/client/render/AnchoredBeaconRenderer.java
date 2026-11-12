package com.dingdongji.mod.client.render;

import com.dingdongji.mod.block.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * 铭化信标光柱渲染器：替换原版信标方块实体渲染器（原版信标委托给原版
 * BeaconRenderer，行为不变）。
 *
 * 原版信标被遮挡时 beamSections 清空、光柱整根消失；这里对铭化信标复刻
 * 腐化信标的锥体光柱（CorruptedBeaconRenderer 同款几何与层叠参数），高度
 * 改为渲染时向上扫描第一个遮光方块（扫描条件与腐化信标 tick 一致：染色
 * 玻璃/透光方块放行，基岩放行，否则顶到建筑上限），被遮挡时锥体只变短
 * 不消失。光柱为纯色顶点着色（无贴图）：核心偏白色，边缘辉光泛金色。
 *
 * 光柱存在条件与转化条件一致：正下方一层 3×3 全为附魔重质金块。
 */
public class AnchoredBeaconRenderer implements BlockEntityRenderer<BeaconBlockEntity> {
   /** 光柱核心：偏白色。 */
   private static final float CORE_R = 1.00F;
   private static final float CORE_G = 1.00F;
   private static final float CORE_B = 0.95F;
   /** 光柱边缘辉光：金色。JEI 中通过 scale=0.65 收窄辉光层保持尖体观感，
    *  世界内保持金色外边缘。 */
   private static final float GLOW_R = 1.00F;
   private static final float GLOW_G = 0.85F;
   private static final float GLOW_B = 0.30F;
   /** 玻璃罩：黄偏紫。 */
   private static final float GLASS_R = 1.00F;
   private static final float GLASS_G = 0.75F;
   private static final float GLASS_B = 0.82F;
   private static final float GLASS_A = 0.30F;
   private static final int GLOW_LAYERS = 4;
   private static Block enchantedGold;

   private final BeaconRenderer vanilla;
   private final BlockRenderDispatcher blockRenderer;
   private final BlockState glassState;

   public AnchoredBeaconRenderer(BlockEntityRendererProvider.Context context) {
      this.vanilla = new BeaconRenderer(context);
      this.blockRenderer = context.getBlockRenderDispatcher();
      this.glassState = Blocks.WHITE_CONCRETE.defaultBlockState();
   }

   @Override
   public void render(BeaconBlockEntity beacon, float partialTick, PoseStack pose, MultiBufferSource buffers, int packedLight, int packedOverlay) {
      if (!beacon.getBlockState().is(ModBlocks.ANCHORED_BEACON.get())) {
         this.vanilla.render(beacon, partialTick, pose, buffers, packedLight, packedOverlay);
         return;
      }
      // 玻璃罩为铭化信标方块的外观组成：无论是否有附魔金底座（是否激活）
      // 都常驻渲染；底座只决定光柱与恢复/锁定等激活效果。
      renderGlassOverlay(pose, buffers, packedLight, packedOverlay);
      Level level = beacon.getLevel();
      if (level == null || !hasEnchantedGoldBase(level, beacon.getBlockPos())) {
         return;
      }
      BlockPos pos = beacon.getBlockPos();
      float height = scanBeamTop(level, pos) - pos.getY() - 0.5F;
      renderBeam(buffers.getBuffer(ModRenderTypes.CORRUPTED_BEACON_BEAM), pose.last(), 0.5F, 0.5F, 0.5F, height);
   }

   /** 复刻腐化信标的玻璃罩：白混凝土模型 + 半透明黄偏紫顶点色（同 BEACON_GLASS 渲染类型）。 */
   private void renderGlassOverlay(PoseStack pose, MultiBufferSource buffers, int packedLight, int packedOverlay) {
      BakedModel glassModel = blockRenderer.getBlockModel(glassState);
      pose.pushPose();
      pose.translate(0.005F, 0.005F, 0.005F);
      pose.scale(0.99F, 0.99F, 0.99F);
      VertexConsumer buffer = buffers.getBuffer(ModRenderTypes.BEACON_GLASS);
      for (Direction direction : Direction.values()) {
         for (BakedQuad quad : glassModel.getQuads(glassState, direction, RandomSource.create(), ModelData.EMPTY, null)) {
            buffer.putBulkData(pose.last(), quad, GLASS_R, GLASS_G, GLASS_B, GLASS_A, packedLight, packedOverlay);
         }
      }
      pose.popPose();
   }

   private static boolean hasEnchantedGoldBase(Level level, BlockPos pos) {
      BlockPos baseCenter = pos.below();
      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            BlockState state = level.getBlockState(baseCenter.offset(dx, 0, dz));
            if (!state.is(ModBlocks.ENCHANTED_HEAVY_GOLD_BLOCK.get()) && !state.is(enchantedGoldBlock())) {
               return false;
            }
         }
      }
      return true;
   }

   /** 本体附魔金块（懒加载缓存，客户端使用时注册表已冻结）。 */
   private static Block enchantedGoldBlock() {
      if (enchantedGold == null) {
         enchantedGold = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("anvilcraft", "enchanted_gold_block"));
      }
      return enchantedGold;
   }

   private static int scanBeamTop(Level level, BlockPos pos) {
      int maxY = level.getMaxBuildHeight();
      BlockPos.MutableBlockPos cursor = pos.above().mutable();
      while (cursor.getY() <= maxY) {
         BlockState state = level.getBlockState(cursor);
         if (state.getBeaconColorMultiplier(level, cursor, pos) == null
            && state.getLightBlock(level, cursor) >= 15
            && !state.is(Blocks.BEDROCK)) {
            return cursor.getY();
         }
         cursor.move(Direction.UP);
      }
      return maxY;
   }

   /** 与腐化信标一致：4 层辉光由外到内 + 1 层核心，顶点上乘 topAlphaFactor 在柱顶淡出。
    *  scale 缩小辉光层宽度（本体 JEI 用 0.65，世界内为 1.0）。 */
   public static void renderBeam(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float height) {
      renderBeam(buffer, pose, x, y, z, height, 1.0F);
   }

   /** 与腐化信标一致：4 层辉光由外到内（暗白色） + 1 层核心（偏白色），顶点淡出在柱顶。 */
   public static void renderBeam(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float height, float scale) {
      float topY = y + height;
      for (int i = GLOW_LAYERS; i >= 1; i--) {
         float half = 0.08F + 0.06F * i * scale;
         float fade = 1.0F / (i + 1);
         fade *= fade;
         emitBeamPyramid(buffer, pose, x, y, z, half, topY, 0.45F * fade, 0.3F * fade, GLOW_R, GLOW_G, GLOW_B);
      }
      emitBeamPyramid(buffer, pose, x, y, z, 0.08F, topY, 0.82F, 0.25F, CORE_R, CORE_G, CORE_B);
   }

   /** 每面一个三角形：底部方形相邻两角 + 柱顶中心点，顶点上乘 topAlphaFactor。 */
   private static void emitBeamPyramid(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float half, float topY, float alpha, float topAlphaFactor, float r, float g, float b) {
      float[][] corners = {{x - half, z - half}, {x + half, z - half}, {x + half, z + half}, {x - half, z + half}};
      float topAlpha = alpha * topAlphaFactor;
      for (int i = 0; i < 4; i++) {
         float[] c0 = corners[i];
         float[] c1 = corners[(i + 1) % 4];
         buffer.addVertex(pose, c0[0], y, c0[1]).setColor(r, g, b, alpha);
         buffer.addVertex(pose, c1[0], y, c1[1]).setColor(r, g, b, alpha);
         buffer.addVertex(pose, x, topY, z).setColor(r, g, b, topAlpha);
      }
   }

   @Override
   public boolean shouldRenderOffScreen(BeaconBlockEntity beacon) {
      return true;
   }

   @Override
   public int getViewDistance() {
      return 256;
   }

   @Override
   public boolean shouldRender(BeaconBlockEntity beacon, Vec3 cameraPos) {
      return Vec3.atCenterOf(beacon.getBlockPos()).multiply(1.0, 0.0, 1.0)
         .closerThan(cameraPos.multiply(1.0, 0.0, 1.0), this.getViewDistance());
   }

   @Override
   public AABB getRenderBoundingBox(BeaconBlockEntity beacon) {
      BlockPos pos = beacon.getBlockPos();
      Level level = beacon.getLevel();
      int top = level != null ? level.getMaxBuildHeight() : pos.getY() + 1;
      return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, top, pos.getZ() + 1);
   }
}
