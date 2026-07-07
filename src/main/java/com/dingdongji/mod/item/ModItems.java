package com.dingdongji.mod.item;

import com.dingdongji.mod.KryptonMod;
import com.dingdongji.mod.block.ModBlocks;
import com.dingdongji.mod.item.component.*;


import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(KryptonMod.MODID);

    // ===== 创造模板（普通 Item，通过 SmithingTemplateSlotMixin 允许放入锻造台模板槽）=====
    public static final DeferredItem<Item> CREATE_TEMPLATE =
            ITEMS.register("create_template",
                    () -> new com.dingdongji.mod.item.template.CreateTemplateItem(
                            new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()
                    )
            );

    public static final ResourceLocation CREATE_TEMPLATE_ID = ResourceLocation.fromNamespaceAndPath(KryptonMod.MODID, "create_template");

    public static boolean isCreateTemplate(ItemStack stack) {
        return !stack.isEmpty() && stack.is(CREATE_TEMPLATE.get());
    }

    // ===== 现有物品 =====

    public static final DeferredItem<BlockItem> JI_ANVIL =
            ITEMS.register("ji_anvil",
                    () -> new BlockItem(ModBlocks.JI_ANVIL.get(), new Item.Properties())
            );

    public static final DeferredItem<BlackHoleSwordItem> BLACK_HOLE_SWORD =
            ITEMS.register("black_hole_sword",
                    BlackHoleSwordItem::new
            );

    public static final DeferredItem<WhiteHoleSwordItem> WHITE_HOLE_SWORD =
            ITEMS.register("white_hole_sword",
                    () -> new WhiteHoleSwordItem(29)
            );

    // ===== 方块物品 =====

    public static final DeferredItem<BlockItem> KEJI_BLOCK =
            ITEMS.register("keji_block",
                    () -> new BlockItem(ModBlocks.KEJI_BLOCK.get(), new Item.Properties())
            );

    // ===== 新物品 =====

    /** 叽升级模板（SmithingTemplateItem 子类，自带锻造模板 tooltip 格式） */
    public static final DeferredItem<Item> JI_UPGRADE =
            ITEMS.register("ji_upgrade",
                    () -> new com.dingdongji.mod.item.template.JiUpgradeTemplateItem(
                            new Item.Properties()
                    )
            );

    // ===== 叽工具专用 Tier（耐久2333，效率14.0）=====
    public static final Tier JI_TOOL_TIER = new Tier() {
        @Override public int getUses() { return 2333; }
        @Override public float getSpeed() { return 14.0f; }
        @Override public float getAttackDamageBonus() { return 0f; }
        @Override public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_DIAMOND_TOOL; }
        @Override public int getEnchantmentValue() { return Tiers.GOLD.getEnchantmentValue(); }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(Items.GOLD_INGOT); }
    };

    /** 叽剑（伤害7，实体交互距离+2） */
    public static final DeferredItem<SwordItem> JI_SWORD =
            ITEMS.register("ji_sword",
                    () -> new SwordItem(
                            JI_TOOL_TIER,
                            new Item.Properties().attributes(createJiSwordAttributes())
                    )
            );

    /** 叽镐（效率+2，交互距离+2） */
    public static final DeferredItem<PickaxeItem> JI_PICKAXE =
            ITEMS.register("ji_pickaxe",
                    () -> new PickaxeItem(
                            JI_TOOL_TIER,
                            new Item.Properties().attributes(PickaxeItem.createAttributes(JI_TOOL_TIER, 1, -2.8f))
                    )
            );

    // ===== 盔甲：叽套 =====

    public static final DeferredItem<ArmorItem> JI_HELMET =
            ITEMS.register("ji_helmet",
                    () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.JI), ArmorItem.Type.HELMET,
                            new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(363)))
            );

    public static final DeferredItem<ArmorItem> JI_CHESTPLATE =
            ITEMS.register("ji_chestplate",
                    () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.JI), ArmorItem.Type.CHESTPLATE,
                            new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(363)))
            );

    public static final DeferredItem<ArmorItem> JI_LEGGINGS =
            ITEMS.register("ji_leggings",
                    () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.JI), ArmorItem.Type.LEGGINGS,
                            new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(363)))
            );

    public static final DeferredItem<ArmorItem> JI_BOOTS =
            ITEMS.register("ji_boots",
                    () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.JI), ArmorItem.Type.BOOTS,
                            new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(363)))
            );

    // ===== 盔甲：皇家钢套 =====

    public static final DeferredItem<ArmorItem> ROYAL_STEEL_HELMET =
            ITEMS.register("royal_steel_helmet",
                    () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.ROYAL_STEEL), ArmorItem.Type.HELMET,
                            new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(363)))
            );

    public static final DeferredItem<ArmorItem> ROYAL_STEEL_CHESTPLATE =
            ITEMS.register("royal_steel_chestplate",
                    () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.ROYAL_STEEL), ArmorItem.Type.CHESTPLATE,
                            new Item.Properties()
                                    .durability(ArmorItem.Type.CHESTPLATE.getDurability(363))
                                    .component(ModComponents.ROYAL_STEEL_AFFINITY.get(), RoyalSteelAffinityComponent.INSTANCE)
                    )
            );

    public static final DeferredItem<ArmorItem> ROYAL_STEEL_LEGGINGS =
            ITEMS.register("royal_steel_leggings",
                    () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.ROYAL_STEEL), ArmorItem.Type.LEGGINGS,
                            new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(363)))
            );

    public static final DeferredItem<ArmorItem> ROYAL_STEEL_BOOTS =
            ITEMS.register("royal_steel_boots",
                    () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.ROYAL_STEEL), ArmorItem.Type.BOOTS,
                            new Item.Properties()
                                    .durability(ArmorItem.Type.BOOTS.getDurability(363))
                                    .component(ModComponents.COMFORTABLE.get(), ComfortableComponent.INSTANCE)
                    )
            );

    // ===== 盔甲：余烬金属套 =====

    public static final DeferredItem<ArmorItem> EMBER_METAL_HELMET =
            ITEMS.register("ember_metal_helmet",
                    () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.EMBER_METAL), ArmorItem.Type.HELMET,
                            new Item.Properties()
                                    .durability(ArmorItem.Type.HELMET.getDurability(407))
                                    .fireResistant()
                                    .component(ModComponents.HEAT_INSULATION.get(), HeatInsulationComponent.INSTANCE)
                    )
            );

    public static final DeferredItem<ArmorItem> EMBER_METAL_CHESTPLATE =
            ITEMS.register("ember_metal_chestplate",
                    () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.EMBER_METAL), ArmorItem.Type.CHESTPLATE,
                            new Item.Properties()
                                    .durability(ArmorItem.Type.CHESTPLATE.getDurability(407))
                                    .fireResistant()
                                    .component(ModComponents.BARRIER_I.get(), BarrierIComponent.INSTANCE)
                    )
            );

    public static final DeferredItem<ArmorItem> EMBER_METAL_LEGGINGS =
            ITEMS.register("ember_metal_leggings",
                    () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.EMBER_METAL), ArmorItem.Type.LEGGINGS,
                            new Item.Properties()
                                    .durability(ArmorItem.Type.LEGGINGS.getDurability(407))
                                    .fireResistant()
                                    .component(ModComponents.EMBER_REGEN.get(), EmberRegenComponent.INSTANCE)
                    )
            );

    public static final DeferredItem<ArmorItem> EMBER_METAL_BOOTS =
            ITEMS.register("ember_metal_boots",
                    () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.EMBER_METAL), ArmorItem.Type.BOOTS,
                            new Item.Properties()
                                    .durability(ArmorItem.Type.BOOTS.getDurability(407))
                                    .fireResistant()
                                    .component(ModComponents.LAVA_WALKER.get(), LavaWalkerComponent.INSTANCE)
                    )
            );

    // ===== 盔甲：超限合金套 =====

    public static final DeferredItem<ArmorItem> TRANSCENDIUM_HELMET =
            ITEMS.register("transcendium_helmet",
                    () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.TRANSCENDIUM), ArmorItem.Type.HELMET,
                            new Item.Properties()
                                    .durability(ArmorItem.Type.HELMET.getDurability(407))
                                    .fireResistant()
                                    .component(ModComponents.GLOWING_VISION.get(), GlowingVisionComponent.DEFAULT)
                    )
            );

    public static final DeferredItem<ArmorItem> TRANSCENDIUM_CHESTPLATE =
            ITEMS.register("transcendium_chestplate",
                    () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.TRANSCENDIUM), ArmorItem.Type.CHESTPLATE,
                            new Item.Properties()
                                    .durability(ArmorItem.Type.CHESTPLATE.getDurability(407))
                                    .fireResistant()
                                    .component(ModComponents.BARRIER_II.get(), BarrierIIComponent.INSTANCE)
                    )
            );

    public static final DeferredItem<ArmorItem> TRANSCENDIUM_LEGGINGS =
            ITEMS.register("transcendium_leggings",
                    () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.TRANSCENDIUM), ArmorItem.Type.LEGGINGS,
                            new Item.Properties()
                                    .durability(ArmorItem.Type.LEGGINGS.getDurability(407))
                                    .fireResistant()
                                    .component(ModComponents.NEUTRON_BARRIER.get(), NeutronBarrierComponent.INSTANCE)
                    )
            );

    public static final DeferredItem<ArmorItem> TRANSCENDIUM_BOOTS =
            ITEMS.register("transcendium_boots",
                    () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.TRANSCENDIUM), ArmorItem.Type.BOOTS,
                            new Item.Properties()
                                    .durability(ArmorItem.Type.BOOTS.getDurability(407))
                                    .fireResistant()
                                    .component(ModComponents.STRIDE_VOID.get(), StrideVoidComponent.INSTANCE)
                    )
            );

    /** 叽剑属性：基础剑属性 + 实体交互距离+2 */
    private static ItemAttributeModifiers createJiSwordAttributes() {
        ItemAttributeModifiers base = SwordItem.createAttributes(JI_TOOL_TIER, 6, -2.4f);
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        base.modifiers().forEach(entry ->
                builder.add(entry.attribute(), entry.modifier(), entry.slot())
        );
        builder.add(
                Attributes.ENTITY_INTERACTION_RANGE,
                new AttributeModifier(
                        ResourceLocation.fromNamespaceAndPath(KryptonMod.MODID, "ji_sword_entity_range"),
                        2.0,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );
        return builder.build();
    }
}
