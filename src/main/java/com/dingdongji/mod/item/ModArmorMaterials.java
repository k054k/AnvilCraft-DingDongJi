package com.dingdongji.mod.item;

import com.dingdongji.mod.KryptonMod;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.ArmorMaterial.Layer;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModArmorMaterials {
   public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, "dingdongji");
   public static final DeferredHolder<ArmorMaterial, ArmorMaterial> JI = ARMOR_MATERIALS.register(
      "ji", () -> new ArmorMaterial((Map)Util.make(new EnumMap(Type.class), map -> {
            map.put(Type.HELMET, 3);
            map.put(Type.CHESTPLATE, 8);
            map.put(Type.LEGGINGS, 6);
            map.put(Type.BOOTS, 3);
         }), 10, SoundEvents.ARMOR_EQUIP_DIAMOND, () -> Ingredient.of(new ItemLike[]{Items.GOLD_INGOT}), List.of(new Layer(KryptonMod.modLoc("ji"))), 2.0F, 0.0F)
   );
   public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ROYAL_STEEL = ARMOR_MATERIALS.register(
      "royal_steel", () -> new ArmorMaterial((Map)Util.make(new EnumMap(Type.class), map -> {
            map.put(Type.HELMET, 3);
            map.put(Type.CHESTPLATE, 8);
            map.put(Type.LEGGINGS, 6);
            map.put(Type.BOOTS, 3);
         }), 10, SoundEvents.ARMOR_EQUIP_DIAMOND, () -> anvilCraftItem("royal_steel_ingot"), List.of(new Layer(KryptonMod.modLoc("royal_steel"))), 2.0F, 0.0F)
   );
   public static final DeferredHolder<ArmorMaterial, ArmorMaterial> EMBER_METAL = ARMOR_MATERIALS.register(
      "ember_metal", () -> new ArmorMaterial((Map)Util.make(new EnumMap(Type.class), map -> {
            map.put(Type.HELMET, 3);
            map.put(Type.CHESTPLATE, 8);
            map.put(Type.LEGGINGS, 6);
            map.put(Type.BOOTS, 3);
         }), 15, SoundEvents.ARMOR_EQUIP_NETHERITE, () -> anvilCraftItem("ember_metal_ingot"), List.of(new Layer(KryptonMod.modLoc("ember_metal"))), 3.0F, 0.1F)
   );
   public static final DeferredHolder<ArmorMaterial, ArmorMaterial> FROST_METAL = ARMOR_MATERIALS.register(
      "frost_metal", () -> new ArmorMaterial((Map)Util.make(new EnumMap(Type.class), map -> {
            map.put(Type.HELMET, 3);
            map.put(Type.CHESTPLATE, 8);
            map.put(Type.LEGGINGS, 6);
            map.put(Type.BOOTS, 3);
         }), 15, SoundEvents.ARMOR_EQUIP_NETHERITE, () -> anvilCraftItem("frost_metal_ingot"), List.of(new Layer(KryptonMod.modLoc("frost_metal"))), 3.0F, 0.1F)
   );
   public static final DeferredHolder<ArmorMaterial, ArmorMaterial> TRANSCENDIUM = ARMOR_MATERIALS.register(
      "transcendium",
      () -> new ArmorMaterial((Map)Util.make(new EnumMap(Type.class), map -> {
            map.put(Type.HELMET, 5);
            map.put(Type.CHESTPLATE, 10);
            map.put(Type.LEGGINGS, 8);
            map.put(Type.BOOTS, 5);
         }), 15, SoundEvents.ARMOR_EQUIP_NETHERITE, () -> anvilCraftItem("transcendium_ingot"), List.of(new Layer(KryptonMod.modLoc("transcendium"))), 5.0F, 0.25F)
   );
   public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SPECTRAL = ARMOR_MATERIALS.register(
      "spectral", () -> new ArmorMaterial((Map)Util.make(new EnumMap(Type.class), map -> {
            map.put(Type.HELMET, 3);
            map.put(Type.CHESTPLATE, 8);
            map.put(Type.LEGGINGS, 6);
            map.put(Type.BOOTS, 3);
         }), 10, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(new ItemLike[]{Items.IRON_INGOT}), List.of(new Layer(KryptonMod.modLoc("spectral"))), 2.0F, 0.0F)
   );

   public static Holder<ArmorMaterial> holder(DeferredHolder<ArmorMaterial, ArmorMaterial> deferred) {
      return BuiltInRegistries.ARMOR_MATERIAL.getHolderOrThrow(deferred.getKey());
   }

   private static Ingredient anvilCraftItem(String path) {
      Item item = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.parse("anvilcraft:" + path));
      return item != Items.AIR ? Ingredient.of(new ItemLike[]{item}) : Ingredient.EMPTY;
   }
}
