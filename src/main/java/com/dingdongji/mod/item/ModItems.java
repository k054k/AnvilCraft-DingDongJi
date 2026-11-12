package com.dingdongji.mod.item;

import com.dingdongji.mod.block.ModBlocks;
import com.dingdongji.mod.item.component.BarrierIComponent;
import com.dingdongji.mod.item.component.BarrierIIComponent;
import com.dingdongji.mod.item.component.ComfortableComponent;
import com.dingdongji.mod.item.component.CreateTemplateMode;
import com.dingdongji.mod.item.component.EmberRegenComponent;
import com.dingdongji.mod.item.component.FrostWalkComponent;
import com.dingdongji.mod.item.component.FrostWardComponent;
import com.dingdongji.mod.item.component.GlowingVisionComponent;
import com.dingdongji.mod.item.component.HeatInsulationComponent;
import com.dingdongji.mod.item.component.LavaWalkerComponent;
import com.dingdongji.mod.item.component.MeaninglessComponent;
import com.dingdongji.mod.item.component.NeutronBarrierComponent;
import com.dingdongji.mod.item.component.RoyalSteelAffinityComponent;
import com.dingdongji.mod.item.template.CreateTemplateItem;
import com.dingdongji.mod.item.template.JiUpgradeTemplateItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.component.ItemAttributeModifiers.Builder;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Items;

public class ModItems {
   public static final Items ITEMS = DeferredRegister.createItems("dingdongji");
   public static final DeferredItem<Item> CREATE_TEMPLATE = ITEMS.register(
      "create_template",
      () -> new CreateTemplateItem(
            new Properties()
               .stacksTo(1)
               .rarity(Rarity.EPIC)
               .fireResistant()
               .component((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get(), CreateTemplateMode.DEFAULT)
         )
   );
   public static final ResourceLocation CREATE_TEMPLATE_ID = ResourceLocation.fromNamespaceAndPath("dingdongji", "create_template");
   public static final DeferredItem<BlockItem> JI_ANVIL = ITEMS.register("ji_anvil", () -> new BlockItem((Block)ModBlocks.JI_ANVIL.get(), new Properties()));
   public static final DeferredItem<BlackHoleSwordItem> BLACK_HOLE_SWORD = ITEMS.register("black_hole_sword", BlackHoleSwordItem::new);
   public static final DeferredItem<WhiteHoleSwordItem> WHITE_HOLE_SWORD = ITEMS.register("white_hole_sword", () -> new WhiteHoleSwordItem(29));
   public static final DeferredItem<BlockItem> KEJI_BLOCK = ITEMS.register(
      "keji_block", () -> new BlockItem((Block)ModBlocks.KEJI_BLOCK.get(), new Properties())
   );
   public static final DeferredItem<BlockItem> HEAVY_GOLD_BLOCK = ITEMS.register(
      "heavy_gold_block", () -> new BlockItem((Block)ModBlocks.HEAVY_GOLD_BLOCK.get(), new Properties())
   );
   public static final DeferredItem<CursedHeavyGoldBlockItem> CURSED_HEAVY_GOLD_BLOCK = ITEMS.register(
      "cursed_heavy_gold_block", () -> new CursedHeavyGoldBlockItem((Block)ModBlocks.CURSED_HEAVY_GOLD_BLOCK.get(), new Properties())
   );
   public static final DeferredItem<EnchantedHeavyGoldBlockItem> ENCHANTED_HEAVY_GOLD_BLOCK = ITEMS.register(
      "enchanted_heavy_gold_block", () -> new EnchantedHeavyGoldBlockItem((Block)ModBlocks.ENCHANTED_HEAVY_GOLD_BLOCK.get(), new Properties())
   );
   public static final DeferredItem<SupercooledFrostMetalBlockItem> SUPERCOOLED_FROST_METAL_BLOCK = ITEMS.register(
      "supercooled_frost_metal_block", () -> new SupercooledFrostMetalBlockItem((Block)ModBlocks.SUPERCOOLED_FROST_METAL_BLOCK.get(), new Properties())
   );
   public static final DeferredItem<BlockItem> ANCHORED_BEACON = ITEMS.register(
      "anchored_beacon", () -> new BlockItem((Block)ModBlocks.ANCHORED_BEACON.get(), new Properties().rarity(Rarity.RARE))
   );
   public static final DeferredItem<Item> JI_INGOT = ITEMS.register("ji_ingot", () -> new Item(new Properties()));
   public static final DeferredItem<Item> JI_UPGRADE = ITEMS.register("ji_upgrade", () -> new JiUpgradeTemplateItem(new Properties()));
   public static final Tier JI_TOOL_TIER = new Tier() {
      public int getUses() {
         return 250;
      }

      public float getSpeed() {
         return 6.0F;
      }

      public float getAttackDamageBonus() {
         return 2.0F;
      }

      public TagKey<Block> getIncorrectBlocksForDrops() {
         return BlockTags.INCORRECT_FOR_IRON_TOOL;
      }

      public int getEnchantmentValue() {
         return Tiers.GOLD.getEnchantmentValue();
      }

      public Ingredient getRepairIngredient() {
         return Ingredient.of(new ItemLike[]{JI_INGOT.get()});
      }
   };
   public static final DeferredItem<SwordItem> JI_SWORD = ITEMS.register(
      "ji_sword", () -> new SwordItem(JI_TOOL_TIER, new Properties().attributes(createJiSwordAttributes()))
   );
   public static final DeferredItem<PickaxeItem> JI_PICKAXE = ITEMS.register(
      "ji_pickaxe", () -> new PickaxeItem(JI_TOOL_TIER, new Properties().attributes(PickaxeItem.createAttributes(JI_TOOL_TIER, 1.0F, -2.8F)))
   );
   public static final DeferredItem<ArmorItem> JI_HELMET = ITEMS.register(
      "ji_helmet", () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.JI), Type.HELMET, new Properties().durability(165))
   );
   public static final DeferredItem<ArmorItem> JI_CHESTPLATE = ITEMS.register(
      "ji_chestplate", () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.JI), Type.CHESTPLATE, new Properties().durability(240))
   );
   public static final DeferredItem<ArmorItem> JI_LEGGINGS = ITEMS.register(
      "ji_leggings", () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.JI), Type.LEGGINGS, new Properties().durability(225))
   );
   public static final DeferredItem<ArmorItem> JI_BOOTS = ITEMS.register(
      "ji_boots", () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.JI), Type.BOOTS, new Properties().durability(195))
   );
   public static final DeferredItem<ArmorItem> ROYAL_STEEL_HELMET = ITEMS.register(
      "royal_steel_helmet",
      () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.ROYAL_STEEL), Type.HELMET, new Properties().durability(vanillaArmor(Type.HELMET, 33)))
   );
   public static final DeferredItem<ArmorItem> ROYAL_STEEL_CHESTPLATE = ITEMS.register(
      "royal_steel_chestplate",
      () -> new ArmorItem(
            ModArmorMaterials.holder(ModArmorMaterials.ROYAL_STEEL),
            Type.CHESTPLATE,
            royalSteelChestProperties()
         )
   );
   public static final DeferredItem<ArmorItem> ROYAL_STEEL_LEGGINGS = ITEMS.register(
      "royal_steel_leggings",
      () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.ROYAL_STEEL), Type.LEGGINGS, new Properties().durability(vanillaArmor(Type.LEGGINGS, 33)))
   );
   public static final DeferredItem<ArmorItem> ROYAL_STEEL_BOOTS = ITEMS.register(
      "royal_steel_boots",
      () -> new ArmorItem(
            ModArmorMaterials.holder(ModArmorMaterials.ROYAL_STEEL),
            Type.BOOTS,
            new Properties()
               .durability(vanillaArmor(Type.BOOTS, 33))
               .component((DataComponentType)ModComponents.COMFORTABLE.get(), ComfortableComponent.INSTANCE)
         )
   );
   public static final DeferredItem<ArmorItem> FROST_METAL_HELMET = ITEMS.register(
      "frost_metal_helmet",
      () -> new ArmorItem(
            ModArmorMaterials.holder(ModArmorMaterials.FROST_METAL),
            Type.HELMET,
            new Properties()
               .durability(vanillaArmor(Type.HELMET, 37))
               .component((DataComponentType)ModComponents.MEANINGLESS.get(), MeaninglessComponent.DEFAULT)
         )
   );
   public static final DeferredItem<ArmorItem> FROST_METAL_CHESTPLATE = ITEMS.register(
      "frost_metal_chestplate",
      () -> new ArmorItem(
            ModArmorMaterials.holder(ModArmorMaterials.FROST_METAL),
            Type.CHESTPLATE,
            frostMetalChestProperties()
         )
   );
   public static final DeferredItem<ArmorItem> FROST_METAL_LEGGINGS = ITEMS.register(
      "frost_metal_leggings",
      () -> new ArmorItem(
            ModArmorMaterials.holder(ModArmorMaterials.FROST_METAL),
            Type.LEGGINGS,
            new Properties()
               .durability(vanillaArmor(Type.LEGGINGS, 37))
               .component((DataComponentType)ModComponents.MEANINGLESS.get(), MeaninglessComponent.DEFAULT)
         )
   );
   public static final DeferredItem<ArmorItem> FROST_METAL_BOOTS = ITEMS.register(
      "frost_metal_boots",
      () -> new FrostMetalBootsItem(
            ModArmorMaterials.holder(ModArmorMaterials.FROST_METAL),
            Type.BOOTS,
            new Properties()
               .durability(vanillaArmor(Type.BOOTS, 37))
               .component((DataComponentType)ModComponents.MEANINGLESS.get(), MeaninglessComponent.DEFAULT)
               .component((DataComponentType)ModComponents.FROST_WALK.get(), FrostWalkComponent.INSTANCE)
         )
   );
   public static final DeferredItem<ArmorItem> EMBER_METAL_HELMET = ITEMS.register(
      "ember_metal_helmet",
      () -> new ArmorItem(
            ModArmorMaterials.holder(ModArmorMaterials.EMBER_METAL),
            Type.HELMET,
            new Properties()
               .durability(vanillaArmor(Type.HELMET, 37))
               .fireResistant()
               .component((DataComponentType)ModComponents.HEAT_INSULATION.get(), HeatInsulationComponent.INSTANCE)
         )
   );
   public static final DeferredItem<ArmorItem> EMBER_METAL_CHESTPLATE = ITEMS.register(
      "ember_metal_chestplate",
      () -> new ArmorItem(
            ModArmorMaterials.holder(ModArmorMaterials.EMBER_METAL),
            Type.CHESTPLATE,
            emberMetalChestProperties()
         )
   );
   public static final DeferredItem<ArmorItem> EMBER_METAL_LEGGINGS = ITEMS.register(
      "ember_metal_leggings",
      () -> new ArmorItem(
            ModArmorMaterials.holder(ModArmorMaterials.EMBER_METAL),
            Type.LEGGINGS,
            new Properties()
               .durability(vanillaArmor(Type.LEGGINGS, 37))
               .fireResistant()
         )
   );
   public static final DeferredItem<ArmorItem> EMBER_METAL_BOOTS = ITEMS.register(
      "ember_metal_boots",
      () -> new ArmorItem(
            ModArmorMaterials.holder(ModArmorMaterials.EMBER_METAL),
            Type.BOOTS,
            new Properties()
               .durability(vanillaArmor(Type.BOOTS, 37))
               .fireResistant()
               .component((DataComponentType)ModComponents.LAVA_WALKER.get(), LavaWalkerComponent.INSTANCE)
         )
   );
   public static final DeferredItem<ArmorItem> TRANSCENDIUM_HELMET = ITEMS.register(
      "transcendium_helmet",
      () -> new ArmorItem(
            ModArmorMaterials.holder(ModArmorMaterials.TRANSCENDIUM),
            Type.HELMET,
            unbreakableArmor()
               .fireResistant()
               .rarity(Rarity.EPIC)
               .component((DataComponentType)ModComponents.GLOWING_VISION.get(), GlowingVisionComponent.DEFAULT)
         )
   );
   public static final DeferredItem<ArmorItem> TRANSCENDIUM_CHESTPLATE = ITEMS.register(
      "transcendium_chestplate",
      () -> new ArmorItem(
            ModArmorMaterials.holder(ModArmorMaterials.TRANSCENDIUM),
            Type.CHESTPLATE,
            transcendiumChestProperties()
         )
   );
   public static final DeferredItem<ArmorItem> TRANSCENDIUM_LEGGINGS = ITEMS.register(
      "transcendium_leggings",
      () -> new ArmorItem(
            ModArmorMaterials.holder(ModArmorMaterials.TRANSCENDIUM),
            Type.LEGGINGS,
            unbreakableArmor()
               .fireResistant()
               .rarity(Rarity.EPIC)
         )
   );
   public static final DeferredItem<ArmorItem> TRANSCENDIUM_BOOTS = ITEMS.register(
      "transcendium_boots",
      () -> new TranscendiumBootsItem(
            ModArmorMaterials.holder(ModArmorMaterials.TRANSCENDIUM), Type.BOOTS, unbreakableArmor().fireResistant().rarity(Rarity.EPIC)
         )
   );

   public static final DeferredItem<ArmorItem> SPECTRAL_HELMET = ITEMS.register(
      "spectral_helmet", () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.SPECTRAL), Type.HELMET, spectralArmorProperties(Type.HELMET))
   );
   public static final DeferredItem<ArmorItem> SPECTRAL_CHESTPLATE = ITEMS.register(
      "spectral_chestplate", () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.SPECTRAL), Type.CHESTPLATE, spectralArmorProperties(Type.CHESTPLATE))
   );
   public static final DeferredItem<ArmorItem> SPECTRAL_LEGGINGS = ITEMS.register(
      "spectral_leggings", () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.SPECTRAL), Type.LEGGINGS, spectralArmorProperties(Type.LEGGINGS))
   );
   public static final DeferredItem<ArmorItem> SPECTRAL_BOOTS = ITEMS.register(
      "spectral_boots", () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.SPECTRAL), Type.BOOTS, spectralArmorProperties(Type.BOOTS))
   );
   /** 小口袋：7 皮革合成，给护腿 +6 栏位。 */
   public static final DeferredItem<Item> SMALL_POUCH = ITEMS.register("small_pouch", () -> new PouchItem(new Properties()));
   /** 深口袋：小口袋中心放小口袋合成，给护腿 +12 栏位。 */
   public static final DeferredItem<Item> BIG_POUCH = ITEMS.register("big_pouch", () -> new PouchItem(new Properties()));
   /** 蹲起式发电器：纯工具（无耐久/护甲），穿在护腿位做蹲起动作发电，功率随蹲起频率提升（封顶120kW），逻辑见 SquatGeneratorManager。 */
   public static final DeferredItem<ArmorItem> SQUAT_GENERATOR_LEGGINGS = ITEMS.register(
      "squat_generator_leggings",
      () -> new SquatGeneratorLeggingsItem(ModArmorMaterials.holder(ModArmorMaterials.SQUAT_GENERATOR), Type.LEGGINGS, new Properties().stacksTo(1))
   );

   public static boolean isCreateTemplate(ItemStack stack) {
      return !stack.isEmpty() && stack.is((Item)CREATE_TEMPLATE.get());
   }

   private static int vanillaArmor(Type type, int multiplier) {
      return type.getDurability(multiplier);
   }

   private static Properties unbreakableArmor() {
      return new Properties().durability(2031).component(DataComponents.UNBREAKABLE, new Unbreakable(true));
   }

   /**
    * 幻灵套装属性：耐久等同于铁套倍率15，附带无法破坏组件。
    * durability 仅为兼容卸载组件后行为，实际不损耗。
    */
   private static Properties spectralArmorProperties(Type type) {
      return new Properties()
         .durability(vanillaArmor(type, 15))
         .component(DataComponents.UNBREAKABLE, new Unbreakable(true));
   }

   // --- 胸甲 Properties 工厂：统一组件/耐久配置 ---
   private static Properties royalSteelChestProperties() {
      return new Properties()
         .durability(vanillaArmor(Type.CHESTPLATE, 33))
         .component((DataComponentType)ModComponents.ROYAL_STEEL_AFFINITY.get(), RoyalSteelAffinityComponent.INSTANCE);
   }

   private static Properties frostMetalChestProperties() {
      return new Properties()
         .durability(vanillaArmor(Type.CHESTPLATE, 37))
         .component((DataComponentType)ModComponents.MEANINGLESS.get(), MeaninglessComponent.DEFAULT);
   }

   private static Properties emberMetalChestProperties() {
      return new Properties()
         .durability(vanillaArmor(Type.CHESTPLATE, 37))
         .fireResistant();
   }

   private static Properties transcendiumChestProperties() {
      return unbreakableArmor()
         .fireResistant()
         .rarity(Rarity.EPIC)
         .component((DataComponentType)ModComponents.NEUTRON_BARRIER.get(), NeutronBarrierComponent.INSTANCE);
   }

   private static ItemAttributeModifiers createJiSwordAttributes() {
      ItemAttributeModifiers base = SwordItem.createAttributes(JI_TOOL_TIER, 3, -2.4F);
      Builder builder = ItemAttributeModifiers.builder();
      base.modifiers().forEach(entry -> builder.add(entry.attribute(), entry.modifier(), entry.slot()));
      builder.add(
         Attributes.ENTITY_INTERACTION_RANGE,
         new AttributeModifier(ResourceLocation.fromNamespaceAndPath("dingdongji", "ji_sword_entity_range"), 2.0, Operation.ADD_VALUE),
         EquipmentSlotGroup.MAINHAND
      );
      return builder.build();
   }
}
