package com.dingdongji.mod.item;

import dev.dubhe.anvilcraft.item.abnormal.IAbnormal;
import dev.dubhe.anvilcraft.item.abnormal.IEnchantedGold;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

// 附魔重金块：携带清除诅咒金 debuff（走 anvilcraft:enchanted_gold 物品标签）；
// 携带一整组（64 个）时获得幸运 II（普通附魔金块的 2 倍，本体为幸运 I）；
// 猪灵货币，以物易物产出翻倍（翻倍逻辑见 PiglinAiMixin）
public class EnchantedHeavyGoldBlockItem extends BlockItem implements IEnchantedGold {
   public EnchantedHeavyGoldBlockItem(Block block, Properties properties) {
      super(block, properties);
   }

   @Override
   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
      super.inventoryTick(stack, level, entity, slot, selected);
      // 不走 IEnchantedGold.super.inventoryTick（本体只给幸运 I），
      // 此处效果等级翻倍（幸运 II），清除诅咒 debuff 逻辑与本体一致
      if (level.isClientSide || !(entity instanceof Player player)) {
         return;
      }
      if (player.getAbilities().instabuild || player.getAbilities().invulnerable) {
         return;
      }
      if (IEnchantedGold.getEnchantedGoldCount(player) >= 64) {
         player.addEffect(IAbnormal.makeEffectInstance(MobEffects.LUCK, 1));
      }
      player.removeEffect(MobEffects.WEAKNESS);
      player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
      player.removeEffect(MobEffects.HUNGER);
   }

   @Override
   public boolean isFoil(ItemStack stack) {
      return true;
   }

   @Override
   public boolean isPiglinCurrency(ItemStack stack) {
      return true;
   }
}
