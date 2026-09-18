package com.dingdongji.mod.util;

import com.mojang.datafixers.util.Unit;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
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
}
