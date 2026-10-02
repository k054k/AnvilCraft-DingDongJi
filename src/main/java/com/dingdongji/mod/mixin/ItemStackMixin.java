package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.item.component.CreateTemplateMode;
import com.dingdongji.mod.item.component.PouchCapacityComponent;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ItemStack.class})
public abstract class ItemStackMixin {
   @Inject(
      method = {"is(Lnet/minecraft/world/item/Item;)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onIsItem(Item item, CallbackInfoReturnable<Boolean> cir) {
      ItemStack self = (ItemStack)(Object)this;
      ResourceLocation targetId = BuiltInRegistries.ITEM.getKey(item);
      if (targetId.equals(ResourceLocation.parse("anvilcraft:eight_to_one_smithing_template"))) {
         ResourceLocation selfId = BuiltInRegistries.ITEM.getKey(self.getItem());
         if (selfId.equals(ResourceLocation.parse("dingdongji:create_template"))) {
            CreateTemplateMode mode = (CreateTemplateMode)self.get((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get());
            if (mode != null && "delta".equals(mode.mode())) {
               cir.setReturnValue(true);
            }
         }
      }
   }

   /**
    * 口袋返还（总兼容）：覆写 {@code IItemStackExtension#hasCraftingRemainingItem} 的接口默认实现。
    * <p>
    * ItemStack 类本体未声明该方法（继承自 NeoForge 扩展接口的 default 方法），
    * mixin 将本方法合并进 ItemStack 后即为唯一实现，所有调用点统一生效：
    * 原版工作台/2×2 格、原版自动合成器、Tom's 简易存储合成终端
    * （走 {@code Recipe#getRemainingItems}）以及机械动力机械合成器
    * （直接调 {@code ItemStack#hasCraftingRemainingItem}，铁桶同机制）。
    * <p>
    * 带口袋组件的护腿声明"有合成剩余物"；其余物品保持原版判定
    * （委托 {@code Item#hasCraftingRemainingItem(ItemStack)}，不递归）。
    */
   public boolean hasCraftingRemainingItem() {
      ItemStack self = (ItemStack)(Object)this;
      if (self.has((DataComponentType)ModComponents.POUCH_CAPACITY.get())) {
         return true;
      }
      return self.getItem().hasCraftingRemainingItem(self);
   }

   /**
    * 口袋返还（总兼容）：覆写 {@code IItemStackExtension#getCraftingRemainingItem} 的接口默认实现。
    * <p>
    * 优先保留物品自身声明的剩余物（铁桶→空桶、玻璃瓶→空瓶等，不覆盖其他模组逻辑）；
    * 无自身剩余物且带口袋组件时，按组件容量返还对应口袋
    * （容量 ≥12 → 深口袋，否则小口袋），与铁桶同机制，见
    * {@link #hasCraftingRemainingItem()} 的兼容性说明。
    */
   public ItemStack getCraftingRemainingItem() {
      ItemStack self = (ItemStack)(Object)this;
      ItemStack original = self.getItem().getCraftingRemainingItem(self);
      if (!original.isEmpty()) {
         return original;
      }
      PouchCapacityComponent pouch = (PouchCapacityComponent)self.get((DataComponentType)ModComponents.POUCH_CAPACITY.get());
      if (pouch == null) {
         return ItemStack.EMPTY;
      }
      Item returned = pouch.capacity() >= 12 ? ModItems.BIG_POUCH.get() : ModItems.SMALL_POUCH.get();
      return new ItemStack(returned);
   }
}
