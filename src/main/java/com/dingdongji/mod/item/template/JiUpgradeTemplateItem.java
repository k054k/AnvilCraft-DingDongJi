package com.dingdongji.mod.item.template;

import com.dingdongji.mod.KryptonMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.SmithingTemplateItem;

import java.util.List;

public class JiUpgradeTemplateItem extends SmithingTemplateItem {

    private static final ChatFormatting TITLE_FORMAT = ChatFormatting.GRAY;
    private static final ChatFormatting DESCRIPTION_FORMAT = ChatFormatting.BLUE;
    private static final Component APPLIES_TO = Component.translatable(
                    "screen.dingdongji.smithing_template.ji_upgrade.applies_to")
            .withStyle(DESCRIPTION_FORMAT);
    private static final Component UPGRADE_INGREDIENTS = Component.translatable(
                    "screen.dingdongji.smithing_template.ji_upgrade.upgrade_ingredients")
            .withStyle(DESCRIPTION_FORMAT);
    private static final Component UPGRADE = Component.translatable(
                    "screen.dingdongji.ji_upgrade")
            .withStyle(TITLE_FORMAT);
    private static final Component BASE_SLOT_DESCRIPTION = Component.translatable(
            "screen.dingdongji.smithing_template.ji_upgrade.base_slot_description");
    private static final Component ADDITIONS_SLOT_DESCRIPTION = Component.translatable(
            "screen.dingdongji.smithing_template.ji_upgrade.additions_slot_description");

    private static final ResourceLocation EMPTY_SLOT_SWORD =
            ResourceLocation.fromNamespaceAndPath("anvilcraft", "item/empty_slot_sword");
    private static final ResourceLocation EMPTY_SLOT_PICKAXE =
            ResourceLocation.fromNamespaceAndPath("anvilcraft", "item/empty_slot_pickaxe");
    private static final ResourceLocation EMPTY_SLOT_INGOT =
            ResourceLocation.fromNamespaceAndPath("anvilcraft", "item/empty_slot_ingot");

    public JiUpgradeTemplateItem(Properties properties) {
        super(
                APPLIES_TO,
                UPGRADE_INGREDIENTS,
                UPGRADE,
                BASE_SLOT_DESCRIPTION,
                ADDITIONS_SLOT_DESCRIPTION,
                List.of(EMPTY_SLOT_SWORD, EMPTY_SLOT_PICKAXE),
                List.of(EMPTY_SLOT_INGOT)
        );
    }

    @Override
    public String getDescriptionId() {
        return this.getOrCreateDescriptionId();
    }
}
