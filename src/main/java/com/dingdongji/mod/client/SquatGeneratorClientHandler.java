package com.dingdongji.mod.client;

import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.network.SquatCrouchPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 蹲起式发电器客户端蹲下状态上报（仅穿着时发送，状态变化才发包）。
 */
public final class SquatGeneratorClientHandler {
   private static Boolean prevCrouching;

   private SquatGeneratorClientHandler() {
   }

   public static void tick(Minecraft mc) {
      LocalPlayer player = mc.player;
      if (player == null) {
         prevCrouching = null;
         return;
      }
      if (!player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.SQUAT_GENERATOR_LEGGINGS.get())) {
         prevCrouching = null;
         return;
      }
      boolean crouching = player.isCrouching();
      if (prevCrouching == null || prevCrouching != crouching) {
         prevCrouching = crouching;
         PacketDistributor.sendToServer(new SquatCrouchPacket(crouching));
      }
   }
}
