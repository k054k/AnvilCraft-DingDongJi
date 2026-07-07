package com.dingdongji.mod.item;

import com.dingdongji.mod.item.component.AccumulateData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.Level;
import net.minecraft.core.component.DataComponents;

public class WhiteHoleSwordItem extends SwordItem {
    public WhiteHoleSwordItem(int attackDamage) {
        super(Tiers.WOOD, new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant()
                .attributes(SwordItem.createAttributes(Tiers.WOOD, attackDamage, -2.4f))
                .component(ModComponents.ACCUMULATE.get(), AccumulateData.DEFAULT)
                .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
        );
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && !isSelected && slotId != 40 && entity instanceof Player) {
            AccumulateData data = stack.get(ModComponents.ACCUMULATE.get());
            if (data != null) {
                stack.set(ModComponents.ACCUMULATE.get(), new AccumulateData(data.ticks() + 1));
            }
        }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        AccumulateData data = stack.get(ModComponents.ACCUMULATE.get());
        if (data != null && data.ticks() > 0) {
            int newTicks = Math.max(0, data.ticks() / 2);
            stack.set(ModComponents.ACCUMULATE.get(), new AccumulateData(newTicks));
        }
        return true;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 22;
    }
}
