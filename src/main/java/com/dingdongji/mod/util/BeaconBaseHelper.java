package com.dingdongji.mod.util;

import com.dingdongji.mod.block.ModBlocks;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 铭化信标相关的底座判定与转化工具（普通类，供 Mixin / 铁砧行为 / 注魔管理器共用，
 * 避免在 Mixin 之间互相引用私有方法）。
 */
public class BeaconBaseHelper {
   /** 本体附魔金块底座 GUI 转化概率（保持本体腐化信标原表）：1 层 2% / 2 层 5% / 3 层 20% / 4 层 100%。 */
   public static final Map<Integer, Float> GUI_CONVERT_CHANCE = Map.of(
      1, 0.02F, 2, 0.05F, 3, 0.20F, 4, 1.0F
   );

   /** 重质附魔金块底座 GUI 转化概率（用户明确参数）：1 层 18% / 2 层 45% / 3 层 100% / 4 层 100%。 */
   public static final Map<Integer, Float> HEAVY_GUI_CONVERT_CHANCE = Map.of(
      1, 0.18F, 2, 0.45F, 3, 1.0F, 4, 1.0F
   );

   private static Block enchantedGoldBlock;
   private static Block cursedGoldBlock;
   private static Block corruptedBeacon;

   /** 本体附魔金块。 */
   public static Block enchantedGoldBlock() {
      if (enchantedGoldBlock == null) {
         enchantedGoldBlock = BuiltInRegistries.BLOCK.get(
            ResourceLocation.fromNamespaceAndPath("anvilcraft", "enchanted_gold_block")
         );
      }
      return enchantedGoldBlock;
   }

   /** 本体诅咒金块。 */
   public static Block cursedGoldBlock() {
      if (cursedGoldBlock == null) {
         cursedGoldBlock = BuiltInRegistries.BLOCK.get(
            ResourceLocation.fromNamespaceAndPath("anvilcraft", "cursed_gold_block")
         );
      }
      return cursedGoldBlock;
   }

   /** 本体腐化信标。 */
   public static Block corruptedBeacon() {
      if (corruptedBeacon == null) {
         corruptedBeacon = BuiltInRegistries.BLOCK.get(
            ResourceLocation.fromNamespaceAndPath("anvilcraft", "corrupted_beacon")
         );
      }
      return corruptedBeacon;
   }

   /**
    * 诅咒侧底座层数（诅咒金块 或 重质诅咒金块，同层可混用，逐块 OR 判定，
    * 与腐化信标光柱的扩展判定一致）。第 1 层不满返回 0。
    * 供 GUI 诅咒金锭转化在「本体纯诅咒金块统计为 0」时补充判定重质底座。
    */
   public static int countCursedOrHeavyLayers(Level level, BlockPos beaconPos) {
      Block gold = cursedGoldBlock();
      Block heavy = (Block) ModBlocks.CURSED_HEAVY_GOLD_BLOCK.get();
      int layers = 0;
      for (int i = 1; i <= 4; i++) {
         BlockPos layerCenter = beaconPos.below(i);
         boolean full = true;
         outer:
         for (int dx = -i; dx <= i && full; dx++) {
            for (int dz = -i; dz <= i; dz++) {
               BlockState state = level.getBlockState(layerCenter.offset(dx, 0, dz));
               if (!state.is(gold) && !state.is(heavy)) {
                  full = false;
                  break outer;
               }
            }
         }
         if (!full) {
            break;
         }
         layers = i;
      }
      return layers;
   }

   /** 本体重质附魔金块。 */
   public static Block enchantedHeavyGoldBlock() {
      return (Block) ModBlocks.ENCHANTED_HEAVY_GOLD_BLOCK.get();
   }

   /**
    * 从信标正下方第 1 层（3×3）起，向上连续铺满指定方块的金字塔层数：
    * 第 i 层边长 2i+1（i=1..4），任一层不满即停止计数。
    */
   public static int countLayers(Level level, BlockPos beaconPos, Block block) {
      int layers = 0;
      for (int i = 1; i <= 4; i++) {
         if (!isLayerFull(level, beaconPos.below(i), i, block)) {
            break;
         }
         layers = i;
      }
      return layers;
   }

   /**
    * 附魔侧底座层数：第 1 层（3×3）必须全为附魔金块或重质附魔金块（可混用），
    * 第 2~4 层取两种方块中该层铺满数更多的那种继续向上判定。
    * 返回 1..4，第 1 层不满返回 0。
    */
   public static int countEnchantedLayers(Level level, BlockPos beaconPos) {
      if (!hasEnchantedBase(level, beaconPos)) {
         return 0;
      }
      // 第 1 层已铺满，从第 2 层起取两种方块中该层全满的那种继续计数
      Block heavy = enchantedHeavyGoldBlock();
      Block gold = enchantedGoldBlock();
      int layers = 1;
      for (int i = 2; i <= 4; i++) {
         boolean heavyFull = isLayerFull(level, beaconPos.below(i), i, heavy);
         boolean goldFull = isLayerFull(level, beaconPos.below(i), i, gold);
         if (heavyFull) {
            layers = i;
         } else if (goldFull) {
            layers = i;
         } else {
            break;
         }
      }
      return layers;
   }

   /** 第 i 层：以 center 为中心、半径 i 的方形（3×3 对应 i=1）全为指定方块。 */
   public static boolean isLayerFull(Level level, BlockPos center, int radius, Block block) {
      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            if (!level.getBlockState(center.offset(dx, 0, dz)).is(block)) {
               return false;
            }
         }
      }
      return true;
   }

   /** 纯附魔金块底座层数（不统计重质），返回 0..4，第 1 层不满则 0。 */
   public static int countPureEnchantedGoldLayers(Level level, BlockPos beaconPos) {
      Block gold = enchantedGoldBlock();
      int layers = 0;
      for (int i = 1; i <= 4; i++) {
         if (!isLayerFull(level, beaconPos.below(i), i, gold)) {
            break;
         }
         layers = i;
      }
      return layers;
   }

   /** 纯重质附魔金块底座层数（不统计普通金块），返回 0..4，第 1 层不满则 0。 */
   public static int countHeavyEnchantedLayers(Level level, BlockPos beaconPos) {
      Block heavy = enchantedHeavyGoldBlock();
      int layers = 0;
      for (int i = 1; i <= 4; i++) {
         if (!isLayerFull(level, beaconPos.below(i), i, heavy)) {
            break;
         }
         layers = i;
      }
      return layers;
   }

   /** 底座 3×3 全为附魔重质金块或本体附魔金块（可混用，与渲染器条件一致）。 */
   public static boolean hasEnchantedBase(Level level, BlockPos beaconPos) {
      BlockPos baseCenter = beaconPos.below();
      Block heavy = enchantedHeavyGoldBlock();
      Block gold = enchantedGoldBlock();
      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            BlockState state = level.getBlockState(baseCenter.offset(dx, 0, dz));
            if (!state.is(heavy) && !state.is(gold)) {
               return false;
            }
         }
      }
      return true;
   }

   /**
    * 注魔/祛咒周期（秒）：第 1 层全重质附魔金块按重质表，否则按附魔金表；
    * 两种方块混用（第一层非单一满铺）返回 0，表示完全不注魔。
    * 每经过一个周期，光柱内物品被注魔一次并祛咒一次。
    * 附魔金块：120 / 100 / 80 / 60 秒；重质附魔金块：80 / 55 / 35 / 20 秒
    * （1 重质块=9 金块，重质线靠材料密度压缩占地并突破金块 4 层 60 秒上限）。
    */
   public static int infusionSeconds(Level level, BlockPos beaconPos) {
      Block heavy = enchantedHeavyGoldBlock();
      Block gold = enchantedGoldBlock();
      Block firstLayerBlock = null;
      if (isLayerFull(level, beaconPos.below(), 1, heavy)) {
         firstLayerBlock = heavy;
      } else if (isLayerFull(level, beaconPos.below(), 1, gold)) {
         firstLayerBlock = gold;
      } else {
         return 0;
      }
      int layers = countLayers(level, beaconPos, firstLayerBlock);
      // 重质表非等差（80/55/35/20），直接查表；金表等差 140-20*layers
      if (firstLayerBlock == heavy) {
         int[] heavyTable = {0, 80, 55, 35, 20};
         return heavyTable[layers];
      }
      return 140 - 20 * layers;
   }

   /** 与铭化信标渲染器扫描条件一致；返回第一个遮光（或白名单）方块的 Y，无遮挡时为建筑上限。 */
   public static int scanBeamTop(Level level, BlockPos pos) {
      int maxY = level.getMaxBuildHeight();
      BlockPos.MutableBlockPos cursor = pos.above().mutable();
      while (cursor.getY() <= maxY) {
         BlockState state = level.getBlockState(cursor);
         // 时定白名单方块可能不遮光（如过冷浮霜金属块 noOcclusion），直接命中
         if (BeamLockTracker.isAnchorable(state)) {
            return cursor.getY();
         }
         if (state.getBeaconColorMultiplier(level, cursor, pos) == null
            && state.getLightBlock(level, cursor) >= 15
            && !state.is(Blocks.BEDROCK)) {
            return cursor.getY();
         }
         cursor.move(net.minecraft.core.Direction.UP);
      }
      return maxY;
   }

   /**
    * 将普通信标转化为铭化信标：替换方块、转晴，并播放附魔符文被吸入的转化粒子。
    */
   public static void convertToAnchored(ServerLevel level, BlockPos pos) {
      dev.dubhe.anvilcraft.util.TriggerUtil.convertBeacon(level, pos);
      level.setBlockAndUpdate(pos, ((Block) ModBlocks.ANCHORED_BEACON.get()).defaultBlockState());
      if (level.getServer().getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).get()) {
         level.setWeatherParameters(ServerLevel.RAIN_DURATION.sample(level.getRandom()), 0, false, false);
      }
      BeaconConversionFx.spawn(level, pos);
   }

   /**
    * 将普通信标转化为腐化信标：与本体 BeaconMenuMixin 掷骰成功后的流程逐行一致
    * （进度触发 TriggerUtil#convertBeacon → 替换方块 → 雷暴天气）。
    */
   public static void convertToCorrupted(ServerLevel level, BlockPos pos) {
      dev.dubhe.anvilcraft.util.TriggerUtil.convertBeacon(level, pos);
      level.setBlockAndUpdate(pos, corruptedBeacon().defaultBlockState());
      if (level.getServer().getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).get()) {
         level.setWeatherParameters(
            0, ServerLevel.THUNDER_DURATION.sample(level.getRandom()), true, true
         );
      }
   }
}
