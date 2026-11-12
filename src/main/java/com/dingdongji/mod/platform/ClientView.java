package com.dingdongji.mod.platform;

import net.minecraft.world.entity.player.Player;

/**
 * 通用侧访问客户端状态的反转接口：实现类位于客户端包，仅在物理客户端安装。
 * 专用服务端上不安装实现，所有方法走 {@link ClientViews} 的安全默认值。
 * 通用代码不得直接 import 任何 {@code net.minecraft.client.*} 或
 * {@code com.dingdongji.mod.client.*} 类型。
 */
public interface ClientView {
   boolean isLavaWalker(Player player);

   boolean isFrostSlide(Player player);

   boolean isPhaseVertical(Player player);

   boolean isTranscendiumPhase(Player player);

   boolean isPhantom(Player player);

   String abilityKey();

   String glowingVisionKey();

   String neutronBarrierKey();

   String fullSetToggleKey();

   String switchToolModeKey();
}
