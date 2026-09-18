package com.dingdongji.mod.input;

import com.dingdongji.mod.client.ClientAbilityState;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.network.AbilityTogglePacket;
import com.dingdongji.mod.network.GlowingVisionTogglePacket;
import com.dingdongji.mod.network.NeutronBarrierTogglePacket;
import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;

public class ModKeyBindings {
   public static final String CATEGORY = "key.categories.dingdongji";
   public static final String ABILITY_NAME = "key.dingdongji.ability_toggle";
   public static final String FROST_SLIDE_NAME = "key.dingdongji.frost_slide_toggle";
   public static final String GLOWING_NAME = "key.dingdongji.glowing_vision_toggle";
   public static final String NEUTRON_BARRIER_NAME = "key.dingdongji.neutron_barrier_toggle";
   public static final KeyMapping ABILITY_KEY = new KeyMapping(
      "key.dingdongji.ability_toggle", KeyConflictContext.IN_GAME, Type.KEYSYM, 86, "key.categories.dingdongji"
   );
   public static final KeyMapping FROST_SLIDE_KEY = new KeyMapping(
      "key.dingdongji.frost_slide_toggle", KeyConflictContext.IN_GAME, Type.KEYSYM, 86, "key.categories.dingdongji"
   );
   public static final KeyMapping GLOWING_VISION_KEY = new KeyMapping(
      "key.dingdongji.glowing_vision_toggle", KeyConflictContext.IN_GAME, Type.KEYSYM, 67, "key.categories.dingdongji"
   );
   public static final KeyMapping NEUTRON_BARRIER_KEY = new KeyMapping(
      "key.dingdongji.neutron_barrier_toggle", KeyConflictContext.IN_GAME, Type.KEYSYM, 90, "key.categories.dingdongji"
   );

   public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
      event.register(ABILITY_KEY);
      event.register(FROST_SLIDE_KEY);
      event.register(GLOWING_VISION_KEY);
      event.register(NEUTRON_BARRIER_KEY);
   }

   private static void sendBootsAbilityToggle(Minecraft mc) {
      if (mc.player != null) {
         ItemStack boots = mc.player.getItemBySlot(EquipmentSlot.FEET);
         if (boots.is((Item)ModItems.EMBER_METAL_BOOTS.get())) {
            ClientAbilityState.lavaWalker = !ClientAbilityState.lavaWalker;
         } else if (boots.is((Item)ModItems.FROST_METAL_BOOTS.get())) {
            ClientAbilityState.frostSlide = !ClientAbilityState.frostSlide;
         }
      }

      PacketDistributor.sendToServer(new AbilityTogglePacket(false), new CustomPacketPayload[0]);
   }

   public static void tick(Minecraft mc) {
      while (ABILITY_KEY.consumeClick()) {
         sendBootsAbilityToggle(mc);
      }

      boolean sameBinding = ABILITY_KEY.getKey().equals(FROST_SLIDE_KEY.getKey());

      while (FROST_SLIDE_KEY.consumeClick()) {
         if (!sameBinding) {
            sendBootsAbilityToggle(mc);
         }
      }

      for (; GLOWING_VISION_KEY.consumeClick(); PacketDistributor.sendToServer(new GlowingVisionTogglePacket(), new CustomPacketPayload[0])) {
         if (mc.player != null) {
            ItemStack helmet = mc.player.getItemBySlot(EquipmentSlot.HEAD);
            if (helmet.is((Item)ModItems.TRANSCENDIUM_HELMET.get())) {
               ClientAbilityState.helmetMode = (ClientAbilityState.helmetMode + 1) % 6;
            }
         }
      }

      while (NEUTRON_BARRIER_KEY.consumeClick()) {
         PacketDistributor.sendToServer(new NeutronBarrierTogglePacket(), new CustomPacketPayload[0]);
      }
   }
}
