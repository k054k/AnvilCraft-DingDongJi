package com.dingdongji.mod.ability;

import com.dingdongji.mod.event.ModArmorSetHandler;
import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.item.component.MeaninglessData;
import com.dingdongji.mod.util.AnvilCraftCompat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemAttributeModifiers.Builder;
import net.minecraft.world.item.component.ItemAttributeModifiers.Entry;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments.Mutable;
import net.minecraft.world.phys.Vec3;

public class TranscendiumSet {
   public static final Map<UUID, Boolean> TRANSCENDIUM_PHASE_ENABLED = new HashMap<>();
   private static final ResourceLocation MEANINGLESS_ARMOR_ID = ResourceLocation.fromNamespaceAndPath("dingdongji", "meaningless_armor");
   private static final ResourceLocation MEANINGLESS_TOUGHNESS_ID = ResourceLocation.fromNamespaceAndPath("dingdongji", "meaningless_toughness");

   public static void toggleTranscendiumPhase(ServerPlayer player) {
      ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
      if (boots.is((Item)ModItems.TRANSCENDIUM_BOOTS.get())) {
         UUID uuid = player.getUUID();
         boolean enabled = !TRANSCENDIUM_PHASE_ENABLED.getOrDefault(uuid, false);
         if (enabled && !ArmorSets.hasFullTranscendiumSet(player)) {
            player.displayClientMessage(Component.literal("相位偏移需要穿戴全套超限合金盔甲").withStyle(ChatFormatting.RED), true);
            return;
         }

         TRANSCENDIUM_PHASE_ENABLED.put(uuid, enabled);
         player.displayClientMessage(ArmorSets.actionMessage("相位偏移", enabled ? "开" : "关", enabled ? ChatFormatting.GREEN : ChatFormatting.RED), true);
         ModArmorSetHandler.saveToggleStates(player);
         ModArmorSetHandler.syncAbilityState(player);
      }
   }

   /**
    * 超限全套的虚空保护，对齐本体耐候航天套：在世界最低高度（主世界 -64、
    * 下界/末地 0）形成隐形地板；站到地板上；一旦掉到地板以下则以 +0.2/tick
    * 向上托举漂浮。飞行/旁观不干预。
    */
   public static void handleVoidFloor(Player player) {
      if (!ArmorSets.hasFullTranscendiumSet(player) || player.getAbilities().flying || player.isSpectator()) {
         return;
      }
      double minY = player.level().getMinBuildHeight();
      double y = player.getY();
      Vec3 mot = player.getDeltaMovement();
      if (y < minY) {
         player.setDeltaMovement(mot.x, 0.2D, mot.z);
         player.fallDistance = 0.0F;
         player.setOnGround(false);
         return;
      }
      if (y + mot.y < minY) {
         player.setPos(player.getX(), minY, player.getZ());
         player.setDeltaMovement(mot.x, 0.0D, mot.z);
         player.setOnGround(true);
         player.fallDistance = 0.0F;
      } else if (y <= minY + 0.05D) {
         player.setOnGround(true);
         if (mot.y < 0.0D) {
            player.setDeltaMovement(mot.x, 0.0D, mot.z);
         }
         player.fallDistance = 0.0F;
      }
   }

   public static void handleMeaninglessConversion(Player player) {
      for (ItemStack stack : player.getInventory().items) {
         tryConvertFrostMetalPiece(player, stack);
      }

      for (ItemStack stack : player.getInventory().armor) {
         tryConvertFrostMetalPiece(player, stack);
      }

      tryConvertFrostMetalPiece(player, player.getOffhandItem());
   }

   private static void tryConvertFrostMetalPiece(Player player, ItemStack stack) {
      Type type = null;
      if (stack.is((Item)ModItems.FROST_METAL_HELMET.get())) {
         type = Type.HELMET;
      } else if (stack.is((Item)ModItems.FROST_METAL_CHESTPLATE.get())) {
         type = Type.CHESTPLATE;
      } else if (stack.is((Item)ModItems.FROST_METAL_LEGGINGS.get())) {
         type = Type.LEGGINGS;
      } else {
         if (!stack.is((Item)ModItems.FROST_METAL_BOOTS.get())) {
            return;
         }

         type = Type.BOOTS;
      }

      convertPiece(player, stack, type);
   }

   private static void convertPiece(Player player, ItemStack stack, Type armorType) {
      ItemEnchantments enchantments = (ItemEnchantments)stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
      if (!enchantments.isEmpty()) {
         ItemEnchantments convertedEnchs = ((MeaninglessData)stack.getOrDefault((DataComponentType)ModComponents.MEANINGLESS_DATA.get(), MeaninglessData.EMPTY))
            .convertedEnchantments();
         int newLevels = 0;
         Mutable convertedEnchsMut = new Mutable(convertedEnchs);

         for (Holder<Enchantment> enchantment : enchantments.keySet()) {
            int level = enchantments.getLevel(enchantment);
            if (convertedEnchs.getLevel(enchantment) < level) {
               int addLevels = level - convertedEnchs.getLevel(enchantment);
               if (addLevels > 0) {
                  newLevels += addLevels;
               }

               convertedEnchsMut.set(enchantment, level);
            }
         }

         if (newLevels != 0) {
            int totalLevels = 0;

            for (Holder<Enchantment> e : convertedEnchsMut.keySet()) {
               totalLevels += convertedEnchsMut.getLevel(e);
            }

            stack.set((DataComponentType)ModComponents.MEANINGLESS_DATA.get(), new MeaninglessData(totalLevels, convertedEnchsMut.toImmutable()));
            float bonusArmor = (float)Math.round(Math.sqrt((double)totalLevels) * 1.5 + (double)totalLevels / 5.0);
            float bonusToughness = (float)totalLevels / 3.0F;
            Builder builder = ItemAttributeModifiers.builder();
            if (bonusArmor > 0.0F) {
               builder.add(
                  Attributes.ARMOR,
                  new AttributeModifier(MEANINGLESS_ARMOR_ID, (double)bonusArmor, Operation.ADD_VALUE),
                  EquipmentSlotGroup.bySlot(armorType.getSlot())
               );
            }

            if (bonusToughness > 0.0F) {
               builder.add(
                  Attributes.ARMOR_TOUGHNESS,
                  new AttributeModifier(MEANINGLESS_TOUGHNESS_ID, (double)bonusToughness, Operation.ADD_VALUE),
                  EquipmentSlotGroup.bySlot(armorType.getSlot())
               );
            }

            for (Entry entry : stack.getAttributeModifiers().modifiers()) {
               if (!entry.modifier().is(MEANINGLESS_ARMOR_ID) && !entry.modifier().is(MEANINGLESS_TOUGHNESS_ID)) {
                  builder.add(entry.attribute(), entry.modifier(), entry.slot());
               }
            }

            stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
         }
      }
   }

   public static void applyAnvilCraftComponent(ItemStack stack) {
      if (stack.is((Item)ModItems.JI_SWORD.get())
         || stack.is((Item)ModItems.JI_PICKAXE.get())
         || stack.is((Item)ModItems.JI_HELMET.get())
         || stack.is((Item)ModItems.JI_CHESTPLATE.get())
         || stack.is((Item)ModItems.JI_LEGGINGS.get())
         || stack.is((Item)ModItems.JI_BOOTS.get())) {
         AnvilCraftCompat.setFortune(stack);
      }

      if (stack.is((Item)ModItems.EMBER_METAL_HELMET.get())
         || stack.is((Item)ModItems.EMBER_METAL_CHESTPLATE.get())
         || stack.is((Item)ModItems.EMBER_METAL_LEGGINGS.get())
         || stack.is((Item)ModItems.EMBER_METAL_BOOTS.get())) {
         AnvilCraftCompat.setReforging(stack);
      }

      if (stack.is((Item)ModItems.TRANSCENDIUM_HELMET.get())
         || stack.is((Item)ModItems.TRANSCENDIUM_CHESTPLATE.get())
         || stack.is((Item)ModItems.TRANSCENDIUM_LEGGINGS.get())
         || stack.is((Item)ModItems.TRANSCENDIUM_BOOTS.get())) {
         AnvilCraftCompat.setEternal(stack);
      }
   }

   public static void clearOnLogout(UUID uuid) {
      TRANSCENDIUM_PHASE_ENABLED.remove(uuid);
   }

   public static void save(CompoundTag data, UUID uuid) {
      data.putBoolean("dingdongji:transcendium_phase", TRANSCENDIUM_PHASE_ENABLED.getOrDefault(uuid, false));
   }

   public static void load(CompoundTag data, UUID uuid) {
      TRANSCENDIUM_PHASE_ENABLED.put(uuid, data.getBoolean("dingdongji:transcendium_phase"));
   }
}
