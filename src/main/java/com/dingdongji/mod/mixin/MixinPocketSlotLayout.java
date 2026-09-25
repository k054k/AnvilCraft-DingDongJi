package com.dingdongji.mod.mixin;

import com.dingdongji.mod.util.AnvilCraftCompat;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PocketSlot 坐标全量接管（HEAD cancellable，任何容量都不再放行铁砧原方法）。
 * <p>
 * 铁砧 {@code updatePosition} 只写 x 不写 y（y 仅构造器设为 84+(i%3)*18）。
 * 这对原版安全（失活槽永远失活），但我方容量会随护腿穿脱在 0/6/12/18/24
 * 之间变化：失活槽被挪到 (-10000,-10000) 后，若重新激活时只恢复 x，
 * y 会永久卡在 -10000——表现为"有面板无格子"，且创造模式 SlotWrapper
 * 每帧同步目标槽 x/y 后同样全灭。因此每个槽每帧都必须按当前容量重算
 * <b>完整的 x 与 y</b>，容量变化后下一帧即自愈。
 * <ul>
 *   <li>cap=6/12：逐字节复刻铁砧原公式（perSide/width/side），y 显式写
 *       84+(i%3)*18；</li>
 *   <li>cap=18/24：固定 2 列、向上加行（half=cap/2，行数 5/6），
 *       x 左 -41/-23、右 183/201，基础三行 y=84/102/120，上扩行
 *       y=66/48/30；18 格右面板孤格靠右列（201）保持镜像对称；</li>
 *   <li>index>=cap：物理槽保留但挪到屏外，永不可见/不可点。</li>
 * </ul>
 */
@Mixin(targets = "dev.dubhe.anvilcraft.inventory.PocketSlot", priority = 2000, remap = false)
public abstract class MixinPocketSlotLayout {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static Field slotXField;
   private static Field slotYField;

   @Shadow private Player owner;
   @Shadow private int pocketIndex;

   @Inject(
      method = "updatePosition",
      at = @At("HEAD"),
      cancellable = true,
      require = 1
   )
   private void ddj$layoutUpwardPockets(CallbackInfo ci) {
      int cap = AnvilCraftCompat.getPocketCapacity(this.owner);
      int index = this.pocketIndex;
      int x;
      int y;
      if (index >= cap) {
         // 数据通路固定 24 格（InventoryMenu 构造时无条件追加 12..23 物理槽，
         // 附件持久化上界也是 24），但当前容量只激活 0..cap-1。失活槽统一
         // 挪到屏幕外，物理存在但永不可见/不可点；创造模式 SlotWrapper
         // 每帧从目标槽同步 x/y，会一并跟随到屏外。
         x = -10000;
         y = -10000;
      } else if (cap <= 12) {
         // 逐字节复刻铁砧 updatePosition 公式（反汇编实证）：
         // perSide = cap==12?6:3; width = cap==12?44:26; side = index/perSide;
         // x = (side==0 ? -width-2 : 178)+5+(index%perSide/3)*18
         // 活动槽 index<cap<=12，side 只可能为 0/1，不与第三组屏外槽混淆。
         // 关键：y 也必须在此重写——铁砧原方法只写 x，一旦该槽曾在 cap=0
         // 时被挪到 y=-10000（脱护腿、换护腿、登录时序都可能触发），只
         // 恢复 x 会让 y 永久卡屏外，即"有 GUI 无格子"回归。
         int perSide = cap == 12 ? 6 : 3;
         int panelWidth = cap == 12 ? 44 : 26;
         int side = index / perSide;
         x = (side == 0 ? -panelWidth - 2 : 178) + 5 + ((index % perSide) / 3) * 18;
         y = 84 + (index % 3) * 18;
      } else {
         int half = cap / 2;
         int local = this.pocketIndex % half;
         int pair = local / 2;
         int col = local % 2;
         y = pair < 3 ? 84 + pair * 18 : 84 - (pair - 2) * 18;
         boolean rightSide = this.pocketIndex >= half;
         // 18 格（half 奇数）时每侧有一个孤格（local=half-1，在顶行）。
         // 左面板孤格保持左列（靠外），右面板孤格改到右列（靠外），
         // 配合右面板镜像贴图，两面板关于背包中心左右对称。
         if (rightSide && half % 2 == 1 && local == half - 1) {
            col = 1;
         }
         x = (rightSide ? 183 : -41) + col * 18;
      }
      if (!writeSlot(x, y)) {
         return;
      }
      ci.cancel();
   }

   /** 反射写 Slot.x/y（public final，非 record，可写）。失败时不 cancel，退回铁砧原方法。 */
   private boolean writeSlot(int x, int y) {
      try {
         if (slotXField == null) {
            slotXField = Slot.class.getDeclaredField("x");
            slotXField.setAccessible(true);
            slotYField = Slot.class.getDeclaredField("y");
            slotYField.setAccessible(true);
         }
         slotXField.setInt(this, x);
         slotYField.setInt(this, y);
         return true;
      } catch (ReflectiveOperationException e) {
         LOGGER.warn("[DingDongJi][口袋] 无法反射写入 Slot.x/y，口袋坐标接管失败", e);
         return false;
      }
   }
}
