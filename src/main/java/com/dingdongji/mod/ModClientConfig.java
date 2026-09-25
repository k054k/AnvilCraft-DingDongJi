package com.dingdongji.mod;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ModClientConfig {
   private static final ModConfigSpec.BooleanValue GLOW_BAND;
   private static final ModConfigSpec.BooleanValue CREATE_TEMPLATE_PIN;
   private static final ModConfigSpec.BooleanValue FLIGHT_AFTERIMAGE;
   public static final ModConfigSpec SPEC;

   static {
      ModConfigSpec.Builder b = new ModConfigSpec.Builder();
      GLOW_BAND = b.comment(
            "Enable the glowing band overlay on the metal armor sets. Uses a custom emissive render "
               + "type; if the band renders black or invisible on your GPU, turn this off."
         )
         .define("glowBandEnabled", true);
      CREATE_TEMPLATE_PIN = b.comment(
            "Pin the create template (and its mode variants) to the first slot of the template "
               + "catalog in the Royal/Ember/Frost/Transcendence smithing tables. Turn this off to "
               + "let it stay in its natural catalog position."
         )
         .define("createTemplatePinEnabled", true);
      FLIGHT_AFTERIMAGE = b.comment(
            "Show the purple afterimage trail behind players flying over-speed in Transcendium Boots."
         )
         .define("flightAfterimageEnabled", true);
      SPEC = b.build();
   }

   private ModClientConfig() {
   }

   public static boolean glowBandEnabled() {
      return GLOW_BAND.get();
   }

   public static boolean createTemplatePinEnabled() {
      return CREATE_TEMPLATE_PIN.get();
   }

   public static boolean flightAfterimageEnabled() {
      return FLIGHT_AFTERIMAGE.get();
   }
}
