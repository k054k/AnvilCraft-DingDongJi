package com.dingdongji.mod.anvil;

import com.dingdongji.mod.block.ModBlocks;
import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 重质金块（1 个重质金块 = 9 个普通金块）金字塔上信标的铁砧概率转化，
 * 与本体普通金块 {@code BeaconConversionBehavior} 完全同流程：
 *
 * <p>铁砧落到激活的信标上、信标上方有 1 个对应金锭（重质诅咒金块→诅咒金锭，
 * 重质附魔金块→附魔金锭），金锭必定消耗（整组只消耗 1 个），按层数掷骰：
 * <ul>
 *   <li>1 层 3×3 → 18%</li>
 *   <li>2 层 5×5 → 45%</li>
 *   <li>3 层 7×7 → 100%（仍需金锭）</li>
 * </ul>
 * 第 1 层是哪种重质金块就决定转化方向：重质诅咒金块→腐化信标，
 * 重质附魔金块→铭化信标。层数自下而上连续计算，中途混入其他方块则停在当前层数。
 *
 * <p>连续铺满 4 层（9×9）同种重质金块时由 {@code BeaconBlockEntityMixin}
 * 放上即刻转化，不需要铁砧与金锭，故本行为遇到完整 4 层时不处理。
 */
public class HeavyGoldBeaconConversionBehavior implements IAnvilBehavior {
   private static final Map<Integer, Float> CHANCE = Map.of(1, 0.18F, 2, 0.45F, 3, 1.0F);

   @Override
   public boolean handle(Level level, BlockPos hitPos, BlockState hitState, float fallDistance, AnvilEvent.OnLand event) {
      Block cursedHeavy = (Block) ModBlocks.CURSED_HEAVY_GOLD_BLOCK.get();
      Block enchantedHeavy = (Block) ModBlocks.ENCHANTED_HEAVY_GOLD_BLOCK.get();
      Block firstLayerBlock;
      if (isFullLayer(level, hitPos, 0, cursedHeavy)) {
         firstLayerBlock = cursedHeavy;
      } else if (isFullLayer(level, hitPos, 0, enchantedHeavy)) {
         firstLayerBlock = enchantedHeavy;
      } else {
         return false;
      }
      // 与本体一致：落点必须是激活中的信标（光柱非空）
      if (!(level.getBlockEntity(hitPos) instanceof BeaconBlockEntity beacon) || beacon.getBeamSections().isEmpty()) {
         return false;
      }
      // 自下而上连续计层，中途混入其他方块则停在当前层数
      int layers = 1;
      for (int i = 1; i < 4; i++) {
         if (!isFullLayer(level, hitPos, i, firstLayerBlock)) {
            break;
         }
         layers = i + 1;
      }
      // 完整 4 层走即刻转化（BeaconBlockEntityMixin），本行为不处理
      if (layers >= 4) {
         return false;
      }
      Item requiredIngot = firstLayerBlock == cursedHeavy ? ingot("cursed_gold_ingot") : ingot("enchanted_gold_ingot");
      for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(hitPos.above()))) {
         if (!entity.getItem().is(requiredIngot)) {
            continue;
         }
         // 与本体一致：整组只消耗 1 个，金锭无论成败均消耗
         ItemStack remaining = entity.getItem().copy();
         remaining.shrink(1);
         entity.setItem(remaining);
         if (level.getRandom().nextDouble() < CHANCE.get(layers)) {
            if (firstLayerBlock == cursedHeavy) {
               level.setBlockAndUpdate(
                  hitPos,
                  BuiltInRegistries.BLOCK.get(ResourceLocation.parse("anvilcraft:corrupted_beacon")).defaultBlockState()
               );
            } else if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
               com.dingdongji.mod.util.BeaconBaseHelper.convertToAnchored(serverLevel, hitPos);
            } else {
               level.setBlockAndUpdate(hitPos, ((Block) ModBlocks.ANCHORED_BEACON.get()).defaultBlockState());
            }
            return true;
         }
         return false;
      }
      return false;
   }

   /** 第 i 层（0 起）：紧贴信标下方第 i+1 层，边长 2i+3 的指定重质金块方阵。 */
   private static boolean isFullLayer(Level level, BlockPos hitPos, int i, Block block) {
      int y = hitPos.getY() - i - 1;
      for (int x = hitPos.getX() - i - 1; x <= hitPos.getX() + i + 1; x++) {
         for (int z = hitPos.getZ() - i - 1; z <= hitPos.getZ() + i + 1; z++) {
            if (!level.getBlockState(new BlockPos(x, y, z)).is(block)) {
               return false;
            }
         }
      }
      return true;
   }

   private static Item ingot(String path) {
      return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("anvilcraft", path));
   }

   /** 在 commonSetup 中调用（注册表冻结后）。
    *  不用 registerBehavior(Blocks.BEACON, ...)——本体腐化信标行为也注册到 BEACON，
    *  同 predicate 会互相覆盖。改用自定义 predicate 确保独立 key。 */
   public static void register() {
      IAnvilBehavior.registerBehavior(
         state -> state.is(Blocks.BEACON),
         new HeavyGoldBeaconConversionBehavior()
      );
   }
}
