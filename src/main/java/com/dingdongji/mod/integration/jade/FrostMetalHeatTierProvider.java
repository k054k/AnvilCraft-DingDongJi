package com.dingdongji.mod.integration.jade;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * 本体常温浮霜金属块（{@code anvilcraft:frost_metal_block}）在叮咚叽环境下的
 * Jade 温度等级显示：固定一行"温度等级：常温"。
 *
 * <p>本体的 HeatableBlockProvider 只服务于可加热方块（超温余烬金属块等），
 * 普通浮霜金属块不会显示任何温度行；本 provider 仅注册客户端组件、无需
 * 服务端数据，语言键直接沿用本体 {@code tooltip.anvilcraft.heat.tier(.normal)}，
 * 与本体/本附属过冷块的措辞完全一致。
 */
public final class FrostMetalHeatTierProvider implements IBlockComponentProvider {
   public static final FrostMetalHeatTierProvider INSTANCE = new FrostMetalHeatTierProvider();

   private FrostMetalHeatTierProvider() {
   }

   @Override
   public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
      tooltip.add(Component.translatable(
         "tooltip.anvilcraft.heat.tier",
         Component.translatable("tooltip.anvilcraft.heat.tier.normal")
      ));
   }

   @Override
   public ResourceLocation getUid() {
      return ResourceLocation.fromNamespaceAndPath("dingdongji", "frost_metal_heat_tier");
   }
}
