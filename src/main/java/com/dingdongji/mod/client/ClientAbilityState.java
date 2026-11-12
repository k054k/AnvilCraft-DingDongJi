package com.dingdongji.mod.client;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class ClientAbilityState {
   public static boolean lavaWalker;
   public static boolean frostSlide;
   public static boolean phaseVertical;
   public static boolean transcendiumPhase;
   /** 本地玩家自己的幻影开关（仅同步本人）。 */
   public static boolean phantom;
   /** 其他玩家的幻影状态（服务端广播）：渲染幻灵色半透明本体。 */
   private static final java.util.Set<UUID> REMOTE_PHANTOM = new java.util.HashSet<>();
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

   public static boolean isTranscendiumPhase(Player player) {
      Minecraft mc = Minecraft.getInstance();
      return mc.player == player && transcendiumPhase;
   }

   public static boolean isPhantom(Player player) {
      Minecraft mc = Minecraft.getInstance();
      return mc.player == player ? phantom : REMOTE_PHANTOM.contains(player.getUUID());
   }

   /** 接收服务端广播的其他玩家幻影状态。 */
   public static void applyRemotePhantom(UUID playerId, boolean phantomState) {
      if (phantomState) {
         REMOTE_PHANTOM.add(playerId);
      } else {
         REMOTE_PHANTOM.remove(playerId);
      }
   }

   public static boolean isLocalNightVision(Player player) {
      Minecraft mc = Minecraft.getInstance();
      return mc.player == player && nightVision();
   }

   public static void applySync(boolean lavaWalkerEnabled, boolean frostSlideEnabled, int mode, boolean phaseVerticalEnabled, boolean transcendiumPhaseEnabled, boolean phantomEnabled) {
      lavaWalker = lavaWalkerEnabled;
      frostSlide = frostSlideEnabled;
      helmetMode = mode;
      phaseVertical = phaseVerticalEnabled;
      transcendiumPhase = transcendiumPhaseEnabled;
      phantom = phantomEnabled;
   }
}
