package com.dingdongji.mod.anvil;

import com.dingdongji.mod.block.ModBlocks;
import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 过冷浮霜版超限合金转化：充能中子锭（1 个）压入过冷浮霜金属块。
 * 与本体"充能中子锭压入超温余烬金属块"（TranscendiumBehavior）逐分支一致：
 *   n=0       → 4 超限合金锭；
 *   1≤n≤10    → 概率返还中子锭 + 4 超限合金锭 + 3n 超限合金粒；
 *   11≤n≤14   → 返还中子锭 + 4 超限合金锭 + 3n 超限合金粒；
 *   n=15      → 返还中子锭 + 原位变超限合金块；
 *   n≥16      → 返还中子锭 + n 超限合金粒 + 原位变超限合金块。
 * n 为充能中子锭上的附魔条目数；底座无论如何消耗（与本体一致）。
 */
public class SupercooledTranscendiumBehavior implements IAnvilBehavior {

   @Override
   public boolean handle(Level level, BlockPos hitPos, BlockState hitState, float fallDistance, AnvilEvent.OnLand event) {
      if (!hitState.is((Block) ModBlocks.SUPERCOOLED_FROST_METAL_BLOCK.get())) {
         return false;
      }
      var entities = level.getEntitiesOfClass(ItemEntity.class, new AABB(hitPos.above()));
      if (entities.isEmpty()) {
         return false;
      }
      ItemEntity itemEntity = entities.getFirst();
      ItemStack stack = itemEntity.getItem();
      if (!stack.is(acItem("charged_neutronium_ingot")) || stack.getCount() != 1) {
         return false;
      }
      ItemEnchantments ench = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
      int n = ench.size();
      level.setBlockAndUpdate(hitPos, Blocks.AIR.defaultBlockState());
      if (itemEntity.isAlive()) {
         itemEntity.discard();
      }
      if (n == 0) {
         this.spawnItem(level, hitPos, acItem("transcendium_ingot"), 4);
      } else if (n <= 10) {
         // 与本体一致（1.6.0 硬编码）：每级附魔 10% 概率返还中子锭
         if (level.getRandom().nextDouble() < (float) (10 * n) / 100.0F) {
            this.spawnItem(level, hitPos, acItem("neutronium_ingot"), 1);
         }
         this.spawnItem(level, hitPos, acItem("transcendium_ingot"), 4);
         this.spawnItem(level, hitPos, acItem("transcendium_nugget"), 3 * n);
      } else if (n <= 14) {
         this.spawnItem(level, hitPos, acItem("neutronium_ingot"), 1);
         this.spawnItem(level, hitPos, acItem("transcendium_ingot"), 4);
         this.spawnItem(level, hitPos, acItem("transcendium_nugget"), 3 * n);
      } else if (n == 15) {
         this.spawnItem(level, hitPos, acItem("neutronium_ingot"), 1);
         level.setBlockAndUpdate(hitPos, acBlock("transcendium_block").defaultBlockState());
      } else {
         this.spawnItem(level, hitPos, acItem("neutronium_ingot"), 1);
         this.spawnItem(level, hitPos, acItem("transcendium_nugget"), n);
         level.setBlockAndUpdate(hitPos, acBlock("transcendium_block").defaultBlockState());
      }
      return true;
   }

   private void spawnItem(Level level, BlockPos pos, Item item, int count) {
      level.addFreshEntity(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(item, count)));
   }

   private static Item acItem(String path) {
      return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("anvilcraft", path));
   }

   private static Block acBlock(String path) {
      return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("anvilcraft", path));
   }

   /** 在 commonSetup 中调用（注册表冻结后）。 */
   public static void register() {
      IAnvilBehavior.registerBehavior((Block) ModBlocks.SUPERCOOLED_FROST_METAL_BLOCK.get(), new SupercooledTranscendiumBehavior());
   }
}
