package com.dingdongji.mod.client.layer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class GlowArmorRenderType {
   // Custom composite based on the vanilla emissive entity shader, but with
   // VIEW_OFFSET_Z_LAYERING: the glow is drawn on geometry identical to the
   // base armor, so without the polygon offset it z-fights with the armor
   // layer (flickering / only one side visible in world view). The stock
   // entityTranslucentEmissive type lacks this layering state.
   private static final Function<ResourceLocation, RenderType> GLOW_ARMOR = Util.memoize(texture -> {
      RenderType.CompositeState state = RenderType.CompositeState.builder()
         .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
         .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
         .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
         .setCullState(RenderStateShard.NO_CULL)
         .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
         .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
         .setWriteMaskState(RenderStateShard.COLOR_WRITE)
         .createCompositeState(false);
      return RenderType.create(
         "ddj_glow_armor", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true, state
      );
   });

   private GlowArmorRenderType() {
   }

   public static RenderType glowArmor(ResourceLocation texture) {
      return GLOW_ARMOR.apply(texture);
   }
}
