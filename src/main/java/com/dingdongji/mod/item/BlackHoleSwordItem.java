package com.dingdongji.mod.item;

import com.dingdongji.mod.item.component.DevourData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.Unbreakable;

public class BlackHoleSwordItem extends SwordItem {
   public BlackHoleSwordItem() {
      super(
         Tiers.WOOD,
         new Properties()
            .stacksTo(1)
            .rarity(Rarity.EPIC)
            .fireResistant()
            .attributes(SwordItem.createAttributes(Tiers.WOOD, 0, -2.4F))
            .component((DataComponentType)ModComponents.DEVOUR.get(), new DevourData(0))
            .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
      );
   }

   public boolean isDamageable(ItemStack stack) {
      return false;
   }

   public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      return true;
   }

   public boolean isEnchantable(ItemStack stack) {
      return true;
   }

   public int getEnchantmentValue(ItemStack stack) {
      return 22;
   }
}
