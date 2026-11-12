package com.dingdongji.mod.client;

import com.dingdongji.mod.KryptonMod;
import com.dingdongji.mod.ModMenuTypes;
import com.dingdongji.mod.client.input.ModKeyBindings;
import com.dingdongji.mod.client.model.SquatGeneratorModel;
import com.dingdongji.mod.client.render.AnchoredBeaconRenderer;
import com.dingdongji.mod.client.render.PhantomDecoyRenderer;
import com.dingdongji.mod.client.screen.JiAnvilScreen;
import com.dingdongji.mod.init.ModEntities;
import com.dingdongji.mod.inventory.JiAnvilMenu;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.platform.ClientViews;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** 全部客户端 mod-bus 注册集中于此；按 Dist.CLIENT 订阅，专用服务端不会加载本类。 */
@EventBusSubscriber(value = Dist.CLIENT, bus = Bus.MOD, modid = KryptonMod.MODID)
public final class ClientModInit {
   static {
      // 类加载即安装桥接实现，早于任何玩家 tick 中的客户端状态查询
      ClientViews.install(new ClientViewImpl());
   }

   private ClientModInit() {
   }

   @SubscribeEvent
   public static void onClientSetup(FMLClientSetupEvent event) {
      ModList.get()
         .getModContainerById(KryptonMod.MODID)
         .ifPresent(container -> container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new));
      ClientSetupHandler.onClientSetup(event);
   }

   @SubscribeEvent
   public static void registerKeyMappings(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
      ModKeyBindings.registerKeyMappings(event);
   }

   @SubscribeEvent
   public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
      ClientSetupHandler.registerParticleProviders(event);
   }

   @SubscribeEvent
   public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
      event.registerLayerDefinition(SquatGeneratorModel.LAYER, SquatGeneratorModel::createBodyLayer);
   }

   @SubscribeEvent
   public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
      event.registerItem(new SquatGeneratorClientExtensions(), ModItems.SQUAT_GENERATOR_LEGGINGS.get());
   }

   @SubscribeEvent
   public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
      event.registerEntityRenderer(ModEntities.PHANTOM_DECOY.get(), PhantomDecoyRenderer::new);
      // 覆盖原版信标渲染器：铭化信标画纯色锥体光柱，其余信标内部委托回原版
      event.registerBlockEntityRenderer(BlockEntityType.BEACON, AnchoredBeaconRenderer::new);
   }

   @SubscribeEvent
   public static void registerScreens(RegisterMenuScreensEvent event) {
      MenuType<JiAnvilMenu> type = (MenuType<JiAnvilMenu>)ModMenuTypes.JI_ANVIL.get();
      event.register(type, JiAnvilScreen::new);
   }

   /** 给所有腿护甲（含铁砧两件套）注册口袋角标。 */
   @SubscribeEvent
   public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
      for (var item : BuiltInRegistries.ITEM) {
         if (item instanceof ArmorItem armor && armor.getType().getSlot() == EquipmentSlot.LEGS) {
            event.register(item, PouchDecoration.INSTANCE);
         }
      }
   }
}
