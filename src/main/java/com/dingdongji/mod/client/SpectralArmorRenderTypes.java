package com.dingdongji.mod.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/**
 * 幻灵套盔甲渲染类型，实现“半透明表面 + 自身重叠面剔除”。
 *
 * 问题：半透明批次按从后往前排序渲染，背面（远）先画、前面（近）后
 * 混合。仅靠 depthMask(true) 无法剔除背面——远面绘制时深度缓冲尚空，
 * 它先写入，近面随后通过测试，结果两面颜色都留在帧缓冲。
 *
 * 方案（深度预渲染双 pass）：
 * - pass A spectral_armor_depth：不透明、不写颜色（colorMask=false）、
 *   只写深度。属于不透明批次，先于半透明批次 flush，把模型最近表面
 *   的深度写入深度缓冲；
 * - pass B spectral_armor_translucent：半透明混合，LEQUAL 深度测试。
 *   模型背面比最近表面更远，深度测试失败被剔除，只有最前面通过。
 * 视觉：透过最前面直接看到背景，看不到套装自身的其他面，与幻灵铁砧
 * 表现一致。两 pass 使用同一着色器与同一几何，逐像素深度精确相等，
 * 最近面在 pass B 必通过 LEQUAL，不丢像素、不 z-fighting。
 *
 * 本类必须在 dingdongji 自己的包内：放到 net.minecraft.* 会与
 * minecraft 模块产生 split package，模块解析直接拒绝启动。
 */
public final class SpectralArmorRenderTypes {
   private SpectralArmorRenderTypes() {
   }

   /** pass A 用：保证 depthMask(true)，不开启混合。 */
   private static final RenderStateShard.TransparencyStateShard DEPTH_ONLY =
      new RenderStateShard.TransparencyStateShard(
         "spectral_depth_only",
         () -> RenderSystem.depthMask(true),
         () -> RenderSystem.depthMask(true)
      );

   /** pass B 用：开启半透明混合，但不关闭深度写入，并显式保证 depthMask(true)。 */
   private static final RenderStateShard.TransparencyStateShard TRANSLUCENT_WITH_DEPTH =
      new RenderStateShard.TransparencyStateShard(
         "spectral_translucent_depth",
         () -> {
            RenderSystem.depthMask(true);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
               GlStateManager.SourceFactor.SRC_ALPHA,
               GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
               GlStateManager.SourceFactor.ONE,
               GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
            );
         },
         () -> {
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
         }
      );

   /** pass A：只写深度。writeMask(false, true) = 不写颜色、写深度。 */
   private static final Function<ResourceLocation, RenderType> DEPTH_PREPASS = Util.memoize(texture -> {
      RenderType.CompositeState state = RenderType.CompositeState.builder()
         // 使用 armor cutout 着色器而非 translucent：它对 alpha < 0.1 的
         // 像素 discard。贴图 alpha=0 的镂空处不写深度（不会挡住镂空处
         // 后面的地形），alpha=128 的盔甲表面正常写深度。
         .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeArmorCutoutNoCullShader))
         .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
         .setTransparencyState(DEPTH_ONLY)
         .setCullState(RenderStateShard.NO_CULL)
         .setLightmapState(RenderStateShard.LIGHTMAP)
         .setOverlayState(RenderStateShard.OVERLAY)
         .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(false, true))
         .createCompositeState(false);
      return RenderType.create(
         "spectral_armor_depth",
         DefaultVertexFormat.NEW_ENTITY,
         VertexFormat.Mode.QUADS,
         1536,
         false,
         false,
         state
      );
   });

   /** pass B：半透明 + 深度测试，重叠背面被 pass A 写入的深度剔除。 */
   private static final Function<ResourceLocation, RenderType> TRANSLUCENT = Util.memoize(texture -> {
      RenderType.CompositeState state = RenderType.CompositeState.builder()
         .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityTranslucentShader))
         .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
         .setTransparencyState(TRANSLUCENT_WITH_DEPTH)
         .setCullState(RenderStateShard.NO_CULL)
         .setLightmapState(RenderStateShard.LIGHTMAP)
         .setOverlayState(RenderStateShard.OVERLAY)
         .createCompositeState(true);
      return RenderType.create(
         "spectral_armor_translucent",
         DefaultVertexFormat.NEW_ENTITY,
         VertexFormat.Mode.QUADS,
         1536,
         true,
         true,
         state
      );
   });

   /** pass A：深度预渲染 buffer。 */
   public static RenderType spectralArmorDepth(ResourceLocation texture) {
      return DEPTH_PREPASS.apply(texture);
   }

   /** pass B：半透明 buffer。 */
   public static RenderType spectralArmor(ResourceLocation texture) {
      return TRANSLUCENT.apply(texture);
   }
}
