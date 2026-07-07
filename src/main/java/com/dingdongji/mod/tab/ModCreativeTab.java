package com.dingdongji.mod.tab;

import com.dingdongji.mod.KryptonMod;
import com.dingdongji.mod.block.ModBlocks;
import com.dingdongji.mod.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KryptonMod.MODID);

    public static final Supplier<CreativeModeTab> KRYPTON_TAB = CREATIVE_MODE_TABS.register("dingdongji_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.dingdongji"))
                    .icon(() -> new ItemStack(ModBlocks.JI_ANVIL.get()))
                    .displayItems((parameters, output) -> {
                        // 方块
                        output.accept(ModBlocks.JI_ANVIL.get());
                        output.accept(ModItems.KEJI_BLOCK.get());
                        // 物品
                        output.accept(ModItems.JI_UPGRADE.get());
                        // 工具
                        output.accept(ModItems.JI_SWORD.get());
                        output.accept(ModItems.JI_PICKAXE.get());
                        // 盔甲 - 叽套
                        output.accept(ModItems.JI_HELMET.get());
                        output.accept(ModItems.JI_CHESTPLATE.get());
                        output.accept(ModItems.JI_LEGGINGS.get());
                        output.accept(ModItems.JI_BOOTS.get());
                        // 盔甲 - 皇家钢套
                        output.accept(ModItems.ROYAL_STEEL_HELMET.get());
                        output.accept(ModItems.ROYAL_STEEL_CHESTPLATE.get());
                        output.accept(ModItems.ROYAL_STEEL_LEGGINGS.get());
                        output.accept(ModItems.ROYAL_STEEL_BOOTS.get());
                        // 盔甲 - 余烬金属套
                        output.accept(ModItems.EMBER_METAL_HELMET.get());
                        output.accept(ModItems.EMBER_METAL_CHESTPLATE.get());
                        output.accept(ModItems.EMBER_METAL_LEGGINGS.get());
                        output.accept(ModItems.EMBER_METAL_BOOTS.get());
                        // 盔甲 - 超限合金套
                        output.accept(ModItems.TRANSCENDIUM_HELMET.get());
                        output.accept(ModItems.TRANSCENDIUM_CHESTPLATE.get());
                        output.accept(ModItems.TRANSCENDIUM_LEGGINGS.get());
                        output.accept(ModItems.TRANSCENDIUM_BOOTS.get());
                        // 武器
                        output.accept(ModItems.BLACK_HOLE_SWORD.get());
                        output.accept(ModItems.WHITE_HOLE_SWORD.get());
                        // 创造模板
                        output.accept(ModItems.CREATE_TEMPLATE.get());
                    })
                    .build()
    );
}
