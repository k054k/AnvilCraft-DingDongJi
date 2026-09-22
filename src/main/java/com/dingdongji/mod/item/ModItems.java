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
   public static final DeferredItem<Item> JI_UPGRADE = ITEMS.register("ji_upgrade", () -> new JiUpgradeTemplateItem(new Properties()));
   public static final Tier JI_TOOL_TIER = new Tier() {
      public int getUses() {
         return 1561;
      }

      public float getSpeed() {
         return 14.0F;
      }

      public float getAttackDamageBonus() {
         return 0.0F;
      }

      public TagKey<Block> getIncorrectBlocksForDrops() {
         return BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
      }

      public int getEnchantmentValue() {
         return Tiers.GOLD.getEnchantmentValue();
      }

      public Ingredient getRepairIngredient() {
         return Ingredient.of(new ItemLike[]{net.minecraft.world.item.Items.GOLD_INGOT});
      }
   };
   public static final DeferredItem<SwordItem> JI_SWORD = ITEMS.register(
      "ji_sword", () -> new SwordItem(JI_TOOL_TIER, new Properties().attributes(createJiSwordAttributes()))
   );
   public static final DeferredItem<PickaxeItem> JI_PICKAXE = ITEMS.register(
      "ji_pickaxe", () -> new PickaxeItem(JI_TOOL_TIER, new Properties().attributes(PickaxeItem.createAttributes(JI_TOOL_TIER, 1.0F, -2.8F)))
   );
   private static final int DURABILITY_JI = 1561;
   private static final int VANILLA_DIAMOND = 33;
   private static final int VANILLA_NETHERITE = 37;
   public static final DeferredItem<ArmorItem> JI_HELMET = ITEMS.register(
      "ji_helmet", () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.JI), Type.HELMET, new Properties().durability(1561))
   );
   public static final DeferredItem<ArmorItem> JI_CHESTPLATE = ITEMS.register(
      "ji_chestplate", () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.JI), Type.CHESTPLATE, new Properties().durability(1561))
   );
   public static final DeferredItem<ArmorItem> JI_LEGGINGS = ITEMS.register(
      "ji_leggings", () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.JI), Type.LEGGINGS, new Properties().durability(1561))
   );
   public static final DeferredItem<ArmorItem> JI_BOOTS = ITEMS.register(
      "ji_boots", () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.JI), Type.BOOTS, new Properties().durability(1561))
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
            new Properties()
               .durability(vanillaArmor(Type.CHESTPLATE, 33))
               .component((DataComponentType)ModComponents.ROYAL_STEEL_AFFINITY.get(), RoyalSteelAffinityComponent.INSTANCE)
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
               .component((DataComponentType)ModComponents.FROST_WARD.get(), FrostWardComponent.INSTANCE)
         )
   );
   public static final DeferredItem<ArmorItem> FROST_METAL_CHESTPLATE = ITEMS.register(
      "frost_metal_chestplate",
      () -> new ArmorItem(
            ModArmorMaterials.holder(ModArmorMaterials.FROST_METAL),
            Type.CHESTPLATE,
            new Properties()
               .durability(vanillaArmor(Type.CHESTPLATE, 37))
               .component((DataComponentType)ModComponents.MEANINGLESS.get(), MeaninglessComponent.DEFAULT)
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
            new Properties()
               .durability(vanillaArmor(Type.CHESTPLATE, 37))
               .fireResistant()
               .component((DataComponentType)ModComponents.BARRIER_I.get(), BarrierIComponent.INSTANCE)
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
               .component((DataComponentType)ModComponents.EMBER_REGEN.get(), EmberRegenComponent.INSTANCE)
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
            unbreakableArmor().fireResistant().rarity(Rarity.EPIC).component((DataComponentType)ModComponents.BARRIER_II.get(), BarrierIIComponent.INSTANCE)
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
               .component((DataComponentType)ModComponents.NEUTRON_BARRIER.get(), NeutronBarrierComponent.INSTANCE)
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

   // 中子航空套：耐候套上位，功能暂为占位，发光屏幕走光带渲染层。
   public static final DeferredItem<ArmorItem> NEUTRON_SPACESUIT_HELMET = ITEMS.register(
      "neutron_spacesuit_helmet",
      () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.NEUTRON_SPACESUIT), Type.HELMET, neutronSpacesuitProperties(Type.HELMET))
   );
   public static final DeferredItem<ArmorItem> NEUTRON_SPACESUIT_CHESTPLATE = ITEMS.register(
      "neutron_spacesuit_chestplate",
      () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.NEUTRON_SPACESUIT), Type.CHESTPLATE, neutronSpacesuitProperties(Type.CHESTPLATE))
   );
   public static final DeferredItem<ArmorItem> NEUTRON_SPACESUIT_LEGGINGS = ITEMS.register(
      "neutron_spacesuit_leggings",
      () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.NEUTRON_SPACESUIT), Type.LEGGINGS, neutronSpacesuitProperties(Type.LEGGINGS))
   );
   public static final DeferredItem<ArmorItem> NEUTRON_SPACESUIT_BOOTS = ITEMS.register(
      "neutron_spacesuit_boots",
      () -> new ArmorItem(ModArmorMaterials.holder(ModArmorMaterials.NEUTRON_SPACESUIT), Type.BOOTS, neutronSpacesuitProperties(Type.BOOTS))
   );

   /** 小口袋：7 皮革合成，给护腿 +6 栏位。 */
   public static final DeferredItem<Item> SMALL_POUCH = ITEMS.register("small_pouch", () -> new PouchItem(new Properties()));
   /** 深口袋：小口袋中心放小口袋合成，给护腿 +12 栏位。 */
   public static final DeferredItem<Item> BIG_POUCH = ITEMS.register("big_pouch", () -> new PouchItem(new Properties()));

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

   /** 中子航空套：下界合金档耐久，防火，史诗稀有度。功能后续补齐。 */
   private static Properties neutronSpacesuitProperties(Type type) {
      return new Properties()
         .durability(vanillaArmor(type, 37))
         .fireResistant()
         .rarity(Rarity.EPIC);
   }

   private static ItemAttributeModifiers createJiSwordAttributes() {
      ItemAttributeModifiers base = SwordItem.createAttributes(JI_TOOL_TIER, 6, -2.4F);
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
