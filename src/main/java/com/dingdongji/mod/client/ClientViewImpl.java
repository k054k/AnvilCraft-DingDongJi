package com.dingdongji.mod.client;

import com.dingdongji.mod.platform.ClientView;
import net.minecraft.world.entity.player.Player;

/** {@link ClientView} 的客户端实现：读取本地同步状态与按键显示名。 */
public final class ClientViewImpl implements ClientView {
   @Override
   public boolean isLavaWalker(Player player) {
      return ClientAbilityState.isLavaWalker(player);
   }

   @Override
   public boolean isFrostSlide(Player player) {
      return ClientAbilityState.isFrostSlide(player);
   }

   @Override
   public boolean isPhaseVertical(Player player) {
      return ClientAbilityState.isPhaseVertical(player);
   }

   @Override
   public boolean isTranscendiumPhase(Player player) {
      return ClientAbilityState.isTranscendiumPhase(player);
   }

   @Override
   public boolean isPhantom(Player player) {
      return ClientAbilityState.isPhantom(player);
   }

   @Override
   public String abilityKey() {
      return ClientKeyNames.abilityKey();
   }

   @Override
   public String glowingVisionKey() {
      return ClientKeyNames.glowingVisionKey();
   }

   @Override
   public String neutronBarrierKey() {
      return ClientKeyNames.neutronBarrierKey();
   }

   @Override
   public String fullSetToggleKey() {
      return ClientKeyNames.fullSetToggleKey();
   }

   @Override
   public String switchToolModeKey() {
      return ClientKeyNames.switchToolModeKey();
   }
}
