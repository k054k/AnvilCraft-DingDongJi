package com.dingdongji.mod;

import com.dingdongji.mod.block.ModBlocks;
import com.dingdongji.mod.event.ModArmorSetHandler;
import com.dingdongji.mod.event.ModEvents;
import com.dingdongji.mod.event.ModRecipeHandler;
import com.dingdongji.mod.event.ModifyDefaultComponentsHandler;
import com.dingdongji.mod.event.PhantomDecoyManager;
import com.dingdongji.mod.init.ModEntities;
import com.dingdongji.mod.init.ModParticles;
import com.dingdongji.mod.init.ModRecipes;
import com.dingdongji.mod.item.ModArmorMaterials;
import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.network.ModNetwork;
import com.dingdongji.mod.tab.ModCreativeTab;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
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
      ModEntities.ENTITY_TYPES.register(modEventBus);
      ModItems.ITEMS.register(modEventBus);
      ModComponents.COMPONENTS.register(modEventBus);
      ModRecipes.RECIPE_TYPES.register(modEventBus);
      ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
      ModArmorMaterials.ARMOR_MATERIALS.register(modEventBus);
      ModMenuTypes.MENUS.register(modEventBus);
      ModCreativeTab.CREATIVE_MODE_TABS.register(modEventBus);
      modEventBus.addListener(ModifyDefaultComponentsHandler::onModifyDefaultComponents);
      modEventBus.addListener(this::commonSetup);
      // 客户端注册全部由 client 包内 @EventBusSubscriber(Dist.CLIENT) 类自行订阅，
      // 本通用入口不引用任何客户端类型
      modEventBus.addListener(ModNetwork::register);
      IEventBus gameBus = NeoForge.EVENT_BUS;
      gameBus.addListener(ModArmorSetHandler::onPlayerTick);
      gameBus.addListener(ModArmorSetHandler::onLivingDamage);
      gameBus.addListener(EventPriority.LOW, false, ModArmorSetHandler::onLivingBreathe);
      gameBus.addListener(ModArmorSetHandler::onPlayerLoggedOut);
      gameBus.addListener(ModArmorSetHandler::onPlayerLogin);
      gameBus.addListener(ModArmorSetHandler::onPlayerClone);
      gameBus.addListener(ModArmorSetHandler::onEquipmentChange);
      gameBus.addListener(ModArmorSetHandler::onBreakSpeed);
      gameBus.addListener(PhantomDecoyManager::onEntityTravelToDimension);
      gameBus.addListener(ModEvents::onTooltip);
      gameBus.addListener(ModEvents::onLivingDeath);
      gameBus.addListener(ModEvents::onItemAttributeModifier);
      gameBus.addListener(com.dingdongji.mod.event.ModEndGatewayHandler::onEntityTravelToDimension);
      gameBus.addListener(com.dingdongji.mod.event.ModEndGatewayHandler::onEntityJoinLevel);
      gameBus.addListener(ModRecipeHandler::onServerStarted);
      gameBus.addListener(com.dingdongji.mod.event.ModEvents::onJiAnvilPlaced);
      gameBus.addListener(com.dingdongji.mod.event.ModEvents::onBeamLockedBlockUse);
      gameBus.addListener(com.dingdongji.mod.event.ModEvents::onServerTick);
      gameBus.addListener(com.dingdongji.mod.event.ModEvents::onServerStopping);
   }

   private void commonSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
      event.enqueueWork(() -> dev.dubhe.anvilcraft.inventory.RoyalGrindstoneMenu.REPAIR_COST_RECIPES.put(
         (net.minecraft.world.item.Item)ModItems.HEAVY_GOLD_BLOCK.get(),
         new dev.dubhe.anvilcraft.inventory.RoyalGrindstoneMenu.RepairCostRecipeEntry(
            81, (net.minecraft.world.item.Item)ModItems.CURSED_HEAVY_GOLD_BLOCK.get()
         )
      ));
      event.enqueueWork(com.dingdongji.mod.anvil.EnchantedBeaconConversionBehavior::register);
      event.enqueueWork(com.dingdongji.mod.anvil.HeavyGoldBeaconConversionBehavior::register);
      event.enqueueWork(com.dingdongji.mod.anvil.SupercooledTranscendiumBehavior::register);
   }
}
