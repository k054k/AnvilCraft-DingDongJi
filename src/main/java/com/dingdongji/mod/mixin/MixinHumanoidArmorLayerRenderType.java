package com.dingdongji.mod.mixin;

import com.dingdongji.mod.client.AfterimageManager;
import com.dingdongji.mod.client.SpectralArmorRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
 * 时序（关键：两 pass 均走共享 buffer，借助 getBuffer 的链式 flush）：
 * - HEAD 注入：先获取 depth 类型 buffer 绘制同一模型；
 * - 原代码随后 getBuffer(translucent)：getBuffer 发现共享 buffer 上一
 *   类型是 depth，立即 flush depth（深度写入），再返回 translucent
 *   builder；
 * - 原代码 renderToBuffer 绘制半透明：背面比 depth 预写入的最近表面
 *   更远，LEQUAL 失败被剔除，只显示最前面。
 *
 * 注意：深度注入必须在 HEAD。若放在 getBuffer 之后，getBuffer(depth)
 * 会先结束/flush 掉原代码已获取但还没画的 translucent builder，随后
 * 原代码向已结束的 builder 写顶点，抛 IllegalStateException
 * ("Not building!")。
 *
 * 其他盔甲保持原版 armorCutoutNoCull 路径不变。
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class MixinHumanoidArmorLayerRenderType {
   private static boolean isSpectral(ResourceLocation texture) {
      return "dingdongji".equals(texture.getNamespace()) && texture.getPath().contains("spectral");
   }

   @Inject(
      method = "renderModel(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/Model;ILnet/minecraft/resources/ResourceLocation;)V",
      at = @At("HEAD"),
      require = 1
   )
   private void ddj$spectralDepthPrepass(
      PoseStack pose,
      MultiBufferSource buffers,
      int packedLight,
      Model model,
      int tint,
      ResourceLocation texture,
      CallbackInfo ci
   ) {
      if (isSpectral(texture) && !AfterimageManager.isRenderingAfterimage()) {
         VertexConsumer depthConsumer = buffers.getBuffer(SpectralArmorRenderTypes.spectralArmorDepth(texture));
         model.renderToBuffer(pose, depthConsumer, packedLight, OverlayTexture.NO_OVERLAY, tint);
      }
   }

   @Redirect(
      method = "renderModel(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/Model;ILnet/minecraft/resources/ResourceLocation;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"
      )
   )
   private RenderType ddj$spectralTranslucentArmor(ResourceLocation texture) {
      return isSpectral(texture) && !AfterimageManager.isRenderingAfterimage()
         ? SpectralArmorRenderTypes.spectralArmor(texture)
         : RenderType.armorCutoutNoCull(texture);
   }
}
