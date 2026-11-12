package com.dingdongji.mod.network;

import com.dingdongji.mod.event.ModArmorSetHandler;
import com.dingdongji.mod.event.PhantomDecoyManager;
import com.dingdongji.mod.event.SquatGeneratorManager;
import com.dingdongji.mod.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetwork {
   public static void register(RegisterPayloadHandlersEvent event) {
      PayloadRegistrar registrar = event.registrar("2");
      registrar.playToServer(AbilityTogglePacket.TYPE, AbilityTogglePacket.STREAM_CODEC, new ModNetwork.AbilityToggleHandler());
      registrar.playToServer(GlowingVisionTogglePacket.TYPE, GlowingVisionTogglePacket.STREAM_CODEC, new ModNetwork.GlowingVisionToggleHandler());
      registrar.playToServer(NeutronBarrierTogglePacket.TYPE, NeutronBarrierTogglePacket.STREAM_CODEC, new ModNetwork.NeutronBarrierToggleHandler());
      registrar.playToServer(FullSetTogglePacket.TYPE, FullSetTogglePacket.STREAM_CODEC, new ModNetwork.FullSetToggleHandler());
      registrar.playToServer(SquatCrouchPacket.TYPE, SquatCrouchPacket.STREAM_CODEC, new ModNetwork.SquatCrouchHandler());
      registrar.playToServer(ArmorFunctionSelectPacket.TYPE, ArmorFunctionSelectPacket.STREAM_CODEC, new ModNetwork.ArmorFunctionSelectHandler());
      registrar.playToServer(SwitchTemplateModePacket.TYPE, SwitchTemplateModePacket.STREAM_CODEC, SwitchTemplateModePacket::handle);
      registrar.playToServer(SelectTemplateModePacket.TYPE, SelectTemplateModePacket.STREAM_CODEC, SelectTemplateModePacket::handle);
      // playToClient 注册必须在 common 侧：专用服务器也要知道这两个包，
      // 否则服务端 sendToPlayer 时抛 UnsupportedOperationException。
      // 处理器 lambda 内的客户端类（ClientAbilityState/IonocraftBootsClientHandler）
      // 由 invokedynamic 惰性链接，仅在客户端实际收到包时才加载，服务端安全。
      registrar.playToClient(IonocraftBootsFlyingPacket.TYPE, IonocraftBootsFlyingPacket.STREAM_CODEC, (packet, context) ->
         context.enqueueWork(() -> com.dingdongji.mod.client.IonocraftBootsClientHandler.onFlyingSync(packet.playerId(), packet.mode())));
      registrar.playToClient(AbilityStateSyncPacket.TYPE, AbilityStateSyncPacket.STREAM_CODEC, (packet, context) ->
         context.enqueueWork(() -> com.dingdongji.mod.client.ClientAbilityState.applySync(
            packet.lavaWalker(), packet.frostSlide(), packet.helmetMode(),
            packet.phaseVertical(), packet.transcendiumPhase(), packet.phantom())));
      registrar.playToClient(PhantomStateSyncPacket.TYPE, PhantomStateSyncPacket.STREAM_CODEC, (packet, context) ->
         context.enqueueWork(() -> com.dingdongji.mod.client.ClientAbilityState.applyRemotePhantom(
            packet.playerId(), packet.phantom())));
   }

   private static void handleAbilityToggle(ServerPlayer player, boolean doublePress) {
      ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
      if (boots.is((Item)ModItems.ROYAL_STEEL_BOOTS.get())) {
         ModArmorSetHandler.toggleComfortable(player);
      } else if (boots.is((Item)ModItems.EMBER_METAL_BOOTS.get())) {
         ModArmorSetHandler.toggleLavaWalker(player);
      } else if (boots.is((Item)ModItems.FROST_METAL_BOOTS.get())) {
         ModArmorSetHandler.toggleFrostSlide(player);
      } else if (boots.is((Item)ModItems.SPECTRAL_BOOTS.get())) {
         ModArmorSetHandler.togglePhaseVertical(player);
      } else if (boots.is((Item)ModItems.TRANSCENDIUM_BOOTS.get())) {
         ModArmorSetHandler.toggleIonocraftFlight(player);
      } else {
         // No recognized boots equipped: the client may have optimistically
         // flipped its local state in the same tick the boots came off, so
         // resync the authoritative state to roll it back.
         ModArmorSetHandler.syncAbilityState(player);
      }
   }

   public static class AbilityToggleHandler implements IPayloadHandler<AbilityTogglePacket> {
      public void handle(AbilityTogglePacket packet, IPayloadContext context) {
         context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
               ModNetwork.handleAbilityToggle(serverPlayer, packet.doublePress());
            }
         });
      }
   }

   public static class GlowingVisionToggleHandler implements IPayloadHandler<GlowingVisionTogglePacket> {
      public void handle(GlowingVisionTogglePacket packet, IPayloadContext context) {
         context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
               ModArmorSetHandler.toggleGlowingVision(serverPlayer);
            }
         });
      }
   }

   public static class NeutronBarrierToggleHandler implements IPayloadHandler<NeutronBarrierTogglePacket> {
      public void handle(NeutronBarrierTogglePacket packet, IPayloadContext context) {
         context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
               ModArmorSetHandler.toggleNeutronBarrier(serverPlayer);
            }
         });
      }
   }

   /**
    * 完整套装开关分发：
    * 幻影/相位激活中 → 关闭对应功能；
    * 否则按穿着判定——幻灵全套开幻影，叽全套开关套装效果，超限全套开相位（未穿全套时各自提示）。
    */
   public static class FullSetToggleHandler implements IPayloadHandler<FullSetTogglePacket> {
      public void handle(FullSetTogglePacket packet, IPayloadContext context) {
         context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
               if (PhantomDecoyManager.isPhantomState(serverPlayer)) {
                  PhantomDecoyManager.toggle(serverPlayer);
               } else if (ModArmorSetHandler.isTranscendiumFlightPhasing(serverPlayer)) {
                  ModArmorSetHandler.toggleTranscendiumPhase(serverPlayer);
               } else if (ModArmorSetHandler.hasFullSpectralSet(serverPlayer)) {
                  PhantomDecoyManager.toggle(serverPlayer);
               } else if (ModArmorSetHandler.hasFullJiSet(serverPlayer)) {
                  ModArmorSetHandler.toggleJiSet(serverPlayer);
               } else {
                  ModArmorSetHandler.toggleTranscendiumPhase(serverPlayer);
               }
            }
         });
      }
   }

   public static class SquatCrouchHandler implements IPayloadHandler<SquatCrouchPacket> {
      public void handle(SquatCrouchPacket packet, IPayloadContext context) {
         context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
               SquatGeneratorManager.onClientCrouch(serverPlayer.getUUID(), packet.crouching());
            }
         });
      }
   }

   public static class ArmorFunctionSelectHandler implements IPayloadHandler<ArmorFunctionSelectPacket> {
      public void handle(ArmorFunctionSelectPacket packet, IPayloadContext context) {
         context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
               ModArmorSetHandler.selectArmorFunctionFromHand(serverPlayer, packet.hand(), packet.state());
            }
         });
      }
   }
}
