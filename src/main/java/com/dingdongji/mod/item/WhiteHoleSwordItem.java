package com.dingdongji.mod.item;

import com.dingdongji.mod.item.component.AccumulateData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.Level;

public class WhiteHoleSwordItem extends SwordItem {
   private static final int MAX_TICKS = 2147000000;

   public WhiteHoleSwordItem(int attackDamage) {
      super(
         Tiers.WOOD,
         new Properties()
            .stacksTo(1)
            .rarity(Rarity.EPIC)
            .fireResistant()
            .attributes(SwordItem.createAttributes(Tiers.WOOD, attackDamage, -2.4F))
            .component((DataComponentType)ModComponents.ACCUMULATE.get(), AccumulateData.DEFAULT)
            .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
      );
   }

   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
      if (!level.isClientSide && !isSelected && slotId != 40 && entity instanceof Player) {
         AccumulateData data = (AccumulateData)stack.get((DataComponentType)ModComponents.ACCUMULATE.get());
         if (data != null) {
            int ticks = data.ticks();
            if (ticks < 2147000000) {
               stack.set((DataComponentType)ModComponents.ACCUMULATE.get(), new AccumulateData(ticks + 1));
            }
         }
      }
   }

   public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      AccumulateData data = (AccumulateData)stack.get((DataComponentType)ModComponents.ACCUMULATE.get());
      if (data != null && data.ticks() > 0) {
         int totalTicks = data.ticks();
         stack.set((DataComponentType)ModComponents.ACCUMULATE.get(), new AccumulateData(totalTicks / 2));
         float bonusDamage = (float)totalTicks * 0.01F;
         if (bonusDamage > 0.0F && target.isAlive()) {
            float newHealth = target.getHealth() - bonusDamage;
            target.setHealth(Math.max(newHealth, 0.0F));
            if (newHealth <= 0.0F) {
               target.hurt(target.damageSources().genericKill(), 0.0F);
            }
         }
      }

      return true;
   }

   public boolean isDamageable(ItemStack stack) {
      return false;
   }

   public boolean isEnchantable(ItemStack stack) {
      return true;
   }

   public int getEnchantmentValue(ItemStack stack) {
      return 22;
   }
}
