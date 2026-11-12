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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 铭化信标的铁砧概率转化：与本体腐化信标（诅咒金块金字塔底座 + 诅咒金锭）
 * 对称。信标底座为本体附魔金块金字塔（1~4 层）时，投入附魔金锭并让铁砧
 * 下落到信标上，按层数概率转化为铭化信标：1层 2%、2层 5%、3层 20%、
 * 4层 100%。金锭无论成功与否都会被消耗（与本体一致）。
 */
public class EnchantedBeaconConversionBehavior implements IAnvilBehavior {
   private static final Map<Integer, Float> CHANCE = Map.of(1, 0.02F, 2, 0.05F, 3, 0.2F, 4, 1.0F);
   private static Block enchantedGoldBlock;
   private static Item enchantedGoldIngot;

   @Override
   public boolean handle(Level level, BlockPos hitPos, BlockState hitState, float fallDistance, AnvilEvent.OnLand event) {
      int layers = 0;
      for (int i = 0; i < 4 && isFullLayer(level, hitPos, i); i++) {
         layers++;
      }
      if (layers == 0) {
         return false;
      }
      ItemEntity ingot = null;
      // 搜信标上方格：金锭丢在信标顶上（信标是整格方块，物品实体不会进入信标格内）
      for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(hitPos.above()))) {
         if (entity.getItem().is(enchantedGoldIngot())) {
            ingot = entity;
            break;
         }
      }
      if (ingot == null) {
         return false;
      }
      // 与本体一致：整组只消耗 1 个，金锭无论成败均消耗
      net.minecraft.world.item.ItemStack remaining = ingot.getItem().copy();
      remaining.shrink(1);
      ingot.setItem(remaining);
      if (level.getRandom().nextFloat() < CHANCE.get(layers)) {
         if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            com.dingdongji.mod.util.BeaconBaseHelper.convertToAnchored(serverLevel, hitPos);
         } else {
            level.setBlockAndUpdate(hitPos, ModBlocks.ANCHORED_BEACON.get().defaultBlockState());
         }
         return true;
      }
      return false;
   }

   /** 第 i 层（0 起）：紧贴信标下方第 i+1 层，边长 2i+3 的附魔金块方阵。 */
   private static boolean isFullLayer(Level level, BlockPos hitPos, int i) {
      int y = hitPos.getY() - i - 1;
      for (int x = hitPos.getX() - i - 1; x <= hitPos.getX() + i + 1; x++) {
         for (int z = hitPos.getZ() - i - 1; z <= hitPos.getZ() + i + 1; z++) {
            if (!level.getBlockState(new BlockPos(x, y, z)).is(enchantedGoldBlock())) {
               return false;
            }
         }
      }
      return true;
   }

   private static Block enchantedGoldBlock() {
      if (enchantedGoldBlock == null) {
         enchantedGoldBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("anvilcraft", "enchanted_gold_block"));
      }
      return enchantedGoldBlock;
   }

   private static Item enchantedGoldIngot() {
      if (enchantedGoldIngot == null) {
         enchantedGoldIngot = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("anvilcraft", "enchanted_gold_ingot"));
      }
      return enchantedGoldIngot;
   }

   /** 在 commonSetup 中调用（注册表冻结后）。
    *  不用 registerBehavior(Blocks.BEACON, ...)——本体腐化信标行为也注册到 BEACON，
    *  同 predicate 会互相覆盖。改用自定义 predicate 确保独立 key。 */
   public static void register() {
      IAnvilBehavior.registerBehavior(
         state -> state.is(Blocks.BEACON),
         new EnchantedBeaconConversionBehavior()
      );
   }
}
