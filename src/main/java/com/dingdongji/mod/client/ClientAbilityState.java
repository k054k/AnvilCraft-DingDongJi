package com.dingdongji.mod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class ClientAbilityState {
   public static boolean lavaWalker;
   public static boolean frostSlide;
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

   public static boolean isLocalNightVision(Player player) {
      Minecraft mc = Minecraft.getInstance();
      return mc.player == player && nightVision();
   }

   public static void applySync(boolean lavaWalkerEnabled, boolean frostSlideEnabled, int mode) {
      lavaWalker = lavaWalkerEnabled;
      frostSlide = frostSlideEnabled;
      helmetMode = mode;
   }
}
