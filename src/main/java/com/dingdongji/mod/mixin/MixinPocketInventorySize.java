package com.dingdongji.mod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 铁砧 PocketInventory 物理容量与数据通路全部硬编码 12：
 * super(12)、List 反序列化 min(12)、STREAM_CODEC ByteBufCodecs.list(12)、
 * snapshot/syncChanges/tick（换护腿退回）/dropAll 的循环上界。
 * DDJ 耐候航天护腿+深口袋需要 24 栏，统一提升到 24。24 是超集容量，
 * 未激活的槽位永远为空，因此对 cap=0/6/12 的情况无副作用。
 * <p>
 * 注意一：修改构造器 super() 实参用 {@link ModifyArg}，且注入点在 super()
 * 之前时 handler 必须为 static。
 * 注意二：全局 defaultRequire=0，每个注入显式写 require 防止静默失败。
 */
@Mixin(targets = "dev.dubhe.anvilcraft.inventory.PocketInventory", remap = false)
public class MixinPocketInventorySize {
   /** 无参构造 super(12) → 24。 */
   @ModifyArg(
      method = "<init>()V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/SimpleContainer;<init>(I)V"
      ),
      index = 0,
      require = 1
   )
   private static int ddj$expandContainerSize(int original) {
      return Math.max(original, 24);
   }

   /** List 私有构造反序列化循环 Math.min(size,12) → 24。 */
   @ModifyConstant(
      method = "<init>(Ljava/util/List;)V",
      constant = @Constant(intValue = 12),
      require = 1
   )
   private static int ddj$expandListLoad(int original) {
      return Math.max(original, 24);
   }

   /** static 块 STREAM_CODEC ByteBufCodecs.list(12) → 24。 */
   @ModifyConstant(
      method = "<clinit>",
      constant = @Constant(intValue = 12),
      require = 1
   )
   private static int ddj$expandStreamCodec(int original) {
      return Math.max(original, 24);
   }

   /** snapshot() 循环上界 12 → 24。 */
   @ModifyConstant(
      method = "snapshot",
      constant = @Constant(intValue = 12),
      require = 1
   )
   private static int ddj$expandSnapshot(int original) {
      return Math.max(original, 24);
   }

   /** syncChanges() 中两处 12（lastSynced 长度判定 + 比较循环）→ 24。 */
   @ModifyConstant(
      method = "syncChanges",
      constant = @Constant(intValue = 12),
      require = 2
   )
   private static int ddj$expandSync(int original) {
      return Math.max(original, 24);
   }

   /** tick() 护腿更换时退回物品的循环上界 12 → 24。 */
   @ModifyConstant(
      method = "tick",
      constant = @Constant(intValue = 12),
      require = 1
   )
   private static int ddj$expandTick(int original) {
      return Math.max(original, 24);
   }

   /** dropAll() 循环上界 12 → 24。 */
   @ModifyConstant(
      method = "dropAll",
      constant = @Constant(intValue = 12),
      require = 1
   )
   private static int ddj$expandDrop(int original) {
      return Math.max(original, 24);
   }
}
