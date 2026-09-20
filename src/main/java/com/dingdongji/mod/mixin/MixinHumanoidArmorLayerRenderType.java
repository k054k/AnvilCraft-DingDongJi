package com.dingdongji.mod.mixin;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The spectral set ships a 50%-alpha texture (mirroring the AnvilCraft
 * spectral anvil palette). Vanilla armor uses the CUTOUT render type which
 * collapses partial alpha to fully opaque, so redirect the armor render type
 * to the vanilla entity-translucent shader (slime / player outer layer) for
 * spectral textures only. Every other armor keeps the vanilla path untouched.
 */
@Mixin({HumanoidArmorLayer.class})
public abstract class MixinHumanoidArmorLayerRenderType {
   @Redirect(
      method = {"renderModel(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/HumanoidModel;ILnet/minecraft/resources/ResourceLocation;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"
      )
   )
   private RenderType ddj$spectralTranslucentArmor(ResourceLocation texture) {
      return "dingdongji".equals(texture.getNamespace()) && texture.getPath().contains("spectral")
         ? RenderType.entityTranslucent(texture)
         : RenderType.armorCutoutNoCull(texture);
   }
}
