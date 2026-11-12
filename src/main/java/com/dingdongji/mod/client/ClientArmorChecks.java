package com.dingdongji.mod.client;

import com.dingdongji.mod.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ClientArmorChecks {
   private ClientArmorChecks() {
   }

   public static Player localPlayer() {
      return Minecraft.getInstance().player;
   }

   public static boolean hasTranscendiumHelmet() {
      Player player = localPlayer();
      if (player == null) {
         return false;
      } else {
         ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
         return helmet.is((Item)ModItems.TRANSCENDIUM_HELMET.get());
      }
   }

   public static boolean hasEmberHelmet() {
      Player player = localPlayer();
      if (player == null) {
         return false;
      } else {
         ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
         return helmet.is((Item)ModItems.EMBER_METAL_HELMET.get());
      }
   }

   public static boolean hasFrostHelmet() {
      Player player = localPlayer();
      if (player == null) {
         return false;
      } else {
         ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
         return helmet.is((Item)ModItems.FROST_METAL_HELMET.get());
      }
   }

   public static boolean hasFullEmberSet() {
      Player player = localPlayer();
      return player != null
         && player.getItemBySlot(EquipmentSlot.HEAD).is((Item)ModItems.EMBER_METAL_HELMET.get())
         && player.getItemBySlot(EquipmentSlot.CHEST).is((Item)ModItems.EMBER_METAL_CHESTPLATE.get())
         && player.getItemBySlot(EquipmentSlot.LEGS).is((Item)ModItems.EMBER_METAL_LEGGINGS.get())
         && player.getItemBySlot(EquipmentSlot.FEET).is((Item)ModItems.EMBER_METAL_BOOTS.get());
   }

   public static boolean hasFullFrostSet() {
      Player player = localPlayer();
      return player != null
         && player.getItemBySlot(EquipmentSlot.HEAD).is((Item)ModItems.FROST_METAL_HELMET.get())
         && player.getItemBySlot(EquipmentSlot.CHEST).is((Item)ModItems.FROST_METAL_CHESTPLATE.get())
         && player.getItemBySlot(EquipmentSlot.LEGS).is((Item)ModItems.FROST_METAL_LEGGINGS.get())
         && player.getItemBySlot(EquipmentSlot.FEET).is((Item)ModItems.FROST_METAL_BOOTS.get());
   }

   public static boolean hasFullTranscendiumSet() {
      Player player = localPlayer();
      return player != null
         && player.getItemBySlot(EquipmentSlot.HEAD).is((Item)ModItems.TRANSCENDIUM_HELMET.get())
         && player.getItemBySlot(EquipmentSlot.CHEST).is((Item)ModItems.TRANSCENDIUM_CHESTPLATE.get())
         && player.getItemBySlot(EquipmentSlot.LEGS).is((Item)ModItems.TRANSCENDIUM_LEGGINGS.get())
         && player.getItemBySlot(EquipmentSlot.FEET).is((Item)ModItems.TRANSCENDIUM_BOOTS.get());
   }

   public static boolean helmetNightVision() {
      return hasTranscendiumHelmet() && ClientAbilityState.nightVision();
   }

   public static boolean shouldClearFog() {
      return hasTranscendiumHelmet();
   }
}
