package com.dingdongji.mod.client.layer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.resources.ResourceLocation;

public final class GlowArmorRenderType {
   public static final Function<ResourceLocation, RenderType> GLOW_ARMOR = Util.memoize(
      texture -> {
         CompositeState state = CompositeState.builder()
            .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
            .setTextureState(new TextureStateShard(texture, false, false))
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setCullState(RenderStateShard.NO_CULL)
            .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .createCompositeState(false);
         return RenderType.create("ddj_glow_armor", DefaultVertexFormat.NEW_ENTITY, Mode.QUADS, 1536, false, true, state);
      }
   );

   private GlowArmorRenderType() {
   }

   public static RenderType glowArmor(ResourceLocation texture) {
      return GLOW_ARMOR.apply(texture);
   }
}
