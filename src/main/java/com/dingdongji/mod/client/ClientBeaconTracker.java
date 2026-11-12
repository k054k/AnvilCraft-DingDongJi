package com.dingdongji.mod.client;

import net.minecraft.core.BlockPos;

/**
 * 客户端最后交互的信标坐标（供 BeaconConfirmButtonMixin 判定 4 层免锭底座）。
 * 放在 client 包：mixin 包内不允许有普通类被直接引用（IllegalClassLoadError）。
 */
public final class ClientBeaconTracker {

   private static volatile BlockPos lastInteractedBeacon = null;

   private ClientBeaconTracker() {
   }

   public static void record(BlockPos pos) {
      lastInteractedBeacon = pos == null ? null : pos.immutable();
   }

   public static BlockPos getLastInteractedBeacon() {
      return lastInteractedBeacon;
   }
}
