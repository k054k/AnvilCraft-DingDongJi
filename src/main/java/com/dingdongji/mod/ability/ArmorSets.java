package com.dingdongji.mod.ability;

import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.ModItems;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ArmorSets {
   /** 套装开关 action bar 统一格式：标题白色 + 状态色（开绿/关红）。 */
   public static Component actionMessage(String title, String suffix, ChatFormatting suffixColor) {
      return Component.empty().append(Component.literal(title + "：").withStyle(ChatFormatting.WHITE)).append(Component.literal(suffix).withStyle(suffixColor));
   }

   public static boolean hasFullJiSet(Player player) {
      return player.getItemBySlot(EquipmentSlot.HEAD).is((Item)ModItems.JI_HELMET.get())
         && player.getItemBySlot(EquipmentSlot.CHEST).is((Item)ModItems.JI_CHESTPLATE.get())
         && player.getItemBySlot(EquipmentSlot.LEGS).is((Item)ModItems.JI_LEGGINGS.get())
         && player.getItemBySlot(EquipmentSlot.FEET).is((Item)ModItems.JI_BOOTS.get());
   }

   public static boolean hasFullSpectralSet(Player player) {
      boolean head = player.getItemBySlot(EquipmentSlot.HEAD).is((Item)ModItems.SPECTRAL_HELMET.get());
      boolean chest = player.getItemBySlot(EquipmentSlot.CHEST).is((Item)ModItems.SPECTRAL_CHESTPLATE.get());
      boolean legs = player.getItemBySlot(EquipmentSlot.LEGS).is((Item)ModItems.SPECTRAL_LEGGINGS.get());
      boolean feet = player.getItemBySlot(EquipmentSlot.FEET).is((Item)ModItems.SPECTRAL_BOOTS.get());
      return head && chest && legs && feet;
   }

   public static boolean hasFullEmberSet(Player player) {
      return player.getItemBySlot(EquipmentSlot.HEAD).is((Item)ModItems.EMBER_METAL_HELMET.get())
         && player.getItemBySlot(EquipmentSlot.CHEST).is((Item)ModItems.EMBER_METAL_CHESTPLATE.get())
         && player.getItemBySlot(EquipmentSlot.LEGS).is((Item)ModItems.EMBER_METAL_LEGGINGS.get())
         && player.getItemBySlot(EquipmentSlot.FEET).is((Item)ModItems.EMBER_METAL_BOOTS.get());
   }

   public static boolean hasFullFrostSet(Player player) {
      return player.getItemBySlot(EquipmentSlot.HEAD).is((Item)ModItems.FROST_METAL_HELMET.get())
         && player.getItemBySlot(EquipmentSlot.CHEST).is((Item)ModItems.FROST_METAL_CHESTPLATE.get())
         && player.getItemBySlot(EquipmentSlot.LEGS).is((Item)ModItems.FROST_METAL_LEGGINGS.get())
         && player.getItemBySlot(EquipmentSlot.FEET).is((Item)ModItems.FROST_METAL_BOOTS.get());
   }

   public static boolean hasFullRoyalSet(Player player) {
      return player.getItemBySlot(EquipmentSlot.HEAD).is((Item)ModItems.ROYAL_STEEL_HELMET.get())
         && player.getItemBySlot(EquipmentSlot.CHEST).is((Item)ModItems.ROYAL_STEEL_CHESTPLATE.get())
         && player.getItemBySlot(EquipmentSlot.LEGS).is((Item)ModItems.ROYAL_STEEL_LEGGINGS.get())
         && player.getItemBySlot(EquipmentSlot.FEET).is((Item)ModItems.ROYAL_STEEL_BOOTS.get());
   }

   /**
    * 0.1.0 套装化后的伤害免疫口径：
    * - 蹈虚靴单件：摔落免疫；
    * - 余烬全套：火焰伤害；超限全套：火焰伤害（隐式，不写 tooltip）；
    * - 浮霜全套：冰冻伤害；超限全套：冰冻伤害（隐式）；
    * - 超限全套：溺水、墙内窒息（相位穿墙配套）、魔法/虚空/接触/爆炸。
    */
   public static boolean isArmorProtectedDamage(Player player, DamageSource source) {
      ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
      boolean fullTrans = hasFullTranscendiumSet(player);
      if (boots.is((Item)ModItems.TRANSCENDIUM_BOOTS.get()) && source.is(DamageTypes.FALL)) {
         return true;
      }
      if (source.is(DamageTypeTags.IS_FIRE) && (hasFullEmberSet(player) || fullTrans)) {
         return true;
      }
      if (source.is(DamageTypeTags.IS_FREEZING) && (hasFullFrostSet(player) || fullTrans)) {
         return true;
      }
      if (fullTrans) {
         if (source.is(DamageTypeTags.IS_DROWNING)) {
            return true;
         }
         // IN_WALL 窒息仅在墙体相位激活时豁免，见 onLivingDamage
         if (isBarrierIIIgnored(source)) {
            return true;
         }
      }
      return false;
   }

   public static boolean hasFullTranscendiumSet(Player player) {
      return player.getItemBySlot(EquipmentSlot.HEAD).is((Item)ModItems.TRANSCENDIUM_HELMET.get())
         && player.getItemBySlot(EquipmentSlot.CHEST).is((Item)ModItems.TRANSCENDIUM_CHESTPLATE.get())
         && player.getItemBySlot(EquipmentSlot.LEGS).is((Item)ModItems.TRANSCENDIUM_LEGGINGS.get())
         && player.getItemBySlot(EquipmentSlot.FEET).is((Item)ModItems.TRANSCENDIUM_BOOTS.get());
   }

   /**
    * 是否为铁砧工艺的太空维度（全套超限"无视星球引力"仅在这些维度生效）：
    * 月球 anvilcraft:mun、锻星砧类地星球 anvilcraft:overworld_like、
    * 锻星砧虚空星球 anvilcraft:void_planet。
    * 普通维度（主世界/下界/末地）绝不命中，保持原版重力。
    */
   public static boolean isAnvilSpaceDimension(Entity entity) {
      ResourceLocation dim = entity.level().dimension().location();
      if (!"anvilcraft".equals(dim.getNamespace())) {
         return false;
      }
      String path = dim.getPath();
      return "mun".equals(path) || "void_planet".equals(path) || "overworld_like".equals(path);
   }

   public static boolean ignoresBlockSlowdown(Entity entity) {
      if (entity instanceof Player player) {
         ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
         if (!boots.is((Item)ModItems.TRANSCENDIUM_BOOTS.get()) && !boots.has((DataComponentType)ModComponents.FROST_WALK.get())) {
            return hasFullFrostSet(player);
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   private static boolean isBarrierIIIgnored(DamageSource source) {
      return source.is(DamageTypes.MAGIC)
         || source.is(DamageTypes.INDIRECT_MAGIC)
         || source.is(DamageTypes.DRAGON_BREATH)
         || source.is(DamageTypes.WITHER)
         || source.is(DamageTypes.WITHER_SKULL)
         || source.is(DamageTypes.SONIC_BOOM)
         || source.is(DamageTypes.FELL_OUT_OF_WORLD)
         || source.is(DamageTypes.CACTUS)
         || source.is(DamageTypes.SWEET_BERRY_BUSH)
         || source.is(DamageTypes.THORNS)
         || source.is(DamageTypes.EXPLOSION)
         || source.is(DamageTypes.PLAYER_EXPLOSION)
         || source.is(DamageTypes.BAD_RESPAWN_POINT);
   }

   public static void clearOnLogout(UUID uuid) {
   }

   public static void save(CompoundTag data, UUID uuid) {
   }

   public static void load(CompoundTag data, UUID uuid) {
   }
}
