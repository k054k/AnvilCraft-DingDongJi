package com.dingdongji.mod;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ModClientConfig {
   private static final ModConfigSpec.BooleanValue GLOW_BAND;
   public static final ModConfigSpec SPEC;

   static {
      ModConfigSpec.Builder b = new ModConfigSpec.Builder();
      GLOW_BAND = b.comment(
            "Enable the glowing band overlay on the metal armor sets. Uses a custom emissive render "
               + "type; if the band renders black or invisible on your GPU, turn this off."
         )
         .define("glowBandEnabled", true);
      SPEC = b.build();
   }

   private ModClientConfig() {
   }

   public static boolean glowBandEnabled() {
      return GLOW_BAND.get();
   }
}
