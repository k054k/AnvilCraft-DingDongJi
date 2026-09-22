package com.dingdongji.mod.util;

import com.mojang.datafixers.util.Unit;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import javax.annotation.Nullable;
import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.component.PouchCapacityComponent;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public class AnvilCraftCompat {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceLocation REFORGING_ID = ResourceLocation.parse("anvilcraft:reforging");
   private static final ResourceLocation ETERNAL_ID = ResourceLocation.parse("anvilcraft:eternal");
   private static final ResourceLocation PROVIDENCE_ID = ResourceLocation.parse("anvilcraft:providence");
   @Nullable
   private static DataComponentType<?> cachedReforging = null;
   @Nullable
   private static DataComponentType<?> cachedEternal = null;
   @Nullable
   private static DataComponentType<?> cachedProvidence = null;
   private static boolean initialized = false;
   @Nullable
   private static Object eternalInstance = null;
   @Nullable
   private static Object providenceInstance = null;

   public static void init(RegistryAccess access) {
      if (!initialized) {
         initialized = true;
         Registry<DataComponentType<?>> registry = access.registryOrThrow(Registries.DATA_COMPONENT_TYPE);
         cachedReforging = (DataComponentType<?>)registry.get(REFORGING_ID);
         cachedEternal = (DataComponentType<?>)registry.get(ETERNAL_ID);
         cachedProvidence = (DataComponentType<?>)registry.get(PROVIDENCE_ID);
         if (cachedEternal != null) {
            try {
               Class<?> eternalClass = Class.forName("dev.dubhe.anvilcraft.item.property.component.Eternal");
               Field instanceField = eternalClass.getDeclaredField("INSTANCE");
               instanceField.setAccessible(true);
               eternalInstance = instanceField.get(null);
            } catch (Exception var5) {
               LOGGER.warn("[DingDongJi] 无法反射获取 Eternal.INSTANCE", var5);
            }
         }

         if (cachedProvidence != null) {
            try {
               Class<?> providenceClass = Class.forName("dev.dubhe.anvilcraft.item.property.component.Providence");
               Field instanceField = providenceClass.getDeclaredField("INSTANCE");
               instanceField.setAccessible(true);
               providenceInstance = instanceField.get(null);
            } catch (Exception var4) {
               LOGGER.warn("[DingDongJi] 无法反射获取 Providence.INSTANCE", var4);
            }
         }

         LOGGER.info(
            "[DingDongJi] AnvilCraftCompat 初始化: reforging={}, eternal={}, providence={}",
            new Object[]{cachedReforging != null, cachedEternal != null, cachedProvidence != null}
         );
      }
   }

   public static boolean isLoaded() {
      return cachedReforging != null || cachedProvidence != null;
   }

   public static void setReforging(ItemStack stack) {
      if (cachedReforging != null && !stack.has(cachedReforging)) {
         DataComponentType<Unit> type = (DataComponentType<Unit>)cachedReforging;
         stack.set(type, Unit.INSTANCE);
      }
   }

   public static void setEternal(ItemStack stack) {
      if (cachedEternal != null && eternalInstance != null && !stack.has(cachedEternal)) {
         DataComponentType<Object> type = (DataComponentType<Object>)cachedEternal;
         stack.set(type, eternalInstance);
      }
   }

   public static void setFortune(ItemStack stack) {
      if (cachedProvidence != null && providenceInstance != null && !stack.has(cachedProvidence)) {
         DataComponentType<Object> type = (DataComponentType<Object>)cachedProvidence;
         stack.set(type, providenceInstance);
      }
   }

   /**
    * 玩家口袋栏容量的唯一计算点（双端共用）。
    * <p>
    * 铁砧本体容量：耐候航天护腿 12、铁砧口袋护腿 6、其他 0；
    * 再与护腿上 DDJ {@link PouchCapacityComponent} 的容量取 max。
    * 结果：普通护腿+小口袋=6，+深口袋=12；耐候航天+深口袋=24。
    */
   public static int getPocketCapacity(Player player) {
      ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
      String itemId = BuiltInRegistries.ITEM.getKey(leggings.getItem()).toString();
      int base = switch (itemId) {
         case "anvilcraft:weatherproof_spacesuit_leggings" -> 12;
         case "anvilcraft:pockets_leggings" -> 6;
         default -> 0;
      };
      PouchCapacityComponent comp = leggings.get(ModComponents.POUCH_CAPACITY.get());
      int pouch = comp != null ? comp.capacity() : 0;
      int result = base + pouch;
      // 调试日志：容量结果变化时才打印（该方法每帧可能被多次调用）
      if (result != lastLoggedCapacity || !itemId.equals(lastLoggedLeggingsId)) {
         lastLoggedCapacity = result;
         lastLoggedLeggingsId = itemId;
         LOGGER.info(
            "[DingDongJi][口袋] 护腿={} 基础容量={} 口袋组件={} → 总容量={}",
            itemId, base, pouch, result
         );
      }
      return result;
   }

   private static int lastLoggedCapacity = -1;
   private static String lastLoggedLeggingsId = "";
}
