package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModItems;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 重质附魔金块以物易物产出放大：1 个重质附魔金块 = 9 个附魔金块，每个附魔金块
 * 相当于 4 份交易（4×9=36）。重质附魔金块在其物品类重写 isPiglinCurrency 为 true，
 * 猪灵才会走拾取→欣赏→交易全流程（喜爱标签本身只让猪灵拾取垂涎，不触发交易）。
 */
@Mixin(PiglinAi.class)
public abstract class PiglinAiMixin {
   /** 一次交易放大的总份数（含原本的 1 份）。 */
   private static final int DINGDONGJI_BARTER_TIMES = 36;

   @Inject(method = "getBarterResponseItems", at = @At("RETURN"), cancellable = true)
   private static void dingdongji$amplifyBarter(Piglin piglin, CallbackInfoReturnable<ObjectArrayList<ItemStack>> cir) {
      if (!piglin.getOffhandItem().is((Item) ModItems.ENCHANTED_HEAVY_GOLD_BLOCK.get())) {
         return;
      }
      ObjectArrayList<ItemStack> original = cir.getReturnValue();
      ObjectArrayList<ItemStack> amplified = new ObjectArrayList<>(original.size() * DINGDONGJI_BARTER_TIMES);
      for (ItemStack stack : original) {
         for (int i = 0; i < DINGDONGJI_BARTER_TIMES; i++) {
            amplified.add(i == 0 ? stack : stack.copy());
         }
      }
      cir.setReturnValue(amplified);
   }
}
