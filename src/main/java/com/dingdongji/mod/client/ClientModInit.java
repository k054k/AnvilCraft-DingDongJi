package com.dingdongji.mod.client;

import com.dingdongji.mod.ModMenuTypes;
import com.dingdongji.mod.client.screen.JiAnvilScreen;
import com.dingdongji.mod.input.ModKeyBindings;
import com.dingdongji.mod.inventory.JiAnvilMenu;
import com.dingdongji.mod.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ArmorItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class ClientModInit {
   private ClientModInit() {
   }

   public static void register(IEventBus modEventBus) {
      modEventBus.addListener(ModKeyBindings::registerKeyMappings);
      modEventBus.addListener(ClientSetupHandler::onClientSetup);
      modEventBus.addListener(ClientSetupHandler::registerParticleProviders);
      modEventBus.addListener(ClientModInit::registerScreens);
      modEventBus.addListener(ClientModInit::registerItemDecorations);
      modEventBus.addListener(ClientModInit::registerItemExtensions);
   }

   private static void registerScreens(RegisterMenuScreensEvent event) {
      MenuType<JiAnvilMenu> type = (MenuType<JiAnvilMenu>)ModMenuTypes.JI_ANVIL.get();
      event.register(type, JiAnvilScreen::new);
   }

   /** 中子航空套复用铁砧耐候套的自定义穿戴模型几何（背包/飘升机/头盔）。 */
   private static void registerItemExtensions(RegisterClientExtensionsEvent event) {
      event.registerItem(
         NeutronArmorModelExtension.INSTANCE,
         ModItems.NEUTRON_SPACESUIT_HELMET.get(),
         ModItems.NEUTRON_SPACESUIT_CHESTPLATE.get(),
         ModItems.NEUTRON_SPACESUIT_LEGGINGS.get(),
         ModItems.NEUTRON_SPACESUIT_BOOTS.get()
      );
   }

   /** 给所有腿护甲（含铁砧两件套）注册口袋角标。 */
   private static void registerItemDecorations(RegisterItemDecorationsEvent event) {
      for (var item : BuiltInRegistries.ITEM) {
         if (item instanceof ArmorItem armor && armor.getType().getSlot() == EquipmentSlot.LEGS) {
            event.register(item, PouchDecoration.INSTANCE);
         }
      }
   }
}
