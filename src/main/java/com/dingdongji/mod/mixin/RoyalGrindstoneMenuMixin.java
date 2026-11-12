package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModItems;
import dev.dubhe.anvilcraft.inventory.RoyalGrindstoneMenu;
import dev.dubhe.anvilcraft.inventory.RoyalGrindstoneMenu.RepairCostRecipeEntry;
import java.util.Iterator;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 皇家砂轮对重质金块（1 个重质金块 = 9 个金块 = 81 个金锭）的扩展。
 *
 * <p>本体逻辑（RoyalGrindstoneMenu#createResult）：
 * <ul>
 *   <li>金锭：每 1 个消除 1 点累积惩罚；祛诅咒限定金锭，每 {@code GOLD_PER_CURSE}=16
 *       个金锭消除 1 条诅咒；</li>
 *   <li>金块：每 1 个消除 9 点惩罚，但<b>不能</b>祛诅咒（祛诅咒分支限定金锭）。</li>
 * </ul>
 *
 * <p>因此直接把重质金块放入砂轮时它只消惩罚、从不祛诅咒（大批量诅咒祛除不了的根因）。
 * 本 mixin 在材料为重质金块时整体接管结果计算，把 1 个重质金块视为 81 个金锭当量：
 * <ul>
 *   <li>惩罚：每个重质金块消除 81 点，剩余不足 81 点的惩罚不动（整块粒度，不浪费）；</li>
 *   <li>诅咒：祛完惩罚后剩余的重质金块按金锭当量预算，每 16 当量消除 1 条诅咒
 *       （单个重质金块最多可消除 5 条），用于大批量祛诅咒。</li>
 * </ul>
 * 消耗与产物仍走本体的 {@code usedGold}（消耗材料物品数）→ 等量子重质诅咒金块。
 */
@Mixin(value = RoyalGrindstoneMenu.class, remap = false)
public abstract class RoyalGrindstoneMenuMixin {
   @Shadow
   @Final
   private Container repairToolSlots;

   @Shadow
   @Final
   private Container repairMaterialSlots;

   @Shadow
   @Final
   private Container resultMaterialSlots;

   @Shadow
   public int usedGold;

   @Shadow
   public int totalRepairCost;

   @Shadow
   public int totalCurseCount;

   @Shadow
   public int removedRepairCost;

   @Shadow
   public int removedCurseCount;

   @Shadow
   public Item repairMaterial;

   @Shadow
   public RepairCostRecipeEntry currentRecipe;

   @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
   private void dingdongji$heavyGoldCreateResult(CallbackInfoReturnable<ItemStack> cir) {
      ItemStack tool = this.repairToolSlots.getItem(0);
      ItemStack material = this.repairMaterialSlots.getItem(0);
      ItemStack resultMaterial = this.resultMaterialSlots.getItem(0);
      Item heavyGoldItem = (Item) ModItems.HEAVY_GOLD_BLOCK.get();
      // 非重质金块材料：交给本体逻辑
      if (material.getItem() != heavyGoldItem) {
         return;
      }

      this.totalRepairCost = 0;
      this.totalCurseCount = 0;
      this.removedRepairCost = 0;
      this.removedCurseCount = 0;
      this.repairMaterial = material.getItem();
      this.currentRecipe = RoyalGrindstoneMenu.REPAIR_COST_RECIPES.get(material.getItem());

      // 产物槽已有的不是对应的重质诅咒金块：不出结果（与本体一致）
      if (!resultMaterial.isEmpty() && this.currentRecipe != null
         && resultMaterial.getItem() != this.currentRecipe.item()) {
         this.usedGold = 0;
         cir.setReturnValue(ItemStack.EMPTY);
         return;
      }

      int repairCost = tool.getOrDefault(DataComponents.REPAIR_COST, 0);
      this.totalRepairCost = repairCost;
      ItemStack copy = tool.copy();
      var enchComp = copy.is(Items.ENCHANTED_BOOK)
         ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS;
      ItemEnchantments ench = copy.get(enchComp);
      ItemEnchantments.Mutable mutable = null;
      if (ench != null) {
         this.totalCurseCount = (int) ench.keySet().stream().filter(h -> h.is(EnchantmentTags.CURSE)).count();
         mutable = new ItemEnchantments.Mutable(ench);
      }

      if (tool.isEmpty() || material.isEmpty() || this.currentRecipe == null) {
         this.usedGold = 0;
         cir.setReturnValue(ItemStack.EMPTY);
         return;
      }

      // 1 个重质金块 = 81 个金锭当量
      int goldEquivalent = this.currentRecipe.count();
      int curseCost = RoyalGrindstoneMenu.GOLD_PER_CURSE;
      int available = Math.min(material.getCount() - resultMaterial.getCount(),
         this.currentRecipe.item().getDefaultMaxStackSize());
      if (available < 0) {
         available = 0;
      }

      // —— 先消累积惩罚：整块粒度，每个重块消 goldEquivalent 点，不足一块的惩罚不处理 ——
      int blocksForPenalty = Math.min(repairCost / goldEquivalent, available);
      this.removedRepairCost = Math.min(repairCost, goldEquivalent * blocksForPenalty);
      int remainingBlocks = available - blocksForPenalty;
      copy.set(DataComponents.REPAIR_COST, repairCost - this.removedRepairCost);
      int consumedBlocks = blocksForPenalty;

      // —— 剩余重块按金锭当量预算祛诅咒：每 curseCost 当量 1 条 ——
      if (mutable != null) {
         int affordableCurses = remainingBlocks * goldEquivalent / curseCost;
         int removedCurses = 0;
         Iterator<Holder<net.minecraft.world.item.enchantment.Enchantment>> it = mutable.keySet().iterator();
         while (it.hasNext() && removedCurses < affordableCurses) {
            Holder<net.minecraft.world.item.enchantment.Enchantment> holder = it.next();
            if (!holder.is(EnchantmentTags.CURSE)) {
               continue;
            }
            it.remove();
            removedCurses++;
         }
         this.removedCurseCount = removedCurses;
         if (removedCurses > 0) {
            // ceil(removedCurses*curseCost / goldEquivalent) 个整重块承载这些当量
            consumedBlocks += (removedCurses * curseCost + goldEquivalent - 1) / goldEquivalent;
         }
         copy.set(enchComp, mutable.toImmutable());
         if (copy.is(Items.ENCHANTED_BOOK) && !EnchantmentHelper.hasAnyEnchantments(copy)) {
            copy = copy.transmuteCopy(Items.BOOK);
         }
      }

      this.usedGold = consumedBlocks;
      cir.setReturnValue(copy);
   }
}
