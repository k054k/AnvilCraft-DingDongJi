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
 * 容量 > 12（18 或 24）时重排 PocketSlot：<b>固定 2 列、向上加行</b>。
 * <p>
 * 旧方案横向加列（18→3列62宽/24→4列80宽）过宽，且 80 贴图裁到 62
 * 会残留第 4 列 3px 窄条（视觉"断开"）。现与 cap=12 一样保持面板 44
 * 宽（2 列），多出的槽位逐行加到上方：
 * <ul>
 *   <li>每侧 half = cap/2 格，行数 R = ceil(half/2)：18→5 行，24→6 行；</li>
 *   <li>基础三行 y 保持 84/102/120（与铁砧原版一致），上扩行
 *       y=66/48/30；</li>
 *   <li>x：左 -41/-23，右 183/201（同 cap=12 原坐标）；18 格时
 *       右面板孤格由左列改到右列（201），与左面板镜像对称。</li>
 * </ul>
 * cap≤12 不干预，走铁砧原算法。
 * <p>
 * Slot.y 在 PocketSlot 构造器里被固定为 84+(i%3)*18，因此和 x 一样
 * 反射 Slot.class 的 x/y 字段直接写（public final，非 record，可写）。
 */
@Mixin(targets = "dev.dubhe.anvilcraft.inventory.PocketSlot", priority = 2000, remap = false)
public abstract class MixinPocketSlotLayout {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static Field slotXField;
   private static Field slotYField;
   private static boolean logged;

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
      // 我方在 InventoryMenu 构造时无条件追加了 index 12..23 的物理槽（数据
      // 通路固定 24 格）。容量 <=12 时这些槽 isActive()=false：生存背包界面
      // 按 isActive 跳过无碍，但创造界面把全部槽包成 SlotWrapper 且按坐标取
      // 最后一个——若任由铁砧公式定位，side=index/half>=2 会全部落到右面板
      // 与 6..11 重合，点击右面板命中无效槽被服务端回滚（实测右口袋失效）。
      // 容量不足时把追加槽挪到屏幕外，物理存在但永不可见/不可点。
      if (this.pocketIndex >= 12 && cap <= 12) {
         try {
            if (slotXField == null) {
               slotXField = Slot.class.getDeclaredField("x");
               slotXField.setAccessible(true);
               slotYField = Slot.class.getDeclaredField("y");
               slotYField.setAccessible(true);
            }
            slotXField.setInt(this, -10000);
            slotYField.setInt(this, -10000);
         } catch (ReflectiveOperationException e) {
            LOGGER.warn("[DingDongJi][口袋] 无法将多余槽位移出屏幕", e);
         }
         ci.cancel();
         return;
      }
      if (cap <= 12) {
         return;
      }
      int half = cap / 2;
      int local = this.pocketIndex % half;
      int pair = local / 2;
      int col = local % 2;
      int y = pair < 3 ? 84 + pair * 18 : 84 - (pair - 2) * 18;
      boolean rightSide = this.pocketIndex >= half;
      // 18 格（half 奇数）时每侧有一个孤格（local=half-1，在顶行）。
      // 左面板孤格保持左列（靠外），右面板孤格改到右列（靠外），
      // 配合右面板镜像贴图，两面板关于背包中心左右对称。
      if (rightSide && half % 2 == 1 && local == half - 1) {
         col = 1;
      }
      int x = (rightSide ? 183 : -41) + col * 18;
      try {
         if (slotXField == null) {
            slotXField = Slot.class.getDeclaredField("x");
            slotXField.setAccessible(true);
            slotYField = Slot.class.getDeclaredField("y");
            slotYField.setAccessible(true);
         }
         slotXField.setInt(this, x);
         slotYField.setInt(this, y);
      } catch (ReflectiveOperationException e) {
         LOGGER.warn("[DingDongJi][口袋] 无法反射写入 Slot.x/y，向上布局失败", e);
      }
      if (!logged) {
         logged = true;
         int rows = (half + 1) / 2;
         LOGGER.info(
            "[DingDongJi][口袋] 向上布局生效：容量={} half={} 行数={}（面板44宽，顶部y={}）",
            cap, half, rows, y
         );
      }
      ci.cancel();
   }
}
