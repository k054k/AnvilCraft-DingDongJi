package com.dingdongji.mod.event;

import com.dingdongji.mod.input.ModKeyBindings;
import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.item.component.AccumulateData;
import com.dingdongji.mod.item.component.DevourData;
import com.dingdongji.mod.item.component.MeaninglessData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public class ModEvents {
   private static final ResourceLocation DEVOUR_DAMAGE_ID = ResourceLocation.parse("dingdongji:devour_damage");
   private static final ResourceLocation JI_SWEEP_ID = ResourceLocation.parse("dingdongji:ji_sword_sweep");
   private static final ResourceLocation JI_REACH_ID = ResourceLocation.parse("dingdongji:ji_pickaxe_reach");
   private static final ResourceLocation JI_BREAK_SPEED_ID = ResourceLocation.parse("dingdongji:ji_pickaxe_break_speed");
   private static final Style DEVOUR_STYLE = Style.EMPTY.withColor(16236032).withItalic(false);
   private static final Style ACCUMULATE_STYLE = Style.EMPTY.withColor(13828091).withItalic(false);
   private static final Style ROYAL_STEEL_STYLE = Style.EMPTY.withColor(12055252).withItalic(false);
   private static final Style JI_ARMOR_STYLE = Style.EMPTY.withColor(16777136).withItalic(false);
   private static final Style EMBER_METAL_STYLE = Style.EMPTY.withColor(16729344).withItalic(false);
   private static final Style TRANSCENDIUM_STYLE = Style.EMPTY.withColor(12211667).withItalic(false);
   private static final Style FROST_STYLE = Style.EMPTY.withColor(10344703).withItalic(false);
   private static final Style FROST_ABILITY_STYLE = Style.EMPTY.withColor(15269887).withItalic(false);
   private static final Style CONVERTED_ENCH_STYLE = Style.EMPTY.withColor(6263715).withItalic(false);
   private static final Style GRAY_STYLE = Style.EMPTY.withColor(11184810).withItalic(false);
   private static final Style CREATE_TEMPLATE_STYLE = Style.EMPTY.withColor(11141375).withItalic(false);
   private static final Style PARANOID_STYLE = Style.EMPTY.withColor(11141120).withItalic(false);

   @SubscribeEvent
   public static void onLivingDeath(LivingDeathEvent event) {
      DamageSource source = event.getSource();
      if (source.getEntity() instanceof Player player) {
         ItemStack weapon = player.getMainHandItem();
         if (weapon.has((DataComponentType)ModComponents.DEVOUR.get())) {
            DevourData data = (DevourData)weapon.get((DataComponentType)ModComponents.DEVOUR.get());
            int kills = data.kills();
            if (kills >= 0) {
               weapon.set((DataComponentType)ModComponents.DEVOUR.get(), new DevourData(kills + 1));
            }
         }
      }
   }

   @SubscribeEvent
   public static void onItemAttributeModifier(ItemAttributeModifierEvent event) {
      ItemStack stack = event.getItemStack();
      if (stack.has((DataComponentType)ModComponents.DEVOUR.get())) {
         DevourData data = (DevourData)stack.get((DataComponentType)ModComponents.DEVOUR.get());
         int kills = data.kills();
         if (kills > 0) {
            float bonus = (float)Math.sqrt((double)kills) * 2.0F;
            event.addModifier(
               Attributes.ATTACK_DAMAGE, new AttributeModifier(DEVOUR_DAMAGE_ID, (double)bonus, Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND
            );
         } else if (kills < 0) {
            event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(DEVOUR_DAMAGE_ID, 100000.0, Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
         }
      }

      if (stack.is((Item)ModItems.JI_SWORD.get())) {
         event.addModifier(Attributes.SWEEPING_DAMAGE_RATIO, new AttributeModifier(JI_SWEEP_ID, 1.0, Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
      }

      if (stack.is((Item)ModItems.JI_PICKAXE.get())) {
         event.addModifier(Attributes.BLOCK_INTERACTION_RANGE, new AttributeModifier(JI_REACH_ID, 2.0, Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
         event.addModifier(Attributes.BLOCK_BREAK_SPEED, new AttributeModifier(JI_BREAK_SPEED_ID, 1.0, Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
      }

      if (stack.is((Item)ModItems.TRANSCENDIUM_HELMET.get())
         || stack.is((Item)ModItems.TRANSCENDIUM_CHESTPLATE.get())
         || stack.is((Item)ModItems.TRANSCENDIUM_LEGGINGS.get())
         || stack.is((Item)ModItems.TRANSCENDIUM_BOOTS.get())) {
         ItemEnchantments ench = (ItemEnchantments)stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
         if (!ench.isEmpty()) {
            int levels = 0;

            for (Holder<Enchantment> e : ench.keySet()) {
               levels += ench.getLevel(e);
            }

            if (levels > 0) {
               float bonusArmor = (float)Math.round((float)(Math.sqrt((double)levels) * 1.5 + (double)levels / 5.0));
               float bonusToughness = (float)levels / 3.0F;
               EquipmentSlotGroup group;
               String slotSuffix;
               if (stack.is((Item)ModItems.TRANSCENDIUM_HELMET.get())) {
                  group = EquipmentSlotGroup.HEAD;
                  slotSuffix = "_head";
               } else if (stack.is((Item)ModItems.TRANSCENDIUM_CHESTPLATE.get())) {
                  group = EquipmentSlotGroup.CHEST;
                  slotSuffix = "_chest";
               } else if (stack.is((Item)ModItems.TRANSCENDIUM_LEGGINGS.get())) {
                  group = EquipmentSlotGroup.LEGS;
                  slotSuffix = "_legs";
               } else {
                  group = EquipmentSlotGroup.FEET;
                  slotSuffix = "_feet";
               }

               if (bonusArmor > 0.0F) {
                  event.addModifier(
                     Attributes.ARMOR,
                     new AttributeModifier(ResourceLocation.parse("dingdongji:paranoid_armor" + slotSuffix), (double)bonusArmor, Operation.ADD_VALUE),
                     group
                  );
               }

               if (bonusToughness > 0.0F) {
                  event.addModifier(
                     Attributes.ARMOR_TOUGHNESS,
                     new AttributeModifier(ResourceLocation.parse("dingdongji:paranoid_toughness" + slotSuffix), (double)bonusToughness, Operation.ADD_VALUE),
                     group
                  );
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onTooltip(ItemTooltipEvent event) {
      List<Component> list = event.getToolTip();
      ItemStack stack = event.getItemStack();
      Player player = event.getEntity();
      ArrayList<Component> descLines = new ArrayList<>();
      if (stack.has((DataComponentType)ModComponents.DEVOUR.get())) {
         DevourData data = (DevourData)stack.get((DataComponentType)ModComponents.DEVOUR.get());
         int kills = data.kills();
         if (kills < 0) {
            descLines.add(Component.literal("吞噬：击杀生物提升武器伤害").setStyle(DEVOUR_STYLE));
            descLines.add(Component.literal("击杀: ∞ | 额外伤害: +100000.0").setStyle(GRAY_STYLE));
         } else {
            float bonus = kills > 0 ? (float)Math.sqrt((double)kills) * 2.0F : 0.0F;
            descLines.add(Component.literal("吞噬：击杀生物提升武器伤害，加伤公式 f(k)=2√k").setStyle(DEVOUR_STYLE));
            descLines.add(Component.literal(String.format("击杀: %d | 额外伤害: +%.1f", kills, bonus)).setStyle(GRAY_STYLE));
         }
      }

      if (stack.has((DataComponentType)ModComponents.ACCUMULATE.get())) {
         AccumulateData data = (AccumulateData)stack.get((DataComponentType)ModComponents.ACCUMULATE.get());
         float bonus = (float)data.ticks() * 0.01F;
         descLines.add(Component.literal("迸发：在背包中持续积蓄能量，每秒积蓄0.2点伤害，攻击时加成减半").setStyle(ACCUMULATE_STYLE));
         int seconds = data.ticks() / 20;
         int hours = seconds / 3600;
         int mins = seconds % 3600 / 60;
         int secs = seconds % 60;
         String timeStr = hours > 0 ? String.format("%d:%02d:%02d", hours, mins, secs) : String.format("%d:%02d", mins, secs);
         descLines.add(Component.literal(String.format("积蓄: %s | 额外伤害: +%.1f", timeStr, bonus)).setStyle(GRAY_STYLE));
      }

      if (isAnyJiArmor(stack)) {
         descLines.add(Component.literal("穿着全套后提升玩家挖掘速度和方块交互距离").setStyle(JI_ARMOR_STYLE));
      }

      if (stack.has((DataComponentType)ModComponents.ROYAL_STEEL_AFFINITY.get())) {
         descLines.add(Component.literal("皇家亲和：持续恢复穿戴者的生命").setStyle(ROYAL_STEEL_STYLE));
      }

      if (stack.has((DataComponentType)ModComponents.COMFORTABLE.get())) {
         String key = ModKeyBindings.ABILITY_KEY.getTranslatedKeyMessage().getString();
         descLines.add(Component.literal(String.format("舒适：按 [%s] 键开关，行走时更加舒适便捷", key)).setStyle(ROYAL_STEEL_STYLE));
      }

      if (stack.has((DataComponentType)ModComponents.HEAT_INSULATION.get())) {
         descLines.add(Component.translatable("tooltip.dingdongji.heat_insulation").setStyle(EMBER_METAL_STYLE));
      }

      if (stack.has((DataComponentType)ModComponents.BARRIER_I.get())) {
         descLines.add(Component.literal("壁垒I：对大部分伤害明显减伤").setStyle(EMBER_METAL_STYLE));
      }

      if (stack.has((DataComponentType)ModComponents.EMBER_REGEN.get())) {
         descLines.add(Component.literal("浴火重生：持续恢复处于熔岩或火焰中佩戴者的生命，处于灵魂火时恢复效果翻倍").setStyle(EMBER_METAL_STYLE));
      }

      if (stack.has((DataComponentType)ModComponents.LAVA_WALKER.get())) {
         String key = ModKeyBindings.ABILITY_KEY.getTranslatedKeyMessage().getString();
         descLines.add(Component.translatable("tooltip.dingdongji.lava_walker", new Object[]{key}).setStyle(EMBER_METAL_STYLE));
      }

      if (stack.has((DataComponentType)ModComponents.GLOWING_VISION.get())) {
         String glowingKey = ModKeyBindings.GLOWING_VISION_KEY.getTranslatedKeyMessage().getString();
         descLines.add(Component.translatable("tooltip.dingdongji.glowing_vision", new Object[]{glowingKey}).setStyle(TRANSCENDIUM_STYLE));
      }

      if (stack.has((DataComponentType)ModComponents.NEUTRON_BARRIER.get())) {
         String neutronKey = ModKeyBindings.NEUTRON_BARRIER_KEY.getTranslatedKeyMessage().getString();
         descLines.add(Component.literal(String.format("中子屏罩：按 [%s] 切换清除飞向自身的弹射物与排斥靠近自身敌对生物的开关", neutronKey)).setStyle(TRANSCENDIUM_STYLE));
      }

      if (stack.has((DataComponentType)ModComponents.BARRIER_II.get())) {
         descLines.add(Component.translatable("tooltip.dingdongji.barrier_ii").setStyle(TRANSCENDIUM_STYLE));
      }

      if (stack.is((Item)ModItems.TRANSCENDIUM_BOOTS.get())) {
         String key = ModKeyBindings.ABILITY_KEY.getTranslatedKeyMessage().getString();
         descLines.add(Component.translatable("tooltip.dingdongji.stride_void_enhanced", new Object[]{key}).setStyle(TRANSCENDIUM_STYLE));
      }

      if (stack.is((Item)ModItems.TRANSCENDIUM_HELMET.get())
         || stack.is((Item)ModItems.TRANSCENDIUM_CHESTPLATE.get())
         || stack.is((Item)ModItems.TRANSCENDIUM_LEGGINGS.get())
         || stack.is((Item)ModItems.TRANSCENDIUM_BOOTS.get())) {
         descLines.add(Component.literal("偏执：根据已有魔咒的等级提升护甲值和盔甲韧性").setStyle(PARANOID_STYLE));
      }

      if (stack.has((DataComponentType)ModComponents.MEANINGLESS.get())) {
         descLines.add(Component.literal("无义：禁用所有魔咒并将其转换为护甲值和盔甲韧性").setStyle(FROST_STYLE));
         MeaninglessData mData = (MeaninglessData)stack.get((DataComponentType)ModComponents.MEANINGLESS_DATA.get());
         if (mData != null && !mData.convertedEnchantments().isEmpty()) {
            for (Holder<Enchantment> ench : mData.convertedEnchantments().keySet()) {
               int lvl = mData.convertedEnchantments().getLevel(ench);
               Enchantment var10000 = (Enchantment)ench.value();
               Component enchName = Enchantment.getFullname(ench, lvl);
               descLines.add(enchName.copy().setStyle(CONVERTED_ENCH_STYLE));
            }
         }

         ItemEnchantments ench = (ItemEnchantments)stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
         list.removeIf(line -> {
            for (Holder<Enchantment> holder : ench.keySet()) {
               int lvlx = ench.getLevel(holder);
               Enchantment var10000x = (Enchantment)holder.value();
               Component enchLine = Enchantment.getFullname(holder, lvlx);
               if (line.getString().equals(enchLine.getString())) {
                  return true;
               }
            }

            return false;
         });
      }

      if (stack.has((DataComponentType)ModComponents.FROST_WARD.get())) {
         descLines.add(Component.translatable("tooltip.dingdongji.frost_ward").setStyle(FROST_ABILITY_STYLE));
      }

      if (stack.has((DataComponentType)ModComponents.FROST_WALK.get())) {
         String frostKey = ModKeyBindings.FROST_SLIDE_KEY.getTranslatedKeyMessage().getString();
         descLines.add(Component.translatable("tooltip.dingdongji.frost_walk", new Object[]{frostKey}).setStyle(FROST_ABILITY_STYLE));
      }

      if (!descLines.isEmpty()) {
         // Anchor above the enchantment block instead of a hardcoded index:
         // enchantment lines (curses included, e.g. enchantment.minecraft.binding_curse)
         // are translatable lines whose key contains "enchantment", matching how
         // AnvilCraft locates the enchantment boundary. Fallback is right below
         // the item name when the item carries no enchantments.
         int insertAt = 1;
         for (int i = 1; i < list.size(); i++) {
            if (list.get(i).getContents() instanceof TranslatableContents tc && tc.getKey().contains("enchantment")) {
               insertAt = i;
               break;
            }
         }

         list.addAll(insertAt, descLines);
      }

      if (ModItems.isCreateTemplate(stack)) {
         list.add(Component.literal("自身不消耗").setStyle(CREATE_TEMPLATE_STYLE));
      }

      if (stack.has((DataComponentType)ModComponents.BARRIER_II.get()) && player != null && stack.is((Item)ModItems.TRANSCENDIUM_CHESTPLATE.get())) {
         long now = player.level().getGameTime();
         long lastHeal = ModArmorSetHandler.getChestHealCooldown(player.getUUID());
         long interval = 1200L;
         if (now < lastHeal + interval) {
            int remaining = (int)((lastHeal + interval - now) / 20L);
            list.add(Component.translatable("tooltip.dingdongji.barrier_ii.cooldown", new Object[]{remaining}).setStyle(GRAY_STYLE));
         } else {
            list.add(Component.translatable("tooltip.dingdongji.barrier_ii.ready").setStyle(Style.EMPTY.withColor(65280).withItalic(false)));
         }
      }
   }

   private static boolean isAnyJiArmor(ItemStack stack) {
      return stack.is((Item)ModItems.JI_HELMET.get())
         || stack.is((Item)ModItems.JI_CHESTPLATE.get())
         || stack.is((Item)ModItems.JI_LEGGINGS.get())
         || stack.is((Item)ModItems.JI_BOOTS.get());
   }
}
