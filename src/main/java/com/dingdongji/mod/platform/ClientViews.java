package com.dingdongji.mod.platform;

import net.minecraft.world.entity.player.Player;

/**
 * 客户端视图持有者。专用服务端永不安装实现，访问安全默认值
 * （布尔假、按键名空串）；物理客户端启动时由客户端初始化代码注入实现。
 */
public final class ClientViews {
   private static final ClientView FALLBACK = new ClientView() {
      @Override
      public boolean isLavaWalker(Player player) {
         return false;
      }

      @Override
      public boolean isFrostSlide(Player player) {
         return false;
      }

      @Override
      public boolean isPhaseVertical(Player player) {
         return false;
      }

      @Override
      public boolean isTranscendiumPhase(Player player) {
         return false;
      }

      @Override
      public boolean isPhantom(Player player) {
         return false;
      }

      @Override
      public String abilityKey() {
         return "";
      }

      @Override
      public String glowingVisionKey() {
         return "";
      }

      @Override
      public String neutronBarrierKey() {
         return "";
      }

      @Override
      public String fullSetToggleKey() {
         return "";
      }

      @Override
      public String switchToolModeKey() {
         return "";
      }
   };

   private static volatile ClientView view = FALLBACK;

   private ClientViews() {
   }

   public static void install(ClientView clientView) {
      view = clientView;
   }

   public static ClientView get() {
      return view;
   }
}
