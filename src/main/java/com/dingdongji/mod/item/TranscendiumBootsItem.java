package com.dingdongji.mod.item;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.Item.Properties;
import org.jetbrains.annotations.NotNull;

public class TranscendiumBootsItem extends ArmorItem {
   public TranscendiumBootsItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
      super(material, type, properties);
   }

   public boolean canWalkOnPowderedSnow(@NotNull ItemStack stack, @NotNull LivingEntity wearer) {
      return true;
   }
}
