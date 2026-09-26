package com.dingdongji.mod.mixin;

import com.dingdongji.mod.client.AfterimageManager;
import com.dingdongji.mod.client.SpectralArmorRenderTypes;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 幻灵套盔甲渲染重定向，实现“半透明 + 自身重叠面剔除”。
 *
 * 完全自控时序（不再依赖共享 buffer 的链式 flush）：
 * - HEAD 注入且为幻灵贴图时：
 *   1) buffers.endBatch() 强制先把主 buffer 中已累积的皮肤等批次画出，
 *      保证后续深度预 pass 不会比皮肤先写深度（否则皮肤被 LEQUAL 剔除，
 *      出现“两件同穿时腿部皮肤/盔甲消失”）；
 *   2) 用独立 buffer 依次执行 深度 pass（只写深度）→ endBatch →
 *      半透明 pass（LEQUAL，剔除自身背面）→ endBatch；
 *   3) ci.cancel() 跳过原版 renderModel。
 * 每件盔甲独立完成完整流程，件数、渲染顺序、BufferSource 内部实现
 * 差异均不影响结果。
 *
 * 其他盔甲保持原版 armorCutoutNoCull 路径不变。
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class MixinHumanoidArmorLayerRenderType {
   /** 幻灵双 pass 专用独立 buffer，避免与主 BufferSource 的共享/排序行为耦合。 */
   private static final MultiBufferSource.BufferSource DDJ_SPECTRAL_BUFFERS =
      MultiBufferSource.immediate(new ByteBufferBuilder(1 << 21));

   private static boolean isSpectral(ResourceLocation texture) {
      return "dingdongji".equals(texture.getNamespace()) && texture.getPath().contains("spectral");
   }

   @Inject(
      method = "renderModel(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/Model;ILnet/minecraft/resources/ResourceLocation;)V",
      at = @At("HEAD"),
      cancellable = true,
      require = 1
   )
   private void ddj$spectralRenderControlled(
      PoseStack pose,
      MultiBufferSource buffers,
      int packedLight,
      Model model,
      int tint,
      ResourceLocation texture,
      CallbackInfo ci
   ) {
      if (!isSpectral(texture) || AfterimageManager.isRenderingAfterimage()) {
         return;
      }
      // 1. 先把皮肤等已累积批次画出，确保皮肤深度先于盔甲深度写入
      if (buffers instanceof MultiBufferSource.BufferSource bs) {
         bs.endBatch();
      }
      // 2. 独立 buffer 内按序执行两个 pass
      MultiBufferSource.BufferSource own = DDJ_SPECTRAL_BUFFERS;
      model.renderToBuffer(pose, own.getBuffer(SpectralArmorRenderTypes.spectralArmorDepth(texture)), packedLight, OverlayTexture.NO_OVERLAY, tint);
      own.endBatch();
      model.renderToBuffer(pose, own.getBuffer(SpectralArmorRenderTypes.spectralArmor(texture)), packedLight, OverlayTexture.NO_OVERLAY, tint);
      own.endBatch();
      // 3. 原版路径整体跳过
      ci.cancel();
   }

   @Redirect(
      method = "renderModel(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/Model;ILnet/minecraft/resources/ResourceLocation;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"
      )
   )
   private RenderType ddj$spectralTranslucentArmor(ResourceLocation texture) {
      // 幻灵走 HEAD 注入的 cancel 分支，不会执行到这里；虚影渲染时回退原版。
      return RenderType.armorCutoutNoCull(texture);
   }
}
