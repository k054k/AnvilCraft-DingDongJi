package com.dingdongji.mod.network;

import com.dingdongji.mod.client.ClientAbilityState;
import com.dingdongji.mod.client.IonocraftBootsClientHandler;
import com.dingdongji.mod.event.ModArmorSetHandler;
import com.dingdongji.mod.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetwork {
   public static void register(RegisterPayloadHandlersEvent event) {
      PayloadRegistrar registrar = event.registrar("1");
      registrar.playToServer(AbilityTogglePacket.TYPE, AbilityTogglePacket.STREAM_CODEC, new ModNetwork.AbilityToggleHandler());
      registrar.playToServer(GlowingVisionTogglePacket.TYPE, GlowingVisionTogglePacket.STREAM_CODEC, new ModNetwork.GlowingVisionToggleHandler());
      registrar.playToServer(NeutronBarrierTogglePacket.TYPE, NeutronBarrierTogglePacket.STREAM_CODEC, new ModNetwork.NeutronBarrierToggleHandler());
      registrar.playToServer(SwitchTemplateModePacket.TYPE, SwitchTemplateModePacket.STREAM_CODEC, SwitchTemplateModePacket::handle);
      registrar.playToServer(SelectTemplateModePacket.TYPE, SelectTemplateModePacket.STREAM_CODEC, SelectTemplateModePacket::handle);
      registrar.playToClient(IonocraftBootsFlyingPacket.TYPE, IonocraftBootsFlyingPacket.STREAM_CODEC, new ModNetwork.IonocraftBootsFlyingHandler());
      registrar.playToClient(AbilityStateSyncPacket.TYPE, AbilityStateSyncPacket.STREAM_CODEC, new ModNetwork.AbilityStateSyncHandler());
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

   public static class AbilityStateSyncHandler implements IPayloadHandler<AbilityStateSyncPacket> {
      public void handle(AbilityStateSyncPacket packet, IPayloadContext context) {
         context.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) {
               ClientAbilityState.applySync(packet.lavaWalker(), packet.frostSlide(), packet.helmetMode(), packet.phaseVertical());
            }
         });
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

   public static class IonocraftBootsFlyingHandler implements IPayloadHandler<IonocraftBootsFlyingPacket> {
      public void handle(IonocraftBootsFlyingPacket packet, IPayloadContext context) {
         context.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) {
               IonocraftBootsClientHandler.onFlyingSync(packet.playerId(), packet.mode());
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
}
