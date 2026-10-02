package com.dingdongji.mod.tab;

import com.dingdongji.mod.block.ModBlocks;
import com.dingdongji.mod.item.ModItems;
import dev.anvilcraft.lib.v2.registrum.util.CreativeTabSection;
import dev.anvilcraft.lib.v2.registrum.util.CreativeTabSections;
import dev.dubhe.anvilcraft.AnvilCraft;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTab {
   public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "dingdongji");

   // Must equal the registered tab name below: AnvilLib's creative-tab mixin
   // looks the tab's layout up by this ResourceLocation.
   private static final ResourceLocation TAB_ID = ResourceLocation.fromNamespaceAndPath("dingdongji", "dingdongji_tab");

   public static final Supplier<CreativeModeTab> KRYPTON_TAB = CREATIVE_MODE_TABS.register(
      "dingdongji_tab",
      () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.dingdongji"))
            .icon(() -> new ItemStack((ItemLike)ModBlocks.JI_ANVIL.get()))
            .displayItems(ModCreativeTab::buildDisplayItems)
            .build()
   );

   private static void buildDisplayItems(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
      // Follow AnvilCraft's own "Use legacy creative inventory" client option,
      // exactly like the official PigsPlus addon: when the base mod uses the
      // flat legacy layout we stay flat, otherwise we join the AnvilLib
      // sectioned layout with the base mod's banners and title keys. No config
      // of our own — the base mod's option (which requires a restart) is the
      // single switch for the whole addon ecosystem.
      if (AnvilCraft.CLIENT_CONFIG.useLegacyCreativeTab) {
         acceptFlat(output);
         return;
      }

      // Sectioned view: AnvilLib (provided at runtime by the hard AnvilCraft
      // dependency) records the section layout and its screen mixin draws the
      // banners. Banner textures reuse AnvilCraft's own 54x18 strips.
      CreativeTabSections.build(TAB_ID, parameters, output, sections -> {
         // 顺序：锻造 → 材料 → 锻造模板 → 工具 → 战斗 → 创造
         addSection(sections, "functional_blocks", "forging",
            ModBlocks.JI_ANVIL.get());
         addSection(sections, "building_blocks", "materials",
            ModItems.KEJI_BLOCK.get());
         addSection(sections, "items", "smithing_template",
            ModItems.JI_UPGRADE.get(), ModItems.CREATE_TEMPLATE.get());
         addSection(sections, "items", "tools",
            ModItems.JI_PICKAXE.get(), ModItems.SMALL_POUCH.get(), ModItems.BIG_POUCH.get());
         addSection(sections, "items", "combat",
            ModItems.JI_SWORD.get(),
            ModItems.JI_HELMET.get(), ModItems.JI_CHESTPLATE.get(),
            ModItems.JI_LEGGINGS.get(), ModItems.JI_BOOTS.get(),
            ModItems.SPECTRAL_HELMET.get(), ModItems.SPECTRAL_CHESTPLATE.get(),
            ModItems.SPECTRAL_LEGGINGS.get(), ModItems.SPECTRAL_BOOTS.get(),
            ModItems.ROYAL_STEEL_HELMET.get(), ModItems.ROYAL_STEEL_CHESTPLATE.get(),
            ModItems.ROYAL_STEEL_LEGGINGS.get(), ModItems.ROYAL_STEEL_BOOTS.get(),
            ModItems.FROST_METAL_HELMET.get(), ModItems.FROST_METAL_CHESTPLATE.get(),
            ModItems.FROST_METAL_LEGGINGS.get(), ModItems.FROST_METAL_BOOTS.get(),
            ModItems.EMBER_METAL_HELMET.get(), ModItems.EMBER_METAL_CHESTPLATE.get(),
            ModItems.EMBER_METAL_LEGGINGS.get(), ModItems.EMBER_METAL_BOOTS.get(),
            ModItems.TRANSCENDIUM_HELMET.get(), ModItems.TRANSCENDIUM_CHESTPLATE.get(),
            ModItems.TRANSCENDIUM_LEGGINGS.get(), ModItems.TRANSCENDIUM_BOOTS.get());
         addSection(sections, "functional_blocks", "creative",
            ModItems.BLACK_HOLE_SWORD.get(), ModItems.WHITE_HOLE_SWORD.get());
      });
   }

   private static void addSection(CreativeTabSections sections, String bannerFolder, String bannerName, ItemLike... items) {
      ResourceLocation banner = ResourceLocation.fromNamespaceAndPath(
         "anvilcraft", "textures/gui/creative_inventory/section/" + bannerFolder + "/" + bannerName + ".png");
      // Reuse AnvilCraft's own section title/tooltip keys verbatim.
      String key = "anvilcraft.creative.section." + bannerFolder + "." + bannerName;
      sections.section(
         CreativeTabSection.builder(banner)
            .textAlignment(CreativeTabSection.TextAlignment.RIGHT)
            .textRange(16, 49)
            .text(Component.translatable(key))
            .tooltip(Component.translatable(key))
            .build(),
         s -> {
            for (ItemLike item : items) {
               s.accept(item);
            }
         }
      );
   }

   private static void acceptFlat(CreativeModeTab.Output output) {
      output.accept((ItemLike)ModBlocks.JI_ANVIL.get());
      output.accept((ItemLike)ModItems.KEJI_BLOCK.get());
      output.accept((ItemLike)ModItems.JI_UPGRADE.get());
      output.accept((ItemLike)ModItems.JI_SWORD.get());
      output.accept((ItemLike)ModItems.JI_PICKAXE.get());
      output.accept((ItemLike)ModItems.JI_HELMET.get());
      output.accept((ItemLike)ModItems.JI_CHESTPLATE.get());
      output.accept((ItemLike)ModItems.JI_LEGGINGS.get());
      output.accept((ItemLike)ModItems.JI_BOOTS.get());
      output.accept((ItemLike)ModItems.SMALL_POUCH.get());
      output.accept((ItemLike)ModItems.BIG_POUCH.get());
      output.accept((ItemLike)ModItems.SPECTRAL_HELMET.get());
      output.accept((ItemLike)ModItems.SPECTRAL_CHESTPLATE.get());
      output.accept((ItemLike)ModItems.SPECTRAL_LEGGINGS.get());
      output.accept((ItemLike)ModItems.SPECTRAL_BOOTS.get());
      output.accept((ItemLike)ModItems.ROYAL_STEEL_HELMET.get());
      output.accept((ItemLike)ModItems.ROYAL_STEEL_CHESTPLATE.get());
      output.accept((ItemLike)ModItems.ROYAL_STEEL_LEGGINGS.get());
      output.accept((ItemLike)ModItems.ROYAL_STEEL_BOOTS.get());
      output.accept((ItemLike)ModItems.FROST_METAL_HELMET.get());
      output.accept((ItemLike)ModItems.FROST_METAL_CHESTPLATE.get());
      output.accept((ItemLike)ModItems.FROST_METAL_LEGGINGS.get());
      output.accept((ItemLike)ModItems.FROST_METAL_BOOTS.get());
      output.accept((ItemLike)ModItems.EMBER_METAL_HELMET.get());
      output.accept((ItemLike)ModItems.EMBER_METAL_CHESTPLATE.get());
      output.accept((ItemLike)ModItems.EMBER_METAL_LEGGINGS.get());
      output.accept((ItemLike)ModItems.EMBER_METAL_BOOTS.get());
      output.accept((ItemLike)ModItems.TRANSCENDIUM_HELMET.get());
      output.accept((ItemLike)ModItems.TRANSCENDIUM_CHESTPLATE.get());
      output.accept((ItemLike)ModItems.TRANSCENDIUM_LEGGINGS.get());
      output.accept((ItemLike)ModItems.TRANSCENDIUM_BOOTS.get());
      output.accept((ItemLike)ModItems.BLACK_HOLE_SWORD.get());
      output.accept((ItemLike)ModItems.WHITE_HOLE_SWORD.get());
      output.accept((ItemLike)ModItems.CREATE_TEMPLATE.get());
   }
}
