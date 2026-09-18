package com.dingdongji.mod.tab;

import com.dingdongji.mod.block.ModBlocks;
import com.dingdongji.mod.item.ModItems;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTab {
   public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "dingdongji");
   public static final Supplier<CreativeModeTab> KRYPTON_TAB = CREATIVE_MODE_TABS.register(
      "dingdongji_tab",
      () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.dingdongji"))
            .icon(() -> new ItemStack((ItemLike)ModBlocks.JI_ANVIL.get()))
            .displayItems((parameters, output) -> {
               output.accept((ItemLike)ModBlocks.JI_ANVIL.get());
               output.accept((ItemLike)ModItems.KEJI_BLOCK.get());
               output.accept((ItemLike)ModItems.JI_UPGRADE.get());
               output.accept((ItemLike)ModItems.JI_SWORD.get());
               output.accept((ItemLike)ModItems.JI_PICKAXE.get());
               output.accept((ItemLike)ModItems.JI_HELMET.get());
               output.accept((ItemLike)ModItems.JI_CHESTPLATE.get());
               output.accept((ItemLike)ModItems.JI_LEGGINGS.get());
               output.accept((ItemLike)ModItems.JI_BOOTS.get());
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
            })
            .build()
   );
}
