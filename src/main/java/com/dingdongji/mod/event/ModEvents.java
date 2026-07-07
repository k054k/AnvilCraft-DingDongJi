package com.dingdongji.mod.event;

import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.item.component.AccumulateData;
import com.dingdongji.mod.item.component.DevourData;
import com.dingdongji.mod.input.ModKeyBindings;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

public class ModEvents {

    private static final ResourceLocation DEVOUR_DAMAGE_ID = ResourceLocation.parse("dingdongji:devour_damage");
    private static final ResourceLocation BURST_DAMAGE_ID = ResourceLocation.parse("dingdongji:burst_damage");
    private static final ResourceLocation JI_SWEEP_ID = ResourceLocation.parse("dingdongji:ji_sword_sweep");
    private static final ResourceLocation JI_REACH_ID = ResourceLocation.parse("dingdongji:ji_pickaxe_reach");
    private static final ResourceLocation JI_BREAK_SPEED_ID = ResourceLocation.parse("dingdongji:ji_pickaxe_break_speed");

    private static final Style DEVOUR_STYLE = Style.EMPTY.withColor(0xF7BE00);
    private static final Style ACCUMULATE_STYLE = Style.EMPTY.withColor(0xD2FFFB);
    private static final Style GRAY_STYLE = Style.EMPTY.withColor(0xAAAAAA);
    private static final Style CREATE_TEMPLATE_STYLE = Style.EMPTY.withColor(0xAA00FF);

    // ===== 盔甲 tooltip 颜色 =====
    private static final Style JI_ARMOR_STYLE = Style.EMPTY.withColor(0xFFFFB0);
    private static final Style ROYAL_STEEL_STYLE = Style.EMPTY.withColor(0xB7F2D4);
    private static final Style EMBER_METAL_STYLE = Style.EMPTY.withColor(0xFF4500);
    private static final Style TRANSCENDIUM_STYLE = Style.EMPTY.withColor(0xBA55D3);

    // ===== 吞噬：击杀计数 =====
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        DamageSource source = event.getSource();
        if (source.getEntity() instanceof Player player) {
            ItemStack weapon = player.getMainHandItem();
            if (weapon.has(ModComponents.DEVOUR.get())) {
                DevourData data = weapon.get(ModComponents.DEVOUR.get());
                weapon.set(ModComponents.DEVOUR.get(), new DevourData(data.kills() + 1));
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
            }
        }

        if (stack.has(ModComponents.ACCUMULATE.get())) {
            AccumulateData data = stack.get(ModComponents.ACCUMULATE.get());
            float bonus = data.ticks() * 0.01f;
            if (bonus > 0) {
                event.addModifier(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BURST_DAMAGE_ID, bonus, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND
                );
            }
        }

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
    }

    // ===== Tooltip =====
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> list = event.getToolTip();

        // 吞噬（#F7BE00）
        if (stack.has(ModComponents.DEVOUR.get())) {
            DevourData data = stack.get(ModComponents.DEVOUR.get());
            int kills = data.kills();
            float bonus = kills > 0 ? (float) Math.sqrt(kills) * 2.0f : 0;
            list.add(1, Component.literal(
                    "吞噬：击杀生物提升武器伤害，加伤公式 f(k)=2√k"
            ).setStyle(DEVOUR_STYLE));
            list.add(Component.literal(
                    "击杀: " + kills + " | 额外伤害: +" + String.format("%.1f", bonus)
            ).setStyle(GRAY_STYLE));
        }

        // 迸发（#D2FFFB）
        if (stack.has(ModComponents.ACCUMULATE.get())) {
            AccumulateData data = stack.get(ModComponents.ACCUMULATE.get());
            float bonus = data.ticks() * 0.01f;
            String desc = "迸发：在背包中持续积蓄能量，每秒积蓄0.2点伤害，攻击时加成减半";
            list.add(1, Component.literal(desc).setStyle(ACCUMULATE_STYLE));
            list.add(Component.literal(
                    "积蓄: " + formatTicks(data.ticks()) + " | 额外伤害: +" + String.format("%.1f", bonus)
            ).setStyle(GRAY_STYLE));
        }

        // ===== 创造模板 tooltip =====
        if (ModItems.isCreateTemplate(stack)) {
            list.add(Component.literal("自身不消耗").setStyle(CREATE_TEMPLATE_STYLE));
        }

        // ===== 盔甲组件 tooltip（置于名称下方）=====

        // --- 叽套 ---
        if (isAnyJiArmor(stack)) {
            list.add(1, Component.literal(
                    "穿着全套后提升玩家挖掘速度和方块交互距离"
            ).setStyle(JI_ARMOR_STYLE));
        }

        // --- 皇家钢靴子：舒适 ---
        if (stack.has(ModComponents.COMFORTABLE.get())) {
            list.add(1, Component.literal(
                    "舒适：行走更加便捷舒适"
            ).setStyle(ROYAL_STEEL_STYLE));
        }

        // --- 皇家钢胸甲：皇家亲和 ---
        if (stack.has(ModComponents.ROYAL_STEEL_AFFINITY.get())) {
            list.add(1, Component.literal(
                    "皇家亲和：持续恢复穿戴者的生命"
            ).setStyle(ROYAL_STEEL_STYLE));
        }

        // --- 余烬头盔：隔热 ---
        if (stack.has(ModComponents.HEAT_INSULATION.get())) {
            list.add(1, Component.literal(
                    "隔热：隔绝高温环境带来的灼烧"
            ).setStyle(EMBER_METAL_STYLE));
        }
        // --- 余烬胸甲：壁垒 I ---
        if (stack.has(ModComponents.BARRIER_I.get())) {
            list.add(1, Component.literal(
                    "壁垒I：对大部分伤害明显减伤"
            ).setStyle(EMBER_METAL_STYLE));
        }
        // --- 余烬护腿：浴火重生 ---
        if (stack.has(ModComponents.EMBER_REGEN.get())) {
            list.add(1, Component.literal(
                    "浴火重生：持续恢复处于熔岩或火焰中佩戴者的生命，处于灵魂火时恢复效果翻倍"
            ).setStyle(EMBER_METAL_STYLE));
        }
        // --- 余烬靴子：蹈火 ---
        if (stack.has(ModComponents.LAVA_WALKER.get())) {
            String abilityKey = ModKeyBindings.ABILITY_KEY.getTranslatedKeyMessage().getString();
            list.add(1, Component.literal(
                    "蹈火：按 [" + abilityKey + "] 切换，行走自带火焰效果"
            ).setStyle(EMBER_METAL_STYLE));
        }

        // --- 超限合金头盔：适应 ---
        if (stack.has(ModComponents.GLOWING_VISION.get())) {
            list.add(1, Component.literal(
                    "适应：适应黑暗，高温，水下环境；高亮最近的敌对生物"
            ).setStyle(TRANSCENDIUM_STYLE));
        }
        // --- 超限合金护腿：中子屏罩 ---
        if (stack.has(ModComponents.NEUTRON_BARRIER.get())) {
            list.add(1, Component.literal(
                    "中子屏罩：清除靠近自身的弹射物，弹飞靠近自身的高威胁生物"
            ).setStyle(TRANSCENDIUM_STYLE));
        }
        // --- 超限合金胸甲：壁垒 II ---
        if (stack.has(ModComponents.BARRIER_II.get())) {
            list.add(1, Component.literal(
                    "壁垒II：对大部分伤害大幅减伤，无视魔法伤害，虚空伤害，接触伤害，爆炸伤害；生命值低时紧急治愈穿戴者，触发时清除所有负面效果"
            ).setStyle(TRANSCENDIUM_STYLE));
            // 冷却倒计时
            Player player = event.getEntity();
            if (player != null) {
                long now = player.level().getGameTime();
                long lastHeal = ModArmorSetHandler.CHEST_HEAL_COOLDOWN.getOrDefault(player.getUUID(), 0L);
                long elapsed = now - lastHeal;
                long remaining = ModArmorSetHandler.CHEST_HEAL_INTERVAL - elapsed;
                if (remaining > 0 && lastHeal > 0) {
                    int seconds = (int) (remaining / 20);
                    list.add(Component.literal(
                            "应急治愈冷却中：" + seconds + "秒"
                    ).withStyle(ChatFormatting.RED));
                } else {
                    list.add(Component.literal(
                            "应急治愈就绪"
                    ).withStyle(ChatFormatting.GREEN));
                }
            }
        }
        // --- 超限合金靴子：蹈虚 ---
        if (stack.has(ModComponents.STRIDE_VOID.get())) {
            String abilityKey = ModKeyBindings.ABILITY_KEY.getTranslatedKeyMessage().getString();
            list.add(1, Component.literal(
                    "蹈虚：按 [" + abilityKey + "] 切换重力模式"
            ).setStyle(TRANSCENDIUM_STYLE));
        }
    }

    private static boolean isAnyJiArmor(ItemStack stack) {
        return stack.is(ModItems.JI_HELMET.get())
                || stack.is(ModItems.JI_CHESTPLATE.get())
                || stack.is(ModItems.JI_LEGGINGS.get())
                || stack.is(ModItems.JI_BOOTS.get());
    }

    private static String formatTicks(int ticks) {
        int seconds = ticks / 20;
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("小时");
        if (minutes > 0) sb.append(minutes).append("分");
        sb.append(secs).append("秒");
        return sb.toString();
    }
}
