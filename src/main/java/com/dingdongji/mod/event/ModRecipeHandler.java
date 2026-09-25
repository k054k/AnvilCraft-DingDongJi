package com.dingdongji.mod.event;

import com.dingdongji.mod.util.AnvilCraftCompat;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

public class ModRecipeHandler {

   @SubscribeEvent
   public static void onServerStarted(ServerStartedEvent event) {
      MinecraftServer server = event.getServer();
      AnvilCraftCompat.init(server.registryAccess());
   }
}
