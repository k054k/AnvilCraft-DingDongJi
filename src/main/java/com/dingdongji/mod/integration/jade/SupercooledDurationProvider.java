package com.dingdongji.mod.integration.jade;

import com.dingdongji.mod.block.SupercooledFrostMetalBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.IBlockComponentProvider;

/**
 * 过冷浮霜金属块的 Jade 倒计时显示（服务端数据 + 客户端 tooltip 一体）。
 *
 * <p>服务端侧：把 BlockState 的 {@code frost_seconds}（剩余秒）写入 Jade
 * 服务端数据键 {@code ddj_duration}（单位 tick）。本方块已无 BlockEntity，
 * 本体 HeatableBlockProvider 不会命中，直接用官方 Jade provider API，
 * 无需任何改名回调。
 *
 * <p>客户端侧：读 {@code ddj_duration} 整秒显示，标点沿用本体（45" / 1'05）。
 */
public final class SupercooledDurationProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
   public static final SupercooledDurationProvider INSTANCE = new SupercooledDurationProvider();

   private static final MutableComponent MINUTE_MARK = Component.literal("'").withStyle(ChatFormatting.GRAY);
   private static final MutableComponent SECOND_MARK = Component.literal("\"").withStyle(ChatFormatting.GRAY);

   private SupercooledDurationProvider() {
   }

   @Override
   public void appendServerData(CompoundTag data, BlockAccessor accessor) {
      int seconds = accessor.getBlockState().getValue(SupercooledFrostMetalBlock.FROST_SECONDS);
      data.putInt("ddj_duration", seconds * 20);
   }

   @Override
   public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
      // 温度等级：过冷（与本体超温余烬金属块"温度等级：超温"同构）
      tooltip.add(Component.translatable(
         "tooltip.anvilcraft.heat.tier",
         Component.translatable("tooltip.dingdongji.heat.tier.supercooled")
      ));
      CompoundTag data = accessor.getServerData();
      if (data.contains("ddj_duration")) {
         tooltip.add(Component.translatable(
            "tooltip.anvilcraft.heat.duration",
            buildWholeSeconds(data.getInt("ddj_duration"))
         ));
      }
   }

   /**
    * tick → 整秒，标点与本体双参 toFormattedTime 完全一致（去掉百分秒）：
    * 不足 1 分钟：{@code 45"}；1 分钟以上：{@code 1'05}（整 0 秒为 {@code 1'}）。
    * 数字与标点均显示为灰色。
    */
   private static Component buildWholeSeconds(int ticks) {
      int totalSeconds = ticks / 20;
      int minutes = totalSeconds / 60;
      int seconds = totalSeconds % 60;
      MutableComponent component = Component.empty();
      if (minutes > 0) {
         component.append(Component.literal(String.valueOf(minutes)).withStyle(ChatFormatting.GRAY));
         component.append(MINUTE_MARK.copy());
         if (seconds > 0) {
            component.append(Component.literal(String.format("%02d", seconds)).withStyle(ChatFormatting.GRAY));
         }
      } else {
         component.append(Component.literal(String.valueOf(seconds)).withStyle(ChatFormatting.GRAY));
         component.append(SECOND_MARK.copy());
      }
      return component;
   }

   @Override
   public ResourceLocation getUid() {
      return ResourceLocation.fromNamespaceAndPath("dingdongji", "supercooled_duration");
   }
}
