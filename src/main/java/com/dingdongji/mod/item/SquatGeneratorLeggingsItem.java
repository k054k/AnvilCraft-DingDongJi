package com.dingdongji.mod.item;

import com.dingdongji.mod.KryptonMod;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

/**
 * 蹲起式发电器护腿：纯工具定位，覆写贴图路径以绕过 vanilla layer_1/layer_2 命名。
 */
public class SquatGeneratorLeggingsItem extends ArmorItem {
   private static final ResourceLocation TEXTURE = KryptonMod.modLoc("textures/entity/equipment/squat_generator.png");

   public SquatGeneratorLeggingsItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
      super(material, type, properties);
   }

   @Override
   public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
      return TEXTURE;
   }
}
