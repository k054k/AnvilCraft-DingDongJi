package com.dingdongji.mod.mixin;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 铁砧 InventoryMenuMixin.anvilcraft$addPockets 在 InventoryMenu 构造期间
 * 固定 addSlot 12 个 PocketSlot（index 0..11）。深口袋最多 24 栏，需要
 * index 12..23 的 PocketSlot 实例。
 * <p>
 * 本 Mixin 注入 InventoryMenu 构造器 RETURN：铁砧 priority=1000 先执行
 * addPockets（12 槽），我方 priority=2000 构造完成后补 12 槽。
 * <p>
 * 关键教训（两次失败后定型）：addSlot 是<b>继承自 AbstractContainerMenu</b>
 * 的 protected 方法，手写空 refmap 环境下，无论 remap=true 还是 false，
 * {@code @Shadow addSlot} 都报 "was not located in the target class"。
 * 因此彻底不用 @Shadow：
 * <ul>
 *   <li>addSlot：遍历 AbstractContainerMenu.getDeclaredMethods，按
 *       "1 个 Slot 参数、返回 Slot"的描述符定位（与运行时方法名无关）；</li>
 *   <li>owner：遍历 InventoryMenu.getDeclaredFields，按 Player 类型定位
 *       （InventoryMenu 中唯一 Player 字段，与字段名无关）；</li>
 *   <li>PocketSlot：铁砧类，反射构造器 (Player,int)。</li>
 * </ul>
 */
@Mixin(InventoryMenu.class)
public abstract class MixinPocketMenuSlots {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static Method ddj$addSlotMethod;
   private static Field ddj$ownerField;

   @Inject(method = "<init>", at = @At("RETURN"))
   private void ddj$addExtraPocketSlots(CallbackInfo ci) {
      try {
         if (ddj$addSlotMethod == null) {
            for (Method m : AbstractContainerMenu.class.getDeclaredMethods()) {
               if (m.getReturnType() == Slot.class
                     && m.getParameterCount() == 1
                     && m.getParameterTypes()[0] == Slot.class) {
                  m.setAccessible(true);
                  ddj$addSlotMethod = m;
                  break;
               }
            }
         }
         if (ddj$addSlotMethod == null) {
            LOGGER.warn("[DingDongJi][口袋] 未找到 AbstractContainerMenu.addSlot(Slot)，放弃补槽");
            return;
         }
         if (ddj$ownerField == null) {
            for (Field f : InventoryMenu.class.getDeclaredFields()) {
               if (f.getType() == Player.class) {
                  f.setAccessible(true);
                  ddj$ownerField = f;
                  break;
               }
            }
         }
         Player owner = (Player) ddj$ownerField.get(this);
         Class<?> pocketSlotClass = Class.forName("dev.dubhe.anvilcraft.inventory.PocketSlot");
         Constructor<?> pocketCtor = pocketSlotClass.getConstructor(Player.class, int.class);
         for (int i = 12; i < 24; i++) {
            Object pocketSlot = pocketCtor.newInstance(owner, i);
            ddj$addSlotMethod.invoke(this, pocketSlot);
         }
         LOGGER.info("[DingDongJi][口袋] 已补充 PocketSlot index 12..23（物理槽总数 24）");
      } catch (Throwable t) {
         LOGGER.warn("[DingDongJi][口袋] 补充深口袋槽位失败", t);
      }
   }
}
