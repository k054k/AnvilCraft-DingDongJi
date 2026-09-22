package com.dingdongji.mod.client;

import com.dingdongji.mod.event.ModArmorSetHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 幻灵套客户端逻辑。
 *
 * mode 1（全套被动）：无客户端处理。noPhysics 保持 false，水平穿墙由
 * MixinSpectralHorizontalPhase 在碰撞计算中处理，垂直碰撞、onGround、
 * 跳跃、坠落、行走视角晃动全部原版。
 *
 * mode 2（虚化）：身体处于方块内部时，无重力悬浮，垂直速度由按键控制
 * （空格爬升 / Shift 下穿），调用与服务端完全相同的 applyVerticalPhase；
 * 身体不在方块内（地表）时与 mode 1 一致，原版跑跳。
 */
public final class SpectralPhaseClientHandler {
   private SpectralPhaseClientHandler() {
   }

   public static void onClientTick(ClientTickEvent.Post event) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player == null || player.isPassenger()) {
         return;
      }

      int mode = ModArmorSetHandler.spectralPhaseMode(player);
      if (mode == 0) {
         return;
      }

      if (mode == 2 && ModArmorSetHandler.isVerticalPhaseActive(player)) {
         // 身体在方块内，或地表按 shift 开始下潜
         ModArmorSetHandler.applyVerticalPhase(
            player,
            mc.options.keyJump.isDown(),
            mc.options.keyShift.isDown()
         );
      }
   }
}
