package com.dingdongji.mod.item.template;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.SmithingTemplateItem;

import java.util.List;

/**
 * 创造模板 — 继承 SmithingTemplateItem 以便放入锻造台模板槽。
 * 功能：可用于所有普通锻造台配方，自身不消耗。
 */
public class CreateTemplateItem extends SmithingTemplateItem {

    private static final Component APPLIES_TO = Component.translatable(
            "screen.dingdongji.create_template.applies_to")
            .withStyle(ChatFormatting.BLUE);
    private static final Component UPGRADE_INGREDIENTS = Component.translatable(
            "screen.dingdongji.create_template.upgrade_ingredients")
            .withStyle(ChatFormatting.BLUE);
    private static final Component UPGRADE = Component.translatable(
            "screen.dingdongji.create_template")
            .withStyle(ChatFormatting.GRAY);
    private static final Component BASE_SLOT_DESCRIPTION = Component.translatable(
            "screen.dingdongji.create_template.base_slot_description");
    private static final Component ADDITIONS_SLOT_DESCRIPTION = Component.translatable(
            "screen.dingdongji.create_template.additions_slot_description");

    private static final List<net.minecraft.resources.ResourceLocation> EMPTY_SLOT_TEXTURES = List.of();

    public CreateTemplateItem(Properties properties) {
        super(
                APPLIES_TO,
                UPGRADE_INGREDIENTS,
                UPGRADE,
                BASE_SLOT_DESCRIPTION,
                ADDITIONS_SLOT_DESCRIPTION,
                EMPTY_SLOT_TEXTURES,
                EMPTY_SLOT_TEXTURES
        );
    }
}
