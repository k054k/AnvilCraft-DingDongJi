package com.dingdongji.mod;

import com.dingdongji.mod.block.ModBlocks;
import com.dingdongji.mod.client.ClientCreateTemplatePinTick;
import com.dingdongji.mod.client.ClientModInit;
import com.dingdongji.mod.client.SpectralPhaseClientHandler;
import com.dingdongji.mod.event.ModArmorSetHandler;
import com.dingdongji.mod.event.ModEvents;
import com.dingdongji.mod.event.ModRecipeHandler;
import com.dingdongji.mod.event.ModifyDefaultComponentsHandler;
import com.dingdongji.mod.init.ModParticles;
import com.dingdongji.mod.init.ModRecipes;
import com.dingdongji.mod.item.ModArmorMaterials;
import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.network.ModNetwork;
import com.dingdongji.mod.tab.ModCreativeTab;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod("dingdongji")
public class KryptonMod {
   public static final String MODID = "dingdongji";

   public static ResourceLocation modLoc(String path) {
      return ResourceLocation.fromNamespaceAndPath("dingdongji", path);
   }

   public KryptonMod(IEventBus modEventBus, ModContainer container) {
      container.registerConfig(ModConfig.Type.CLIENT, ModClientConfig.SPEC);
      ModBlocks.BLOCKS.register(modEventBus);
      ModParticles.PARTICLES.register(modEventBus);
      ModItems.ITEMS.register(modEventBus);
      ModComponents.COMPONENTS.register(modEventBus);
      ModRecipes.RECIPE_TYPES.register(modEventBus);
      ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
      ModArmorMaterials.ARMOR_MATERIALS.register(modEventBus);
      ModMenuTypes.MENUS.register(modEventBus);
      ModCreativeTab.CREATIVE_MODE_TABS.register(modEventBus);
      modEventBus.addListener(ModifyDefaultComponentsHandler::onModifyDefaultComponents);
      if (FMLEnvironment.dist == Dist.CLIENT) {
         ClientModInit.register(modEventBus);
         container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
      }

      modEventBus.addListener(ModNetwork::register);
      IEventBus gameBus = NeoForge.EVENT_BUS;
      if (FMLEnvironment.dist == Dist.CLIENT) {
         gameBus.addListener(SpectralPhaseClientHandler::onClientTick);
         gameBus.addListener(ClientCreateTemplatePinTick::onClientTick);
      }
      gameBus.addListener(ModArmorSetHandler::onPlayerTick);
      gameBus.addListener(ModArmorSetHandler::onLivingDamage);
      gameBus.addListener(EventPriority.LOW, false, ModArmorSetHandler::onLivingBreathe);
      gameBus.addListener(ModArmorSetHandler::onPlayerLoggedOut);
      gameBus.addListener(ModArmorSetHandler::onPlayerLogin);
      gameBus.addListener(ModArmorSetHandler::onEquipmentChange);
      gameBus.addListener(ModArmorSetHandler::onBreakSpeed);
      gameBus.addListener(ModEvents::onTooltip);
      gameBus.addListener(ModEvents::onLivingDeath);
      gameBus.addListener(ModEvents::onItemAttributeModifier);
      gameBus.addListener(com.dingdongji.mod.event.ModEndGatewayHandler::onEntityJoinLevel);
      gameBus.addListener(ModRecipeHandler::onServerStarted);
   }
}
