package com.dingdongji.mod.item;

import com.dingdongji.mod.KryptonMod;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

public class ModArmorMaterials {

    /** 从 DeferredHolder 获取 Holder（用于 ArmorItem 构造器） */
    public static Holder<ArmorMaterial> holder(DeferredHolder<ArmorMaterial, ArmorMaterial> deferred) {
        return BuiltInRegistries.ARMOR_MATERIAL.getHolderOrThrow(deferred.getKey());
    }
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, KryptonMod.MODID);

    private static Ingredient anvilCraftItem(String path) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse("anvilcraft:" + path));
        return item != Items.AIR ? Ingredient.of(item) : Ingredient.EMPTY;
    }

    // ===== 叽盔甲 =====
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> JI = ARMOR_MATERIALS.register("ji",
            () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.HELMET, 3);
                        map.put(ArmorItem.Type.CHESTPLATE, 8);
                        map.put(ArmorItem.Type.LEGGINGS, 6);
                        map.put(ArmorItem.Type.BOOTS, 3);
                    }),
                    10,
                    SoundEvents.ARMOR_EQUIP_DIAMOND,
                    () -> Ingredient.of(Items.GOLD_INGOT),
                    List.of(new ArmorMaterial.Layer(KryptonMod.modLoc("ji"))),
                    2.0f,
                    0.0f
            )
    );

    // ===== 皇家钢盔甲 =====
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ROYAL_STEEL = ARMOR_MATERIALS.register("royal_steel",
            () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.HELMET, 3);
                        map.put(ArmorItem.Type.CHESTPLATE, 8);
                        map.put(ArmorItem.Type.LEGGINGS, 6);
                        map.put(ArmorItem.Type.BOOTS, 3);
                    }),
                    10,
                    SoundEvents.ARMOR_EQUIP_DIAMOND,
                    () -> anvilCraftItem("royal_steel_ingot"),
                    List.of(new ArmorMaterial.Layer(KryptonMod.modLoc("royal_steel"))),
                    2.0f,
                    0.0f
            )
    );

    // ===== 余烬金属盔甲 =====
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> EMBER_METAL = ARMOR_MATERIALS.register("ember_metal",
            () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.HELMET, 3);
                        map.put(ArmorItem.Type.CHESTPLATE, 8);
                        map.put(ArmorItem.Type.LEGGINGS, 6);
                        map.put(ArmorItem.Type.BOOTS, 3);
                    }),
                    15,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> anvilCraftItem("ember_metal_ingot"),
                    List.of(new ArmorMaterial.Layer(KryptonMod.modLoc("ember_metal"))),
                    3.0f,
                    0.1f
            )
    );

    // ===== 超限合金盔甲 =====
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> TRANSCENDIUM = ARMOR_MATERIALS.register("transcendium",
            () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.HELMET, 5);
                        map.put(ArmorItem.Type.CHESTPLATE, 10);
                        map.put(ArmorItem.Type.LEGGINGS, 8);
                        map.put(ArmorItem.Type.BOOTS, 5);
                    }),
                    15,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> anvilCraftItem("transcendium_ingot"),
                    List.of(new ArmorMaterial.Layer(KryptonMod.modLoc("transcendium"))),
                    5.0f,
                    0.25f
            )
    );
}
