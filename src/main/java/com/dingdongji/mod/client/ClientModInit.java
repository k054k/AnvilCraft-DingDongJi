package com.dingdongji.mod.client;

import com.dingdongji.mod.ModMenuTypes;
import com.dingdongji.mod.client.screen.JiAnvilScreen;
import com.dingdongji.mod.input.ModKeyBindings;
import com.dingdongji.mod.inventory.JiAnvilMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class ClientModInit {
   private ClientModInit() {
   }

   public static void register(IEventBus modEventBus) {
      modEventBus.addListener(ModKeyBindings::registerKeyMappings);
      modEventBus.addListener(ClientSetupHandler::onClientSetup);
      modEventBus.addListener(ClientSetupHandler::registerParticleProviders);
      modEventBus.addListener(ClientModInit::registerScreens);
   }

   private static void registerScreens(RegisterMenuScreensEvent event) {
      MenuType<JiAnvilMenu> type = (MenuType<JiAnvilMenu>)ModMenuTypes.JI_ANVIL.get();
      event.register(type, JiAnvilScreen::new);
   }
}
