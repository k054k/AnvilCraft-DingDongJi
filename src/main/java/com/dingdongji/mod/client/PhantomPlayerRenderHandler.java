package com.dingdongji.mod.client;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * 幻灵套"幻影"状态的本地玩家本体渲染：不隐身，全身以幻灵色半透明绘制。
 *
 * <p>单 pass 方案（旧的深度双 pass 会让第一层/第二层皮肤交界处随帧闪烁，
 * 已弃用）：
 * <ol>
 * <li>{@link PhantomSkinMerger} 事先把皮肤第二图层（帽/外套/双袖/双裤）的
 *     非透明像素 alpha 合成到第一图层对应 UV，生成单层皮肤贴图；</li>
 * <li>渲染前临时把 {@link PlayerModel} 的六个第二图层部件
 *     {@code visible=false}，几何只画一次，皮肤直接采样合并贴图；</li>
 * <li>全部几何经 {@link AfterimageManager#phantomBodyTint} 改走原版
 *     半透明渲染类型（entityTranslucent/translucent：写深度 + 面剔除）
 *     并染幻灵色——单 pass 不透背面、无双层重叠闪烁。盔甲、披风、
 *     手持物品等同路染色。</li>
 * </ol>
 * 合并贴图尚未就绪（皮肤文件刚下载/反射失败）时回退为原贴图、保留第二层
 * 几何，表现为普通半透明，绝不下掉第二层外观。渲染结束后六个部件的
 * visible 原样恢复，不影响其他玩家与后续帧。
 *
 * <p>{@code setPhantomBody(true)} 让幻灵盔甲层直接取消——幻影玩家身上
 * 不再渲染幻灵盔甲（人体本身即为幻影）。
 *
 * <p>仅对本地玩家生效（幻影状态只同步本人）；第一人称本体不渲染，手臂由
 * PhantomArmMixin 以同一方案染色。renderingPhantom 防止手动重绘触发的事件递归。
 */
@EventBusSubscriber({Dist.CLIENT})
public final class PhantomPlayerRenderHandler {
   private static boolean renderingPhantom;
   /** 幻影本体专用独立 buffer，不与主 BufferSource 的排序/共享行为耦合。 */
   private static final BufferSource DDJ_PHANTOM_BODY =
      MultiBufferSource.immediate(new ByteBufferBuilder(1 << 22));

   private PhantomPlayerRenderHandler() {
   }

   @SubscribeEvent
   public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
      if (renderingPhantom || AfterimageManager.isRenderingAfterimage()) {
         return;
      }
      if (!(event.getEntity() instanceof AbstractClientPlayer player) || !ClientAbilityState.isPhantom(player)) {
         return;
      }

      event.setCanceled(true);
      Minecraft mc = Minecraft.getInstance();
      float partial = event.getPartialTick();
      float yaw = Mth.rotLerp(partial, player.yRotO, player.getYRot());
      MultiBufferSource original = event.getMultiBufferSource();
      EntityRenderer<? super AbstractClientPlayer> renderer =
         mc.getEntityRenderDispatcher().getRenderer(player);
      int packedLight = 15728880;

      ResourceLocation skinTexture = player.getSkin().texture();
      ResourceLocation mergedSkin = PhantomSkinMerger.getMergedSkin(skinTexture);
      ModelPart[] hiddenParts = null;
      boolean[] savedVisibility = null;
      if (mergedSkin != null && renderer instanceof LivingEntityRenderer<?, ?> livingRenderer
            && livingRenderer.getModel() instanceof PlayerModel<?> playerModel) {
         hiddenParts = new ModelPart[] {
            playerModel.hat, playerModel.jacket,
            playerModel.leftSleeve, playerModel.rightSleeve,
            playerModel.leftPants, playerModel.rightPants
         };
         savedVisibility = new boolean[hiddenParts.length];
         for (int i = 0; i < hiddenParts.length; i++) {
            savedVisibility[i] = hiddenParts[i].visible;
            hiddenParts[i].visible = false;
         }
      }

      renderingPhantom = true;
      AfterimageManager.setRenderingAfterimage(true);
      AfterimageManager.setPhantomBody(true);
      try {
         // 主 buffer 中已累积的批次先画出，保证幻影的半透明时序独立
         if (original instanceof BufferSource bs) {
            bs.endBatch();
         }
         // 单 pass：全部几何一次绘制，entityTranslucent 写深度+面剔除，
         // 皮肤采样合并贴图（第二层几何已隐藏，无双层竞争/闪烁）
         renderer.render(
            player, yaw, partial, event.getPoseStack(),
            AfterimageManager.phantomBodyTint(DDJ_PHANTOM_BODY, skinTexture, mergedSkin), packedLight
         );
         DDJ_PHANTOM_BODY.endBatch();
      } finally {
         AfterimageManager.setPhantomBody(false);
         AfterimageManager.setRenderingAfterimage(false);
         renderingPhantom = false;
         if (hiddenParts != null) {
            for (int i = 0; i < hiddenParts.length; i++) {
               hiddenParts[i].visible = savedVisibility[i];
            }
         }
      }
   }
}
