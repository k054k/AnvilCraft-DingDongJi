package com.dingdongji.mod.event;

import com.dingdongji.mod.util.AnvilCraftCompat;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

public class ModRecipeHandler {
   private static final Logger LOGGER = LogUtils.getLogger();

   @SubscribeEvent
   public static void onServerStarted(ServerStartedEvent event) {
      MinecraftServer server = event.getServer();
      AnvilCraftCompat.init(server.registryAccess());
      LOGGER.info("(இωஇ )");
   }
}
