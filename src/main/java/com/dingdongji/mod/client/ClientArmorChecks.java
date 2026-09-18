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

   public static boolean helmetNightVision() {
      return hasTranscendiumHelmet() && ClientAbilityState.nightVision();
   }

   // Fog removal is active with the Transcendium helmet everywhere except
   // the Overworld, whose distance fog barely affects visibility and is
   // kept for the natural look.
   public static boolean shouldClearFog() {
      if (!hasTranscendiumHelmet()) {
         return false;
      } else {
         ClientLevel level = Minecraft.getInstance().level;
         return level == null || !level.dimension().equals(Level.OVERWORLD);
      }
   }
}
