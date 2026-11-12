package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModItems;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.inventory.TranscendenceGrindstoneMenu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;

/**
 * 超限合金砂轮对重质金块的扩展支持。
 *
 * <p>与皇家砂轮不同，超限砂轮的 {@code isGold} 已经同时认金锭和金块，
 * 但重质金块不在其列。本 mixin 三条路径：
 * <ol>
 *   <li>扩展 {@code isGold} 把重质金块也算作有效金材料（放行放入 + 金模式判定）。</li>
 *   <li>接管 {@code createGoldResult}：1 重质金块 = 81 金锭当量，先整块粒度消惩罚，
 *       剩余按 {@code GOLD_PER_CURSE}=16 当量/条祛诅咒。</li>
 *   <li>修正 {@code takeGoldResult} 产物：重质金块消耗后产出诅咒重质金块
 *       （而非被误判为诅咒金锭）。</li>
 * </ol>
 */
@Mixin(value = TranscendenceGrindstoneMenu.class, remap = false)
public abstract class TranscendenceGrindstoneMenuMixin {

   @Shadow @Final private Container source;
   @Shadow @Final private Container modifier;
   @Shadow private int usedGold;
   @Shadow private int totalRepairCost;
   @Shadow private int totalCurseCount;
   @Shadow private int removedRepairCost;
   @Shadow private int removedCurseCount;

   private static final int GOLD_PER_CURSE = 16;
   private static final int GOLD_EQUIVALENT = 81; // 1 重质金块 = 9 金块 = 81 金锭

   /** 扩展 isGold：重质金块也算有效金材料。 */
   @ModifyReturnValue(
      method = "isGold(Lnet/minecraft/world/item/ItemStack;)Z",
      at = @At("RETURN")
   )
   private static boolean ddj$heavyGoldIsGold(boolean original, ItemStack stack) {
      if (original) {
         return true;
      }
      return stack.is((Item) ModItems.HEAVY_GOLD_BLOCK.get());
   }

   /** 接管 createGoldResult：重质金块按 81 当量计算。 */
   @Inject(method = "createGoldResult", at = @At("HEAD"), cancellable = true)
   private void ddj$heavyGoldCreateResult(CallbackInfoReturnable<ItemStack> cir) {
      ItemStack tool = this.source.getItem(0);
      ItemStack material = this.modifier.getItem(0);
      Item heavyGoldItem = (Item) ModItems.HEAVY_GOLD_BLOCK.get();
      if (material.getItem() != heavyGoldItem) {
         return;
      }

      // 重置统计
      this.totalRepairCost = 0;
      this.totalCurseCount = 0;
      this.removedRepairCost = 0;
      this.removedCurseCount = 0;

      int repairCost = tool.getOrDefault(DataComponents.REPAIR_COST, 0);
      this.totalRepairCost = repairCost;
      this.totalCurseCount = countCurses(tool);

      ItemStack copy = tool.copy();
      int materialCount = material.getCount();

      // —— 先消累积惩罚：整块粒度，每个重质金块消 81 点 ——
      int blocksForPenalty = Math.min(repairCost / GOLD_EQUIVALENT, materialCount);
      this.removedRepairCost = Math.min(repairCost, GOLD_EQUIVALENT * blocksForPenalty);
      int remainingBlocks = materialCount - blocksForPenalty;
      copy.set(DataComponents.REPAIR_COST, repairCost - this.removedRepairCost);
      int consumedBlocks = blocksForPenalty;

      // —— 剩余重质金块按金锭当量预算祛诅咒：每 16 当量 1 条 ——
      if (remainingBlocks > 0 && this.totalCurseCount > 0) {
         int affordableCurses = Math.min(
            this.totalCurseCount,
            remainingBlocks * GOLD_EQUIVALENT / GOLD_PER_CURSE
         );
         int removed = removeCurses(copy, affordableCurses);
         this.removedCurseCount = removed;
         if (removed > 0) {
            consumedBlocks += (removed * GOLD_PER_CURSE + GOLD_EQUIVALENT - 1) / GOLD_EQUIVALENT;
         }
      }

      this.usedGold = consumedBlocks;

      // 附魔书祛完诅咒后变回普通书
      if (copy.is(Items.ENCHANTED_BOOK) && !EnchantmentHelper.hasAnyEnchantments(copy)) {
         copy = copy.transmuteCopy(Items.BOOK);
      }

      cir.setReturnValue(copy);
   }

   /** 修正 takeGoldResult 产物：重质金块消耗 → 诅咒重质金块。
    *  原方法在材料不是金块时产物固定为诅咒金锭，这里完整接管重质路径。 */
   @Inject(method = "takeGoldResult", at = @At("HEAD"), cancellable = true)
   private void ddj$heavyGoldTakeResult(net.minecraft.world.entity.player.Player player, CallbackInfo ci) {
      ItemStack material = this.modifier.getItem(0);
      Item heavyGoldItem = (Item) ModItems.HEAVY_GOLD_BLOCK.get();
      if (material.getItem() != heavyGoldItem) {
         return;
      }

      // 清除输入并按 usedGold 消耗材料
      this.source.setItem(0, ItemStack.EMPTY);
      consumeModifier(this.usedGold);

      // 服务端侧把产物（诅咒重质金块）放回玩家背包
      if (!player.level().isClientSide && this.usedGold > 0) {
         Item cursedHeavy = (Item) ModItems.CURSED_HEAVY_GOLD_BLOCK.get();
         player.getInventory().placeItemBackInInventory(
            new ItemStack(cursedHeavy, this.usedGold)
         );
      }
      ci.cancel();
   }

   // Shadow 辅助方法
   @Shadow
   private void consumeModifier(int count) {}

   @Shadow
   private int countCurses(ItemStack stack) { return 0; }

   @Shadow
   private int removeCurses(ItemStack stack, int count) { return 0; }
}
