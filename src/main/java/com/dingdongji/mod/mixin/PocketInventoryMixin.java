package com.dingdongji.mod.mixin;

import com.dingdongji.mod.util.AnvilCraftCompat;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 0.1.0 合并类：铁砧 PocketInventory 的容量动态化（DDJ 护腿 24 栏）。
 * 物理容量与数据通路全部硬编码 12：super(12)、List 反序列化、STREAM_CODEC、
 * snapshot/syncChanges/tick/dropAll 循环上界，统一提升到 24（超集容量，
 * 未激活槽位永远为空，对 cap=0/6/12 无副作用）。
 * capacity() 改为 AnvilCraftCompat 动态计算。
 */
@Mixin(targets = "dev.dubhe.anvilcraft.inventory.PocketInventory", remap = false)
public class PocketInventoryMixin {

   @Inject(method = "capacity", at = @At("RETURN"), cancellable = true, require = 1)
   private static void ddj$extendCapacity(Player player, CallbackInfoReturnable<Integer> cir) {
      cir.setReturnValue(AnvilCraftCompat.getPocketCapacity(player));
   }

   @ModifyArg(
      method = "<init>()V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/SimpleContainer;<init>(I)V"),
      index = 0,
      require = 1
   )
   private static int ddj$expandContainerSize(int original) {
      return Math.max(original, 24);
   }

   @ModifyConstant(method = "<init>(Ljava/util/List;)V", constant = @Constant(intValue = 12), require = 1)
   private static int ddj$expandListLoad(int original) {
      return Math.max(original, 24);
   }

   @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 12), require = 1)
   private static int ddj$expandStreamCodec(int original) {
      return Math.max(original, 24);
   }

   @ModifyConstant(method = "snapshot", constant = @Constant(intValue = 12), require = 1)
   private static int ddj$expandSnapshot(int original) {
      return Math.max(original, 24);
   }

   @ModifyConstant(method = "syncChanges", constant = @Constant(intValue = 12), require = 2)
   private static int ddj$expandSync(int original) {
      return Math.max(original, 24);
   }

   @ModifyConstant(method = "tick", constant = @Constant(intValue = 12), require = 1)
   private static int ddj$expandTick(int original) {
      return Math.max(original, 24);
   }

   @ModifyConstant(method = "dropAll", constant = @Constant(intValue = 12), require = 1)
   private static int ddj$expandDrop(int original) {
      return Math.max(original, 24);
   }
}
