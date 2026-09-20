package com.dingdongji.mod.client;

import com.dingdongji.mod.event.ModArmorSetHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public final class SpectralPhaseClientHandler {
   private SpectralPhaseClientHandler() {
   }

   // Client mirror of the Vex recipe: the local player owns its own physics
   // prediction, so noPhysics/noGravity must be applied here as well, plus the
   // scaffolding-style Y control with the real key states (the server only
   // sees shift; space ascend relies on client-authoritative positions, which
   // noPhysics makes the server accept - same packet path as spectators).
   public static void onClientTick(ClientTickEvent.Post event) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player == null || player.isPassenger()) {
         return;
      }

      int mode = ModArmorSetHandler.spectralPhaseMode(player);
      boolean phasing = mode >= 1;
      player.noPhysics = phasing || player.isSpectator();
      player.setNoGravity(phasing);
      if (phasing) {
         player.fallDistance = 0.0F;
         double dy = 0.0;
         if (mode == 2) {
            if (mc.options.keyJump.isDown()) {
               dy = 0.2;
            } else if (mc.options.keyShift.isDown()) {
               dy = -0.15;
            }
         }

         player.setDeltaMovement(player.getDeltaMovement().x, dy, player.getDeltaMovement().z);
      } else if (player.isNoGravity()) {
         player.setNoGravity(false);
      }
   }
}
