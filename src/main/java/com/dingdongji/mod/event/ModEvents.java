package com.dingdongji.mod.event;

import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.item.component.AccumulateData;
import com.dingdongji.mod.item.component.DevourData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;

import java.util.List;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public class ModEvents {

    private static final ResourceLocation DEVOUR_DAMAGE_ID = ResourceLocation.parse("dingdongji:devour_damage");
    private static final ResourceLocation JI_SWEEP_ID = ResourceLocation.parse("dingdongji:ji_sword_sweep");
    private static final ResourceLocation JI_REACH_ID = ResourceLocation.parse("dingdongji:ji_pickaxe_reach");
    private static final ResourceLocation JI_BREAK_SPEED_ID = ResourceLocation.parse("dingdongji:ji_pickaxe_break_speed");

    // ===== 吞噬：击杀计数 =====
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        DamageSource source = event.getSource();
        if (source.getEntity() instanceof Player player) {
            ItemStack weapon = player.getMainHandItem();
            if (weapon.has(ModComponents.DEVOUR.get())) {
                DevourData data = weapon.get(ModComponents.DEVOUR.get());
                int kills = data.kills();
                // 击杀数溢出为负数后不再增加，显示 ∞ 且增伤固定 100000
                if (kills >= 0) {
                    weapon.set(ModComponents.DEVOUR.get(), new DevourData(kills + 1));
                }
            }
        }
    }

    // ===== 属性修饰器 =====
    @SubscribeEvent
    public static void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();

        if (stack.has(ModComponents.DEVOUR.get())) {
            DevourData data = stack.get(ModComponents.DEVOUR.get());
            int kills = data.kills();
            if (kills > 0) {
                float bonus = (float) Math.sqrt(kills) * 2.0f;
                event.addModifier(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(DEVOUR_DAMAGE_ID, bonus, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND
                );
            } else if (kills < 0) {
                // 溢出：固定增伤 100000
                event.addModifier(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(DEVOUR_DAMAGE_ID, 100000.0f, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND
                );
            }
        }

        // 积蓄伤害由 WhiteHoleSwordItem.hurtEnemy 通过 setHealth 直接扣血（绕过限伤），
        // 不加到攻击力属性上，避免双重应用

        // 叽剑：横扫伤害 +1
        if (stack.is(ModItems.JI_SWORD.get())) {
            event.addModifier(
                    Attributes.SWEEPING_DAMAGE_RATIO,
                    new AttributeModifier(JI_SWEEP_ID, 1.0, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND
            );
        }

        // 叽镐：方块交互距离 +2，方块破坏速度 +1
        if (stack.is(ModItems.JI_PICKAXE.get())) {
            event.addModifier(
                    Attributes.BLOCK_INTERACTION_RANGE,
                    new AttributeModifier(JI_REACH_ID, 2.0, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND
            );
            event.addModifier(
                    Attributes.BLOCK_BREAK_SPEED,
                    new AttributeModifier(JI_BREAK_SPEED_ID, 1.0, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND
            );
        }

        // 偏执：超限合金套根据附魔提升护甲值和盔甲韧性
        if (stack.is(ModItems.TRANSCENDIUM_HELMET.get()) || stack.is(ModItems.TRANSCENDIUM_CHESTPLATE.get())
                || stack.is(ModItems.TRANSCENDIUM_LEGGINGS.get()) || stack.is(ModItems.TRANSCENDIUM_BOOTS.get())) {
            ItemEnchantments ench = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            if (!ench.isEmpty()) {
                int levels = 0;
                for (Holder<Enchantment> e : ench.keySet()) {
                    levels += ench.getLevel(e);
                }
                if (levels > 0) {
                    float bonusArmor = Math.round((float) (Math.sqrt(levels) * 1.5 + levels / 5.0));
                    float bonusToughness = levels / 3.0f;
                    // 根据部位确定 EquipmentSlotGroup 和 modifier 后缀（保证 ID 唯一性）
                    EquipmentSlotGroup group;
                    String slotSuffix;
                    if (stack.is(ModItems.TRANSCENDIUM_HELMET.get())) {
                        group = EquipmentSlotGroup.HEAD;
                        slotSuffix = "_head";
                    } else if (stack.is(ModItems.TRANSCENDIUM_CHESTPLATE.get())) {
                        group = EquipmentSlotGroup.CHEST;
                        slotSuffix = "_chest";
                    } else if (stack.is(ModItems.TRANSCENDIUM_LEGGINGS.get())) {
                        group = EquipmentSlotGroup.LEGS;
                        slotSuffix = "_legs";
                    } else {
                        group = EquipmentSlotGroup.FEET;
                        slotSuffix = "_feet";
                    }
                    if (bonusArmor > 0) {
                        event.addModifier(Attributes.ARMOR,
                                new AttributeModifier(
                                        ResourceLocation.parse("dingdongji:paranoid_armor" + slotSuffix),
                                        bonusArmor, AttributeModifier.Operation.ADD_VALUE),
                                group);
                    }
                    if (bonusToughness > 0) {
                        event.addModifier(Attributes.ARMOR_TOUGHNESS,
                                new AttributeModifier(
                                        ResourceLocation.parse("dingdongji:paranoid_toughness" + slotSuffix),
                                        bonusToughness, AttributeModifier.Operation.ADD_VALUE),
                                group);
                    }
                }
            }
        }
    }

    // ===== Tooltip（list.addAll(1) 在附魔后、属性前插入组件描述）=====
    private static final net.minecraft.network.chat.Style DEVOUR_STYLE = net.minecraft.network.chat.Style.EMPTY.withColor(0xF7BE00).withItalic(false);
    private static final net.minecraft.network.chat.Style ACCUMULATE_STYLE = net.minecraft.network.chat.Style.EMPTY.withColor(0xD2FFFB).withItalic(false);
    private static final net.minecraft.network.chat.Style ROYAL_STEEL_STYLE = net.minecraft.network.chat.Style.EMPTY.withColor(0xB7F2D4).withItalic(false);
    private static final net.minecraft.network.chat.Style JI_ARMOR_STYLE = net.minecraft.network.chat.Style.EMPTY.withColor(0xFFFFB0).withItalic(false);
    private static final net.minecraft.network.chat.Style EMBER_METAL_STYLE = net.minecraft.network.chat.Style.EMPTY.withColor(0xFF4500).withItalic(false);
    private static final net.minecraft.network.chat.Style TRANSCENDIUM_STYLE = net.minecraft.network.chat.Style.EMPTY.withColor(0xBA55D3).withItalic(false);
    private static final net.minecraft.network.chat.Style FROST_STYLE = net.minecraft.network.chat.Style.EMPTY.withColor(0x9DD8FF).withItalic(false);
    private static final net.minecraft.network.chat.Style CONVERTED_ENCH_STYLE = net.minecraft.network.chat.Style.EMPTY.withColor(0x5F93A3).withItalic(false);
    private static final net.minecraft.network.chat.Style GRAY_STYLE = net.minecraft.network.chat.Style.EMPTY.withColor(0xAAAAAA).withItalic(false);
    private static final net.minecraft.network.chat.Style CREATE_TEMPLATE_STYLE = net.minecraft.network.chat.Style.EMPTY.withColor(0xAA00FF).withItalic(false);
    private static final net.minecraft.network.chat.Style PARANOID_STYLE = net.minecraft.network.chat.Style.EMPTY.withColor(0xAA0000).withItalic(false);

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        List<Component> list = event.getToolTip();
        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();

        // ===== 构建组件描述列表（将在附魔之后、属性之前插入）=====
        java.util.ArrayList<Component> descLines = new java.util.ArrayList<>();

        // 吞噬
        if (stack.has(ModComponents.DEVOUR.get())) {
            DevourData data = stack.get(ModComponents.DEVOUR.get());
            int kills = data.kills();
            if (kills < 0) {
                // 溢出状态：显示 ∞，增伤固定 100000
                descLines.add(Component.literal("吞噬：击杀生物提升武器伤害").setStyle(DEVOUR_STYLE));
                descLines.add(Component.literal("击杀: ∞ | 额外伤害: +100000.0").setStyle(GRAY_STYLE));
            } else {
                float bonus = kills > 0 ? (float) Math.sqrt(kills) * 2.0f : 0;
                descLines.add(Component.literal("吞噬：击杀生物提升武器伤害，加伤公式 f(k)=2√k").setStyle(DEVOUR_STYLE));
                descLines.add(Component.literal(String.format("击杀: %d | 额外伤害: +%.1f", kills, bonus)).setStyle(GRAY_STYLE));
            }
        }

        // 迸发
        if (stack.has(ModComponents.ACCUMULATE.get())) {
            AccumulateData data = stack.get(ModComponents.ACCUMULATE.get());
            float bonus = data.ticks() * 0.01f;
            descLines.add(Component.literal("迸发：在背包中持续积蓄能量，每秒积蓄0.2点伤害，攻击时加成减半").setStyle(ACCUMULATE_STYLE));
            int seconds = data.ticks() / 20;
            int hours = seconds / 3600;
            int mins = (seconds % 3600) / 60;
            int secs = seconds % 60;
            String timeStr = hours > 0
                    ? String.format("%d:%02d:%02d", hours, mins, secs)
                    : String.format("%d:%02d", mins, secs);
            descLines.add(Component.literal(String.format("积蓄: %s | 额外伤害: +%.1f", timeStr, bonus)).setStyle(GRAY_STYLE));
        }

        // 叽套
        if (isAnyJiArmor(stack)) {
            descLines.add(Component.literal("穿着全套后提升玩家挖掘速度和方块交互距离").setStyle(JI_ARMOR_STYLE));
        }

        // 皇家钢
        if (stack.has(ModComponents.ROYAL_STEEL_AFFINITY.get())) {
            descLines.add(Component.literal("皇家亲和：持续恢复穿戴者的生命").setStyle(ROYAL_STEEL_STYLE));
        }
        if (stack.has(ModComponents.COMFORTABLE.get())) {
            String key = com.dingdongji.mod.input.ModKeyBindings.ABILITY_KEY.getTranslatedKeyMessage().getString();
            descLines.add(Component.literal(String.format("舒适：按 [%s] 键开关，行走时更加舒适便捷", key)).setStyle(ROYAL_STEEL_STYLE));
        }

        // 余烬
        if (stack.has(ModComponents.HEAT_INSULATION.get())) {
            descLines.add(Component.literal("赴汤：隔绝高温环境带来的灼烧").setStyle(EMBER_METAL_STYLE));
        }
        if (stack.has(ModComponents.BARRIER_I.get())) {
            descLines.add(Component.literal("壁垒I：对大部分伤害明显减伤").setStyle(EMBER_METAL_STYLE));
        }
        if (stack.has(ModComponents.EMBER_REGEN.get())) {
            descLines.add(Component.literal("浴火重生：持续恢复处于熔岩或火焰中佩戴者的生命，处于灵魂火时恢复效果翻倍").setStyle(EMBER_METAL_STYLE));
        }
        if (stack.has(ModComponents.LAVA_WALKER.get())) {
            String key = com.dingdongji.mod.input.ModKeyBindings.ABILITY_KEY.getTranslatedKeyMessage().getString();
            descLines.add(Component.literal(String.format("蹈火：按 [%s] 切换，行走自带火焰效果", key)).setStyle(EMBER_METAL_STYLE));
        }

        // 超限
        if (stack.has(ModComponents.GLOWING_VISION.get())) {
            String glowingKey = com.dingdongji.mod.input.ModKeyBindings.GLOWING_VISION_KEY.getTranslatedKeyMessage().getString();
            descLines.add(Component.literal(String.format("适应：按 [%s] 键切换高亮敌对生物和夜视；适应黑暗，水下，高温环境", glowingKey)).setStyle(TRANSCENDIUM_STYLE));
        }
        if (stack.has(ModComponents.NEUTRON_BARRIER.get())) {
            String neutronKey = com.dingdongji.mod.input.ModKeyBindings.NEUTRON_BARRIER_KEY.getTranslatedKeyMessage().getString();
            descLines.add(Component.literal(String.format("中子屏罩：按 [%s] 切换清除飞向自身的弹射物与排斥靠近自身敌对生物的开关", neutronKey)).setStyle(TRANSCENDIUM_STYLE));
        }
        if (stack.has(ModComponents.BARRIER_II.get())) {
            descLines.add(Component.literal("壁垒II：对大部分伤害大幅减伤，无视魔法伤害，虚空伤害，接触伤害，爆炸伤害；生命值低时紧急治愈穿戴者，触发时清除所有负面效果").setStyle(TRANSCENDIUM_STYLE));
        }
        if (stack.is(ModItems.TRANSCENDIUM_BOOTS.get())) {
            String key = com.dingdongji.mod.input.ModKeyBindings.ABILITY_KEY.getTranslatedKeyMessage().getString();
            descLines.add(Component.literal(String.format("蹈虚：穿戴后即可创造飞行，按 [%s] 可开关；与飘升机同时穿戴时提升飞行速度", key)).setStyle(TRANSCENDIUM_STYLE));
        }

        // 偏执
        if (stack.is(ModItems.TRANSCENDIUM_HELMET.get()) || stack.is(ModItems.TRANSCENDIUM_CHESTPLATE.get())
                || stack.is(ModItems.TRANSCENDIUM_LEGGINGS.get()) || stack.is(ModItems.TRANSCENDIUM_BOOTS.get())) {
            descLines.add(Component.literal("偏执：根据已有魔咒的等级提升护甲值和盔甲韧性").setStyle(PARANOID_STYLE));
        }

        // 浮霜无义
        if (stack.has(ModComponents.MEANINGLESS.get())) {
            descLines.add(Component.literal("无义：禁用所有魔咒并将其转换为护甲值和盔甲韧性").setStyle(FROST_STYLE));
            com.dingdongji.mod.item.component.MeaninglessData mData = stack.get(ModComponents.MEANINGLESS_DATA.get());
            if (mData != null && !mData.convertedEnchantments().isEmpty()) {
                for (var ench : mData.convertedEnchantments().keySet()) {
                    int lvl = mData.convertedEnchantments().getLevel(ench);
                    Component enchName = ench.value().getFullname(ench, lvl);
                    descLines.add(enchName.copy().setStyle(CONVERTED_ENCH_STYLE));
                }
            }
            // 移除原版附魔 tooltip 行（无义已禁用它们，避免重复显示）
            ItemEnchantments ench = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            list.removeIf(line -> {
                for (Holder<net.minecraft.world.item.enchantment.Enchantment> holder : ench.keySet()) {
                    int lvl = ench.getLevel(holder);
                    Component enchLine = holder.value().getFullname(holder, lvl);
                    if (line.getString().equals(enchLine.getString())) {
                        return true;
                    }
                }
                return false;
            });
        }

        // 在附魔之后、属性之前插入
        if (!descLines.isEmpty()) {
            list.addAll(1, descLines);
        }

        // 创造模板的自身不消耗（最底部）
        if (ModItems.isCreateTemplate(stack)) {
            list.add(Component.literal("自身不消耗").setStyle(CREATE_TEMPLATE_STYLE));
        }

        // 应急治愈状态（最底部）
        if (stack.has(ModComponents.BARRIER_II.get()) && player != null && stack.is(ModItems.TRANSCENDIUM_CHESTPLATE.get())) {
            long now = player.level().getGameTime();
            long lastHeal = com.dingdongji.mod.event.ModArmorSetHandler.getChestHealCooldown(player.getUUID());
            long interval = com.dingdongji.mod.event.ModArmorSetHandler.CHEST_HEAL_INTERVAL;
            if (now < lastHeal + interval) {
                int remaining = (int) ((lastHeal + interval - now) / 20);
                list.add(Component.literal(String.format("应急治愈冷却中：%d秒", remaining)).setStyle(GRAY_STYLE));
            } else {
                list.add(Component.literal("应急治愈就绪").setStyle(net.minecraft.network.chat.Style.EMPTY.withColor(0x00FF00).withItalic(false)));
            }
        }
    }

    private static boolean isAnyJiArmor(ItemStack stack) {
        return stack.is(ModItems.JI_HELMET.get()) || stack.is(ModItems.JI_CHESTPLATE.get())
            || stack.is(ModItems.JI_LEGGINGS.get()) || stack.is(ModItems.JI_BOOTS.get());
    }
}

