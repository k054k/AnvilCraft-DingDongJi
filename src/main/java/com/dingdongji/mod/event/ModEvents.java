package com.dingdongji.mod.event;

import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.platform.ClientViews;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.item.component.AccumulateData;
import com.dingdongji.mod.item.component.DevourData;
import com.dingdongji.mod.item.component.MeaninglessData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
   private static final Style SPECTRAL_SET_STYLE = Style.EMPTY.withColor(ChatFormatting.AQUA.getColor()).withItalic(false);
   private static final Style SPECTRAL_PHASE_STYLE = Style.EMPTY.withColor(0x7187F5).withItalic(false);

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
      if (stack.is((Item)ModItems.SMALL_POUCH.get())) {
         descLines.add(Component.translatable("tooltip.dingdongji.pouch", 6).setStyle(GRAY_STYLE));
         descLines.add(Component.translatable("tooltip.dingdongji.pouch.detach").setStyle(GRAY_STYLE));
      } else if (stack.is((Item)ModItems.BIG_POUCH.get())) {
         descLines.add(Component.translatable("tooltip.dingdongji.pouch", 12).setStyle(GRAY_STYLE));
         descLines.add(Component.translatable("tooltip.dingdongji.pouch.detach").setStyle(GRAY_STYLE));
      }

      // ===== 方块/材料说明行（全灰色）=====
      if (stack.is((Item)ModItems.HEAVY_GOLD_BLOCK.get())) {
         descLines.add(Component.translatable("tooltip.dingdongji.heavy_gold_block").setStyle(GRAY_STYLE));
      } else if (stack.is((Item)ModItems.CURSED_HEAVY_GOLD_BLOCK.get())) {
         descLines.add(Component.translatable("tooltip.dingdongji.cursed_heavy_gold_block").setStyle(GRAY_STYLE));
      } else if (stack.is((Item)ModItems.ENCHANTED_HEAVY_GOLD_BLOCK.get())) {
         descLines.add(Component.translatable("tooltip.dingdongji.enchanted_heavy_gold_block").setStyle(GRAY_STYLE));
      } else if (stack.is((Item)ModItems.ANCHORED_BEACON.get())) {
         descLines.add(Component.translatable("tooltip.dingdongji.anchored_beacon").setStyle(GRAY_STYLE));
      } else if (stack.is((Item)ModItems.SUPERCOOLED_FROST_METAL_BLOCK.get())) {
         descLines.add(Component.translatable("tooltip.dingdongji.supercooled_frost_metal_block").setStyle(GRAY_STYLE));
         descLines.add(Component.translatable("tooltip.dingdongji.supercooled_frost_metal_block.degrade").setStyle(GRAY_STYLE));
      } else if (stack.is((Item)ModItems.JI_INGOT.get())) {
         descLines.add(Component.translatable("tooltip.dingdongji.ji_ingot").setStyle(GRAY_STYLE));
      } else if (stack.is((Item)ModItems.KEJI_BLOCK.get())) {
         descLines.add(Component.translatable("tooltip.dingdongji.keji_block").setStyle(GRAY_STYLE));
      }

      if (stack.has((DataComponentType)ModComponents.DEVOUR.get())) {
         DevourData data = (DevourData)stack.get((DataComponentType)ModComponents.DEVOUR.get());
         int kills = data.kills();
         if (kills < 0) {
            descLines.add(Component.translatable("tooltip.dingdongji.devour.desc").setStyle(DEVOUR_STYLE));
            descLines.add(Component.translatable("tooltip.dingdongji.devour.detail", "∞", "100000.0").setStyle(GRAY_STYLE));
         } else {
            float bonus = kills > 0 ? (float)Math.sqrt((double)kills) * 2.0F : 0.0F;
            descLines.add(Component.translatable("tooltip.dingdongji.devour", "f(k)=2√k").setStyle(DEVOUR_STYLE));
            descLines.add(Component.translatable("tooltip.dingdongji.devour.detail", kills, String.format("%.1f", bonus)).setStyle(GRAY_STYLE));
         }
      }

      if (stack.has((DataComponentType)ModComponents.ACCUMULATE.get())) {
         AccumulateData data = (AccumulateData)stack.get((DataComponentType)ModComponents.ACCUMULATE.get());
         float bonus = (float)data.ticks() * 0.01F;
         descLines.add(Component.translatable("tooltip.dingdongji.accumulate").setStyle(ACCUMULATE_STYLE));
         int seconds = data.ticks() / 20;
         int hours = seconds / 3600;
         int mins = seconds % 3600 / 60;
         int secs = seconds % 60;
         String timeStr = hours > 0 ? String.format("%d:%02d:%02d", hours, mins, secs) : String.format("%d:%02d", mins, secs);
         descLines.add(Component.translatable("tooltip.dingdongji.accumulate.detail", timeStr, String.format("%.1f", bonus)).setStyle(GRAY_STYLE));
      }

      // ===== 完整套装行：单独收集，最后统一放到 descLines 末尾（跟随其他提示而非整个 tooltip 最底部） =====
      ArrayList<Component> setLines = new ArrayList<>();
      if (isAnyJiArmor(stack)) {
         setLines.add(Component.translatable("tooltip.dingdongji.ji_set", new Object[]{ClientViews.get().fullSetToggleKey()}).setStyle(SPECTRAL_SET_STYLE));
      }

      if (isAnyRoyalSteelArmor(stack)) {
         setLines.add(Component.translatable("tooltip.dingdongji.royal_set").setStyle(SPECTRAL_SET_STYLE));
      }

      if (isAnyEmberArmor(stack)) {
         setLines.add(Component.translatable("tooltip.dingdongji.ember_set").setStyle(SPECTRAL_SET_STYLE));
      }

      if (stack.is((Item)ModItems.TRANSCENDIUM_HELMET.get())
         || stack.is((Item)ModItems.TRANSCENDIUM_CHESTPLATE.get())
         || stack.is((Item)ModItems.TRANSCENDIUM_LEGGINGS.get())
         || stack.is((Item)ModItems.TRANSCENDIUM_BOOTS.get())) {
         String phaseKey = ClientViews.get().fullSetToggleKey();
         setLines.add(Component.translatable("tooltip.dingdongji.transcendium_set", new Object[]{phaseKey}).setStyle(SPECTRAL_SET_STYLE));
      }

      if (isAnyFrostArmor(stack)) {
         setLines.add(Component.translatable("tooltip.dingdongji.frost_set").setStyle(SPECTRAL_SET_STYLE));
      }

      if (stack.is((Item)ModItems.SPECTRAL_HELMET.get())
         || stack.is((Item)ModItems.SPECTRAL_CHESTPLATE.get())
         || stack.is((Item)ModItems.SPECTRAL_LEGGINGS.get())
         || stack.is((Item)ModItems.SPECTRAL_BOOTS.get())) {
         String phantomKey = ClientViews.get().fullSetToggleKey();
         setLines.add(Component.translatable("tooltip.dingdongji.spectral_set", new Object[]{phantomKey}).setStyle(SPECTRAL_SET_STYLE));
      }

      // ===== 部件能力行 =====
      if (stack.has((DataComponentType)ModComponents.COMFORTABLE.get())) {
         String key = ClientViews.get().abilityKey();
         descLines.add(Component.translatable("tooltip.dingdongji.comfortable", new Object[]{key, ClientViews.get().switchToolModeKey()}).setStyle(ROYAL_STEEL_STYLE));
      }

      if (stack.has((DataComponentType)ModComponents.HEAT_INSULATION.get())) {
         descLines.add(Component.translatable("tooltip.dingdongji.heat_insulation").setStyle(EMBER_METAL_STYLE));
      }

      if (stack.has((DataComponentType)ModComponents.LAVA_WALKER.get())) {
         String key = ClientViews.get().abilityKey();
         descLines.add(Component.translatable("tooltip.dingdongji.lava_walker", new Object[]{key, ClientViews.get().switchToolModeKey()}).setStyle(EMBER_METAL_STYLE));
      }

      if (stack.has((DataComponentType)ModComponents.GLOWING_VISION.get())) {
         String glowingKey = ClientViews.get().glowingVisionKey();
         descLines.add(Component.translatable("tooltip.dingdongji.glowing_vision", new Object[]{glowingKey, ClientViews.get().switchToolModeKey()}).setStyle(TRANSCENDIUM_STYLE));
      }

      if (stack.has((DataComponentType)ModComponents.NEUTRON_BARRIER.get())) {
         String neutronKey = ClientViews.get().neutronBarrierKey();
         descLines.add(Component.translatable("tooltip.dingdongji.neutron_barrier", neutronKey, ClientViews.get().switchToolModeKey()).setStyle(TRANSCENDIUM_STYLE));
      }

      if (stack.is((Item)ModItems.TRANSCENDIUM_BOOTS.get())) {
         String key = ClientViews.get().abilityKey();
         descLines.add(Component.translatable("tooltip.dingdongji.stride_void_enhanced", new Object[]{key, ClientViews.get().switchToolModeKey()}).setStyle(TRANSCENDIUM_STYLE));
      }

      if (stack.is((Item)ModItems.SQUAT_GENERATOR_LEGGINGS.get())) {
         descLines.add(Component.translatable("tooltip.dingdongji.squat_generator").setStyle(GRAY_STYLE));
      }

      if (stack.is((Item)ModItems.TRANSCENDIUM_HELMET.get())
         || stack.is((Item)ModItems.TRANSCENDIUM_CHESTPLATE.get())
         || stack.is((Item)ModItems.TRANSCENDIUM_LEGGINGS.get())
         || stack.is((Item)ModItems.TRANSCENDIUM_BOOTS.get())) {
         descLines.add(Component.translatable("tooltip.dingdongji.paranoid").setStyle(PARANOID_STYLE));
      }

      if (stack.has((DataComponentType)ModComponents.MEANINGLESS.get())) {
         descLines.add(Component.translatable("tooltip.dingdongji.meaningless").setStyle(FROST_STYLE));
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

      if (stack.has((DataComponentType)ModComponents.FROST_WALK.get())) {
         String frostKey = ClientViews.get().abilityKey();
         descLines.add(Component.translatable("tooltip.dingdongji.frost_walk", new Object[]{frostKey, ClientViews.get().switchToolModeKey()}).setStyle(FROST_ABILITY_STYLE));
      }

      if (stack.is((Item)ModItems.SPECTRAL_BOOTS.get())) {
         String phaseKey = ClientViews.get().abilityKey();
         descLines.add(Component.translatable("tooltip.dingdongji.spectral_phase", new Object[]{phaseKey, ClientViews.get().switchToolModeKey()}).setStyle(SPECTRAL_PHASE_STYLE));
      }

      // 套装行并入 descLines 末尾（跟随普通提示块，不再强制放 tooltip 最底）
      descLines.addAll(setLines);

      if (!descLines.isEmpty()) {
         // 全部追加到物品名称（第 0 行）正下方：描述/套装说明紧贴物品，
         // 与附魔、其他模组提示互不干扰。
         list.addAll(1, descLines);
      }

      if (ModItems.isCreateTemplate(stack)) {
         list.add(Component.translatable("tooltip.dingdongji.create_template").setStyle(CREATE_TEMPLATE_STYLE));
      }
   }

   private static boolean isAnyJiArmor(ItemStack stack) {
      return stack.is((Item)ModItems.JI_HELMET.get())
         || stack.is((Item)ModItems.JI_CHESTPLATE.get())
         || stack.is((Item)ModItems.JI_LEGGINGS.get())
         || stack.is((Item)ModItems.JI_BOOTS.get());
   }

   private static boolean isAnyEmberArmor(ItemStack stack) {
      return stack.is((Item)ModItems.EMBER_METAL_HELMET.get())
         || stack.is((Item)ModItems.EMBER_METAL_CHESTPLATE.get())
         || stack.is((Item)ModItems.EMBER_METAL_LEGGINGS.get())
         || stack.is((Item)ModItems.EMBER_METAL_BOOTS.get());
   }

   private static boolean isAnyFrostArmor(ItemStack stack) {
      return stack.is((Item)ModItems.FROST_METAL_HELMET.get())
         || stack.is((Item)ModItems.FROST_METAL_CHESTPLATE.get())
         || stack.is((Item)ModItems.FROST_METAL_LEGGINGS.get())
         || stack.is((Item)ModItems.FROST_METAL_BOOTS.get());
   }

   private static boolean isAnyRoyalSteelArmor(ItemStack stack) {
      return stack.is((Item)ModItems.ROYAL_STEEL_HELMET.get())
         || stack.is((Item)ModItems.ROYAL_STEEL_CHESTPLATE.get())
         || stack.is((Item)ModItems.ROYAL_STEEL_LEGGINGS.get())
         || stack.is((Item)ModItems.ROYAL_STEEL_BOOTS.get());
   }

   // 叽砧被玩家手动放置时额外播放鸡受伤音效（放置音效本身由 SoundType 提供鸡叫）
   @SubscribeEvent
   public static void onJiAnvilPlaced(net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent event) {
      if (!(event.getEntity() instanceof Player)) {
         return;
      }
      if (!event.getPlacedBlock().is((net.minecraft.world.level.block.Block)com.dingdongji.mod.block.ModBlocks.JI_ANVIL.get())) {
         return;
      }
      if (!(event.getLevel() instanceof net.minecraft.world.level.Level level)) {
         return;
      }
      net.minecraft.core.BlockPos pos = event.getPos();
      level.playSound(
         null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
         net.minecraft.sounds.SoundEvents.CHICKEN_HURT, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F
      );
   }

   // 锁定期间右键该方块：直接取消（时定配方方块均为温度方块，本身无右键交互）。
      // 挖掘不被拦截（锁定=光柱上方块的方块状态不变化，破坏属于允许的"消失"）。
      // 玩家拿着方块对着锁定方块的侧面放置旁边方块时不取消（否则旁边方块会回弹）。
      @SubscribeEvent
      public static void onBeamLockedBlockUse(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
         if (event.getLevel().isClientSide) {
            return;
         }
         net.minecraft.world.level.Level level = event.getLevel();
         BlockPos pos = event.getPos();
         if (!com.dingdongji.mod.util.BeamLockTracker.isLocked(level, pos)) {
            return;
         }
         // 手持方块物品时在锁定方块旁边放置新方块：不拦截，允许正常放置
         if (event.getItemStack().getItem() instanceof net.minecraft.world.item.BlockItem) {
            return;
         }
         event.setCanceled(true);
      }

   // 服务器 tick：转化粒子 + 时定粒子；每 20 tick 兜底清理过期锁定
   @SubscribeEvent
   public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
      com.dingdongji.mod.util.BeaconConversionFx.tick(event.getServer());
      if (event.getServer().getTickCount() % 20L == 0L) {
         com.dingdongji.mod.util.BeamLockTracker.sweepExpired(event.getServer());
      }
      // 被时定方块周身的附魔符文吸入粒子
      com.dingdongji.mod.util.BeamLockTracker.tickParticles(event.getServer());
      // 铭化信标注魔（光柱内停留物品的附魔叠加与祛咒）
      com.dingdongji.mod.event.BeaconInfusionManager.tick(event.getServer());
   }

   // 服务器关闭/退出世界：清空时定锁定静态状态，避免换世界串档
   @SubscribeEvent
   public static void onServerStopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
      com.dingdongji.mod.util.BeamLockTracker.resetAll();
      com.dingdongji.mod.util.BeaconConversionFx.resetAll();
      com.dingdongji.mod.event.BeaconInfusionManager.resetAll();
   }
}
