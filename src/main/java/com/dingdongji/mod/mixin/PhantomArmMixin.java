package com.dingdongji.mod.mixin;

import com.dingdongji.mod.client.AfterimageManager;
import com.dingdongji.mod.client.ClientAbilityState;
import com.dingdongji.mod.client.PhantomSkinMerger;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 幻影状态第一人称手臂：取消原版单次渲染，改为与第三人称本体完全一致的
 * 单 pass 方案（独立 BufferSource）——手臂几何一次绘制，走原版半透明
 * 渲染类型（写深度 + 面剔除）并染幻灵色，无双层重叠闪烁。
 */
@Mixin(PlayerRenderer.class)
public abstract class PhantomArmMixin {
   private static boolean ddj$reentrant;
   /** 手臂单 pass 专用独立 buffer，容量只需容纳手臂+袖子几何。 */
   private static final MultiBufferSource.BufferSource DDJ_PHANTOM_ARM =
      MultiBufferSource.immediate(new ByteBufferBuilder(1 << 20));

   @Shadow
   private void renderHand(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                           AbstractClientPlayer player, ModelPart armModel, ModelPart sleeveModel) {
      throw new AssertionError();
   }

   @Inject(
      method = "renderHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/model/geom/ModelPart;)V",
      at = @At("HEAD"),
      cancellable = true,
      require = 1
   )
   private void ddj$phantomArmPasses(PoseStack poseStack, MultiBufferSource source, int packedLight,
                                     AbstractClientPlayer player, ModelPart armModel, ModelPart sleeveModel,
                                     CallbackInfo ci) {
      if (ddj$reentrant || !ClientAbilityState.isPhantom(Minecraft.getInstance().player)) {
         return;
      }
      ci.cancel();
      ddj$reentrant = true;
      // 与第三人称本体同一方案：袖子几何隐藏 + 合并贴图（袖子外观已烘焙进手臂 UV），
      // 避免半透明下手臂背面/袖子重影。合并贴图未就绪时保留袖子几何回退渲染。
      ResourceLocation skinTexture = player.getSkin().texture();
      ResourceLocation mergedSkin = PhantomSkinMerger.getMergedSkin(skinTexture);
      boolean savedSleeveVisible = sleeveModel.visible;
      if (mergedSkin != null) {
         sleeveModel.visible = false;
      }
      try {
         // 单 pass：手臂一次绘制，皮肤采样合并贴图（袖子几何已隐藏）
         renderHand(
            poseStack,
            AfterimageManager.phantomBodyTint(DDJ_PHANTOM_ARM, skinTexture, mergedSkin),
            packedLight, player, armModel, sleeveModel
         );
         DDJ_PHANTOM_ARM.endBatch();
      } finally {
         sleeveModel.visible = savedSleeveVisible;
         ddj$reentrant = false;
      }
   }
}
