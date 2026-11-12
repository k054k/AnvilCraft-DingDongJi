package com.dingdongji.mod.client;

import com.dingdongji.mod.block.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.model.data.ModelData;
import java.util.List;

/**
 * 附魔重质金块的世界附魔光效（完全独立于本体 EnchantedGoldBlockGlints）。
 *
 * <p>两个问题的修复：
 * <ul>
 *   <li>重进世界光效消失：位置集合自维护——区块加载时扫描 + 每 20 tick
 *             对玩家身边 5×5 区块做增量扫描（section 调色盘预判），不再依赖
 *             本体 scanChunk 的 solid 层优化；</li>
 *   <li>镂空处也铺满光：双 pass——pass A 用 cutout shader 绘制模型几何
 *       只写深度（贴图 alpha 镂空像素 discard，不写深度）；pass B 用
 *       SheetedDecal 贴 glint，深度测试为 EQUAL，于是只有贴图不透明像素
 *       处的几何面深度相等、光效通过，镂空处深度是后方地形、光效被剔除。</li>
 * </ul>
 */
@EventBusSubscriber({Dist.CLIENT})
public final class EnchantedHeavyGoldGlints {
   private static final int FULL_LIGHT = 15728880;
   private static final float GLINT_TEXTURE_SCALE = 0.0078125F;
   private static final double RENDER_DISTANCE_SQR = 64.0 * 64.0;
   private static final int RESCAN_INTERVAL = 20;
   private static final int RESCAN_RADIUS_CHUNKS = 2;

   private static final LongSet POSITIONS = new LongOpenHashSet();
   private static ClientLevel trackedLevel;
   private static int tickCounter;

   /** 行走/转镜头时不闪烁的深多边形偏移：区块地形与覆盖层顶点管线不同，
    * 深度存在 1 ulp 级误差，偏移量需显著大于该误差（-4/-6 远大于
    * VIEW_OFFSET_Z_LAYERING 的 -1/-10 组合中的有效偏移）。 */
   private static final RenderStateShard.LayeringStateShard DDJ_POLYGON_OFFSET =
      new RenderStateShard.LayeringStateShard(
         "ddj_polygon_offset",
         () -> {
            com.mojang.blaze3d.platform.GlStateManager._polygonOffset(-4.0F, -6.0F);
            com.mojang.blaze3d.platform.GlStateManager._enablePolygonOffset();
         },
         () -> {
            com.mojang.blaze3d.platform.GlStateManager._polygonOffset(0.0F, 0.0F);
            com.mojang.blaze3d.platform.GlStateManager._disablePolygonOffset();
         }
      );

   /** pass A：cutout 模型几何只写深度，镂空像素随 shader discard 不写深度。 */
   private static final RenderType DEPTH_MASK = RenderType.create(
      "ddj_enchanted_heavy_gold_depth",
      com.mojang.blaze3d.vertex.DefaultVertexFormat.BLOCK,
      com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
      2097152,
      false,
      false,
      CompositeState.builder()
         .setShaderState(new RenderStateShard.ShaderStateShard(
            net.minecraft.client.renderer.GameRenderer::getRendertypeCutoutShader))
         .setTextureState(new TextureStateShard(InventoryMenu.BLOCK_ATLAS, false, false))
         .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
         .setLightmapState(RenderStateShard.LIGHTMAP)
         .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(false, true))
         .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
         .setLayeringState(DDJ_POLYGON_OFFSET)
         .createCompositeState(false)
   );

   /** pass B：glint 贴片，EQUAL 深度测试——只在 pass A 写入了深度的像素上显示。 */
   private static final RenderType GLINT = RenderType.create(
      "ddj_enchanted_heavy_gold_glint",
      com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX,
      com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
      2097152,
      false,
      false,
      CompositeState.builder()
         .setShaderState(RenderStateShard.RENDERTYPE_GLINT_SHADER)
         .setTextureState(new TextureStateShard(ItemRenderer.ENCHANTED_GLINT_ITEM, true, false))
         .setWriteMaskState(RenderStateShard.COLOR_WRITE)
         .setCullState(RenderStateShard.NO_CULL)
         .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
         .setTransparencyState(RenderStateShard.GLINT_TRANSPARENCY)
         .setTexturingState(RenderStateShard.GLINT_TEXTURING)
         // 与 pass A 同一偏移：两 pass 顶点与偏移完全一致，深度位级相等，
         // EQUAL 测试稳定通过（不动与移动都不闪）。
         .setLayeringState(DDJ_POLYGON_OFFSET)
         .createCompositeState(false)
   );

   private EnchantedHeavyGoldGlints() {
   }

   @SubscribeEvent
   public static void onChunkLoad(ChunkEvent.Load event) {
      if (event.getLevel() instanceof ClientLevel clientLevel && event.getChunk() instanceof LevelChunk chunk) {
         ensureLevel(clientLevel);
         scanChunk(clientLevel, chunk);
      }
   }

   /** 玩家放下附魔重质金块：立即收录位置，不等下一个 20 tick 重扫周期。 */
   @SubscribeEvent
   public static void onBlockPlaced(net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent event) {
      if (event.getLevel() instanceof ClientLevel clientLevel
         && event.getPlacedBlock().is((Block)ModBlocks.ENCHANTED_HEAVY_GOLD_BLOCK.get())) {
         ensureLevel(clientLevel);
         POSITIONS.add(event.getPos().asLong());
      }
   }

   @SubscribeEvent
   public static void onClientLevelTick(LevelTickEvent.Post event) {
      if (!(event.getLevel() instanceof ClientLevel clientLevel)) {
         return;
      }
      ensureLevel(clientLevel);
      if (++tickCounter % RESCAN_INTERVAL != 0) {
         return;
      }
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return;
      }
      ChunkPos center = new ChunkPos(mc.player.blockPosition());
      for (int dx = -RESCAN_RADIUS_CHUNKS; dx <= RESCAN_RADIUS_CHUNKS; dx++) {
         for (int dz = -RESCAN_RADIUS_CHUNKS; dz <= RESCAN_RADIUS_CHUNKS; dz++) {
            int cx = center.x + dx;
            int cz = center.z + dz;
            if (clientLevel.getChunkSource().hasChunk(cx, cz)) {
               LevelChunk chunk = clientLevel.getChunk(cx, cz);
               scanChunk(clientLevel, chunk);
            }
         }
      }
   }

   private static void ensureLevel(ClientLevel level) {
      if (trackedLevel != level) {
         trackedLevel = level;
         POSITIONS.clear();
      }
   }

   /**
    * 供客户端 LevelChunk.setBlockState Mixin 在方块写入同帧调用：玩家放下附魔
    * 重质金块的当帧即收录位置，本帧渲染就出现光效，不必等 20 tick 增量重扫。
    */
   public static void addPosition(ClientLevel level, BlockPos pos) {
      ensureLevel(level);
      POSITIONS.add(pos.asLong());
   }

   /** section 调色盘预判：只有可能包含重金块的段才逐方块遍历。 */
   private static void scanChunk(ClientLevel level, LevelChunk chunk) {
      Block block = (Block) ModBlocks.ENCHANTED_HEAVY_GOLD_BLOCK.get();
      int minY = level.getMinBuildHeight() >> 4;
      LevelChunkSection[] sections = chunk.getSections();
      for (int i = 0; i < sections.length; i++) {
         LevelChunkSection section = sections[i];
         if (section == null || section.hasOnlyAir() || !section.maybeHas(state -> state.is(block))) {
            continue;
         }
         int baseX = chunk.getPos().getMinBlockX();
         int baseZ = chunk.getPos().getMinBlockZ();
         int baseY = (minY + i) << 4;
         for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
               for (int z = 0; z < 16; z++) {
                  if (section.getBlockState(x, y, z).is(block)) {
                     POSITIONS.add(BlockPos.asLong(baseX + x, baseY + y, baseZ + z));
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES || POSITIONS.isEmpty()) {
         return;
      }
      Minecraft mc = Minecraft.getInstance();
      ClientLevel level = mc.level;
      if (level == null) {
         return;
      }
      Block block = (Block) ModBlocks.ENCHANTED_HEAVY_GOLD_BLOCK.get();
      BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
      Vec3 camPos = event.getCamera().getPosition();
      PoseStack pose = event.getPoseStack();
      MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
      RandomSource random = RandomSource.create(42L);

      LongOpenHashSet removed = new LongOpenHashSet();
      for (long posLong : POSITIONS) {
         BlockPos pos = BlockPos.of(posLong);
         BlockState state = level.getBlockState(pos);
         if (!state.is(block)) {
            removed.add(posLong);
            continue;
         }
         if (Vec3.atCenterOf(pos).distanceToSqr(camPos) > RENDER_DISTANCE_SQR) {
            continue;
         }

         var model = dispatcher.getBlockModel(state);
         pose.pushPose();
         pose.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);

         // pass A：模型几何（cutout 层）只写深度
         VertexConsumer depthConsumer = buffers.getBuffer(DEPTH_MASK);
         forEachQuad(model, state, level, pos, random, pose.last(),
            (quad, poseEntry) -> depthConsumer.putBulkData(poseEntry, quad,
               1.0F, 1.0F, 1.0F, 1.0F, FULL_LIGHT, OverlayTexture.NO_OVERLAY));
         buffers.endBatch(DEPTH_MASK);

         // pass B：沿模型 quad 贴 glint，EQUAL 深度测试在镂空像素处失败
         VertexConsumer glintConsumer = new SheetedDecalTextureGenerator(
            buffers.getBuffer(GLINT), pose.last(), GLINT_TEXTURE_SCALE);
         forEachQuad(model, state, level, pos, random, pose.last(),
            (quad, poseEntry) -> glintConsumer.putBulkData(poseEntry, quad,
               1.0F, 1.0F, 1.0F, 1.0F, FULL_LIGHT, OverlayTexture.NO_OVERLAY));
         buffers.endBatch(GLINT);

         pose.popPose();
      }
      POSITIONS.removeAll(removed);
   }

   private interface QuadConsumer {
      void accept(BakedQuad quad, PoseStack.Pose pose);
   }

   /** 与本体一致：6 个可见面 + 无方向 quad；使用 cutout 层取重金块模型面片。 */
   private static void forEachQuad(
      net.minecraft.client.resources.model.BakedModel model,
      BlockState state,
      Level level,
      BlockPos pos,
      RandomSource random,
      PoseStack.Pose poseEntry,
      QuadConsumer out
   ) {
      for (Direction direction : Direction.values()) {
         if (!net.minecraft.world.level.block.Block.shouldRenderFace(
            state, level, pos, direction, pos.relative(direction))) {
            continue;
         }
         random.setSeed(42L);
         for (BakedQuad quad : model.getQuads(
            state, direction, random, ModelData.EMPTY, RenderType.cutout())) {
            out.accept(quad, poseEntry);
         }
      }
      random.setSeed(42L);
      for (BakedQuad quad : model.getQuads(state, null, random, ModelData.EMPTY, RenderType.cutout())) {
         out.accept(quad, poseEntry);
      }
   }
}
