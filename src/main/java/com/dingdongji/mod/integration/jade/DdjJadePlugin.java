package com.dingdongji.mod.integration.jade;

import com.dingdongji.mod.block.CursedHeavyGoldBlock;
import com.dingdongji.mod.block.SupercooledFrostMetalBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade（玉）集成入口。
 *
 * <p>Jade 未安装时本类不会被扫描加载。
 */
@WailaPlugin
public class DdjJadePlugin implements IWailaPlugin {
   @Override
   public void register(IWailaCommonRegistration registration) {
      // 过冷浮霜金属块：服务端把 frost_seconds 写入 ddj_duration 键
      registration.registerBlockDataProvider(
         SupercooledDurationProvider.INSTANCE, SupercooledFrostMetalBlock.class);
   }

   @Override
   public void registerClient(IWailaClientRegistration registration) {
      // 重质诅咒金块：头顶显示"附魔能力：-2"（与本体诅咒金块同款）
      registration.registerBlockComponent(
         CursedHeavyGoldEnchantPowerProvider.INSTANCE, CursedHeavyGoldBlock.class);
      // 过冷浮霜金属块：倒计时整秒显示（本体 HeatableBlockProvider 的双参
      // 格式尾部带百分秒，如 58"95，故自写 provider 用单参格式）
      registration.registerBlockComponent(
         SupercooledDurationProvider.INSTANCE, SupercooledFrostMetalBlock.class);
      // 本体常温浮霜金属块：补一行"温度等级：常温"（本体仅对可加热方块显示温度行）
      registration.registerBlockComponent(
         FrostMetalHeatTierProvider.INSTANCE, dev.dubhe.anvilcraft.block.FrostMetalBlock.class);
   }
}
