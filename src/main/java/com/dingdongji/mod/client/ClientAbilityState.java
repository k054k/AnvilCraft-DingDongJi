package com.dingdongji.mod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class ClientAbilityState {
   public static boolean lavaWalker;
   public static boolean frostSlide;
   public static boolean phaseVertical;
   public static int helmetMode = 5;

   private ClientAbilityState() {
   }

   public static boolean nightVision() {
      return helmetMode == 2 || helmetMode == 4;
   }

   public static boolean isLavaWalker(Player player) {
      Minecraft mc = Minecraft.getInstance();
      return mc.player == player && lavaWalker;
   }

   public static boolean isFrostSlide(Player player) {
      Minecraft mc = Minecraft.getInstance();
      return mc.player == player && frostSlide;
   }

   public static boolean isPhaseVertical(Player player) {
      Minecraft mc = Minecraft.getInstance();
      return mc.player == player && phaseVertical;
   }

   /**
    * Controlled climb/descent speed for the vertical phase (client author side):
    * space ascends, shift descends, neither hovers. Gravity is consumed by the
    * collide mixin, so this value becomes the raw Y delta.
    */
   public static double phaseVerticalVelocity() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return 0.0;
      }

      if (mc.options.keyJump.isDown()) {
         return 0.2;
      }

      return mc.options.keyShift.isDown() ? -0.15 : 0.0;
   }

   public static boolean isLocalNightVision(Player player) {
      Minecraft mc = Minecraft.getInstance();
      return mc.player == player && nightVision();
   }

   public static void applySync(boolean lavaWalkerEnabled, boolean frostSlideEnabled, int mode, boolean phaseVerticalEnabled) {
      lavaWalker = lavaWalkerEnabled;
      frostSlide = frostSlideEnabled;
      helmetMode = mode;
      phaseVertical = phaseVerticalEnabled;
   }
}
