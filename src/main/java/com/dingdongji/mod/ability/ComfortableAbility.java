package com.dingdongji.mod.ability;

import com.dingdongji.mod.event.ModArmorSetHandler;
import com.dingdongji.mod.item.ModItems;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ComfortableAbility {
   public static final Map<UUID, Boolean> COMFORTABLE_ENABLED = new HashMap<>();
   public static final ResourceLocation COMFORTABLE_SPEED_ID = ResourceLocation.parse("dingdongji:comfortable_speed");
   public static final ResourceLocation COMFORTABLE_STEP_ID = ResourceLocation.parse("dingdongji:comfortable_step");

   public static void toggleComfortable(ServerPlayer player) {
      ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
      if (boots.is((Item)ModItems.ROYAL_STEEL_BOOTS.get())) {
         UUID uuid = player.getUUID();
         boolean enabled = COMFORTABLE_ENABLED.getOrDefault(uuid, false);
         enabled = !enabled;
         COMFORTABLE_ENABLED.put(uuid, enabled);
         player.displayClientMessage(ArmorSets.actionMessage("舒适", enabled ? "开" : "关", enabled ? ChatFormatting.GREEN : ChatFormatting.RED), true);
         ModArmorSetHandler.saveToggleStates(player);
      }
   }

   public static void handleComfortable(Player player) {
      ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
      boolean hasComfort = boots.is((Item)ModItems.ROYAL_STEEL_BOOTS.get()) && COMFORTABLE_ENABLED.getOrDefault(player.getUUID(), false);
      AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
      AttributeInstance stepHeight = player.getAttribute(Attributes.STEP_HEIGHT);
      if (hasComfort) {
         if (moveSpeed != null) {
            moveSpeed.removeModifier(COMFORTABLE_SPEED_ID);
            moveSpeed.addTransientModifier(new AttributeModifier(COMFORTABLE_SPEED_ID, 0.02, Operation.ADD_VALUE));
         }

         if (stepHeight != null) {
            stepHeight.removeModifier(COMFORTABLE_STEP_ID);
            stepHeight.addTransientModifier(new AttributeModifier(COMFORTABLE_STEP_ID, 0.5, Operation.ADD_VALUE));
         }
      } else {
         if (moveSpeed != null) {
            moveSpeed.removeModifier(COMFORTABLE_SPEED_ID);
         }

         if (stepHeight != null) {
            stepHeight.removeModifier(COMFORTABLE_STEP_ID);
         }
      }
   }

   public static void clearOnLogout(UUID uuid) {
      COMFORTABLE_ENABLED.remove(uuid);
   }

   public static void save(CompoundTag data, UUID uuid) {
      data.putBoolean("dingdongji:comfortable", COMFORTABLE_ENABLED.getOrDefault(uuid, false));
   }

   public static void load(CompoundTag data, UUID uuid) {
      COMFORTABLE_ENABLED.put(uuid, data.getBoolean("dingdongji:comfortable"));
   }
}
