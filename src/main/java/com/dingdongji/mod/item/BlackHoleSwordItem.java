package com.dingdongji.mod.item;

import com.dingdongji.mod.item.component.DevourData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.Unbreakable;

public class BlackHoleSwordItem extends SwordItem {
    public BlackHoleSwordItem() {
        super(Tiers.WOOD, new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant()
                .attributes(SwordItem.createAttributes(Tiers.WOOD, 0, -2.4f))
                .component(ModComponents.DEVOUR.get(), new DevourData(0))
                .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
        );
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return true;
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
