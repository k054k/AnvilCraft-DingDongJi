package com.dingdongji.mod.integration.jade;

import java.text.DecimalFormat;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

/**
 * 重质诅咒金块的 Jade 显示：附魔能力（与本体诅咒金块同款显示，
 * 数值取自方块的 getEnchantPowerBonus，重质块的负值比单个诅咒金块更大）。
 */
public final class CursedHeavyGoldEnchantPowerProvider implements IBlockComponentProvider {
   public static final CursedHeavyGoldEnchantPowerProvider INSTANCE = new CursedHeavyGoldEnchantPowerProvider();
   private static final DecimalFormat FORMAT = new DecimalFormat();

   private CursedHeavyGoldEnchantPowerProvider() {
   }

   @Override
   public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
      float power = accessor.getBlockState()
         .getEnchantPowerBonus(accessor.getLevel(), accessor.getPosition());
      tooltip.add(Component.translatable("jade.ench_power", IThemeHelper.get().info(FORMAT.format(power))));
   }

   @Override
   public ResourceLocation getUid() {
      return ResourceLocation.fromNamespaceAndPath("dingdongji", "cursed_heavy_gold_enchant_power");
   }
}