package com.dingdongji.mod.client;

import com.dingdongji.mod.util.CreateTemplatePinOrder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Polls the client-only create-template pin option every client tick and
 * reorders the open AdjacentSmithingMenu catalog the moment it flips, so the
 * change is visible on the next frame without waiting for a server sync and
 * regardless of the integrated-server pause while the config screen is open.
 */
public final class ClientCreateTemplatePinTick {
   private ClientCreateTemplatePinTick() {
   }

   public static void onClientTick(ClientTickEvent.Post event) {
      Minecraft minecraft = Minecraft.getInstance();
      Object menu = minecraft.screen instanceof AbstractContainerScreen<?> screen ? screen.getMenu() : null;
      CreateTemplatePinOrder.tick(menu);
   }
}
