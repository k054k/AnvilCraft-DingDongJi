package com.dingdongji.mod.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

/**
 * 幻影本体皮肤的"单 pass 纹理合并器"。
 *
 * <p>幻影玩家半透明渲染时，皮肤第二层（帽/外套/双袖/双裤）几何比第一层
 * 外扩 0.5px，过去靠深度双 pass 消除两层半透明叠色，但深度批次与半透明
 * 批次的帧间时序会让第一层/第二层交界处闪烁。本工具在皮肤纹理就绪后，
 * 把六个第二图层区域的非透明像素 alpha 合成（over）到对应的第一层 UV
 * 区域，得到一张"单层皮肤"贴图：渲染时隐藏第二层几何、只画一次，既保留
 * 第二层的贴图内容，又彻底消除叠色与闪烁。
 *
 * <p>贴图来源：优先读取 SkinManager 的本地缓存文件（root/&lt;sha1前2位&gt;/
 * &lt;sha1&gt;，对应 {@code minecraft:skins/<sha1>}）；默认皮肤等资源包内
 * 贴图则回退到 ResourceManager。读取与像素合成在 IO 线程完成，
 * {@link DynamicTexture} 的注册（GL 上传）只在渲染线程进行；合成完成前
 * 调用方回退为普通皮肤渲染。
 *
 * <p>支持高清皮肤：所有 UV 坐标按 {@code width/64} 缩放；源/目标区域
 * 越界（经典 64x32 皮肤没有第二图层）时自动跳过。
 */
public final class PhantomSkinMerger {
   /** 第二图层 → 第一图层的 UV 搬运区域（64 基准）：srcX, srcY, dstX, dstY, w, h。 */
   private static final int[][] OVERLAY_REGIONS = {
      {32,  0,  0,  0, 32, 16}, // hat        → head
      { 0, 32,  0, 16, 16, 16}, // right pants→ right leg
      {16, 32, 16, 16, 24, 16}, // jacket     → body
      {40, 32, 40, 16, 16, 16}, // right sleeve→right arm
      { 0, 48, 16, 48, 16, 16}, // left pants → left leg
      {48, 48, 32, 48, 16, 16}  // left sleeve→ left arm
   };

   private static final Map<ResourceLocation, Object> CACHE = new ConcurrentHashMap<>();
   private static final Object FAILED = new Object();

   private static boolean initDone;
   private static Path skinRoot;

   private PhantomSkinMerger() {
   }

   /**
    * 返回已合成的单层皮肤贴图 RL；尚未就绪（首次请求/合成失败/反射不可用）
    * 时返回 {@code null}，调用方应回退为原贴图且不隐藏第二层几何。
    */
   public static ResourceLocation getMergedSkin(ResourceLocation skin) {
      Object value = CACHE.get(skin);
      if (value == null) {
         startBuild(skin);
         return null;
      }
      if (value instanceof ResourceLocation merged) {
         return merged;
      }
      if (value instanceof CompletableFuture<?> future) {
         if (!future.isDone() || future.isCompletedExceptionally()) {
            if (future.isCompletedExceptionally()) {
               CACHE.put(skin, FAILED);
            }
            return null;
         }
         Object result;
         try {
            result = future.join();
         } catch (Throwable t) {
            CACHE.put(skin, FAILED);
            return null;
         }
         if (!(result instanceof NativeImage image)) {
            CACHE.put(skin, FAILED);
            return null;
         }
         ResourceLocation merged = register(skin, image);
         CACHE.put(skin, merged != null ? merged : FAILED);
         return merged;
      }
      return null;
   }

   private static void startBuild(ResourceLocation skin) {
      Path root = cacheRoot();
      CompletableFuture<NativeImage> future = CompletableFuture.supplyAsync(() -> {
         try {
            return build(skin, root);
         } catch (Throwable t) {
            return null;
         }
      }, Util.ioPool());
      CACHE.put(skin, future);
   }

   /** 反射一次拿到 SkinManager.TextureCache.root（皮肤缓存目录）。 */
   private static Path cacheRoot() {
      if (!initDone) {
         initDone = true;
         try {
            SkinManager manager = Minecraft.getInstance().getSkinManager();
            Field cacheField = SkinManager.class.getDeclaredField("skinTextures");
            cacheField.setAccessible(true);
            Object cache = cacheField.get(manager);
            Field rootField = cache.getClass().getDeclaredField("root");
            rootField.setAccessible(true);
            skinRoot = (Path) rootField.get(cache);
         } catch (Throwable t) {
            skinRoot = null;
         }
      }
      return skinRoot;
   }

   private static NativeImage build(ResourceLocation skin, Path root) throws Exception {
      String file = skin.getPath().substring(skin.getPath().lastIndexOf('/') + 1);
      NativeImage image = null;
      InputStream in = null;
      try {
         if (root != null && skin.getPath().startsWith("skins/")) {
            String prefix = file.length() > 2 ? file.substring(0, 2) : "xx";
            Path cached = root.resolve(prefix).resolve(file);
            if (Files.isRegularFile(cached)) {
               in = Files.newInputStream(cached);
            }
         }
         if (in == null) {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(skin);
            if (resource.isPresent()) {
               in = resource.get().open();
            }
         }
         if (in == null) {
            return null;
         }
         image = NativeImage.read(in);
         if (mergeOverlays(image)) {
            return image;
         }
         image.close();
         return null;
      } finally {
         if (in != null) {
            in.close();
         }
      }
   }

   /**
    * 把第二图层像素 over 合成进第一图层。
    *
    * @return 是否实际处理（贴图宽 >= 64）；不处理时调用方放弃合成
    */
   private static boolean mergeOverlays(NativeImage image) {
      int width = image.getWidth();
      int height = image.getHeight();
      if (width < 64 || height < 32) {
         return false;
      }
      int scale = width / 64;
      if (scale < 1) {
         return false;
      }
      for (int[] region : OVERLAY_REGIONS) {
         int srcX0 = region[0] * scale;
         int srcY0 = region[1] * scale;
         int dstX0 = region[2] * scale;
         int dstY0 = region[3] * scale;
         int regionW = region[4] * scale;
         int regionH = region[5] * scale;
         if (srcX0 + regionW > width || srcY0 + regionH > height
               || dstX0 + regionW > width || dstY0 + regionH > height) {
            continue;
         }
         for (int y = 0; y < regionH; y++) {
            for (int x = 0; x < regionW; x++) {
               blendOver(image, srcX0 + x, srcY0 + y, dstX0 + x, dstY0 + y);
            }
         }
      }
      return true;
   }

   /** src 像素（第二图层）over 合成到 dst 像素（第一图层），原地写回 dst。 */
   private static void blendOver(NativeImage image, int srcX, int srcY, int dstX, int dstY) {
      int src = image.getPixelRGBA(srcX, srcY);
      int sa = src >>> 24 & 0xFF;
      if (sa == 0) {
         return;
      }
      int sr = src & 0xFF;
      int sg = src >>> 8 & 0xFF;
      int sb = src >>> 16 & 0xFF;

      int dst = image.getPixelRGBA(dstX, dstY);
      int da = dst >>> 24 & 0xFF;
      int dr = dst & 0xFF;
      int dg = dst >>> 8 & 0xFF;
      int db = dst >>> 16 & 0xFF;

      int invSa = 255 - sa;
      int daScaled = da * invSa / 255;
      int outA = sa + daScaled;
      int outR = outA == 0 ? 0 : (sr * sa + dr * daScaled) / outA;
      int outG = outA == 0 ? 0 : (sg * sa + dg * daScaled) / outA;
      int outB = outA == 0 ? 0 : (sb * sa + db * daScaled) / outA;
      image.setPixelRGBA(dstX, dstY, (outA << 24) | (outB << 16) | (outG << 8) | outR);
   }

   /** 必须在渲染线程调用：上传合成贴图并注册。失败时关闭 image。 */
   private static ResourceLocation register(ResourceLocation skin, NativeImage image) {
      try {
         String file = skin.getPath().substring(skin.getPath().lastIndexOf('/') + 1);
         ResourceLocation merged = ResourceLocation.fromNamespaceAndPath(
            "dingdongji", "phantom_skin/" + skin.getNamespace() + "/" + file);
         TextureManager textures = Minecraft.getInstance().getTextureManager();
         textures.release(merged);
         textures.register(merged, new DynamicTexture(image));
         return merged;
      } catch (Throwable t) {
         image.close();
         return null;
      }
   }
}
