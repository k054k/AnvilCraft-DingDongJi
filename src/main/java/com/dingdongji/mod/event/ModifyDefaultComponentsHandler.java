package com.dingdongji.mod.event;

import com.dingdongji.mod.item.ModItems;
import com.mojang.datafixers.util.Unit;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import org.slf4j.Logger;

public class ModifyDefaultComponentsHandler {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static DataComponentType<?> cachedReforging = null;
   private static DataComponentType<?> cachedEternal = null;
   private static DataComponentType<?> cachedProvidence = null;
   private static Object eternalInstance = null;
   private static Object providenceInstance = null;
   private static boolean initialized = false;

   @SubscribeEvent
   public static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
      initComponents();
      if (initialized && (cachedReforging != null || cachedEternal != null || cachedProvidence != null)) {
         setProvidence(event, (Item)ModItems.JI_SWORD.get());
         setProvidence(event, (Item)ModItems.JI_PICKAXE.get());
         setProvidence(event, (Item)ModItems.JI_HELMET.get());
         setProvidence(event, (Item)ModItems.JI_CHESTPLATE.get());
         setProvidence(event, (Item)ModItems.JI_LEGGINGS.get());
         setProvidence(event, (Item)ModItems.JI_BOOTS.get());
         setReforging(event, (Item)ModItems.EMBER_METAL_HELMET.get());
         setReforging(event, (Item)ModItems.EMBER_METAL_CHESTPLATE.get());
         setReforging(event, (Item)ModItems.EMBER_METAL_LEGGINGS.get());
         setReforging(event, (Item)ModItems.EMBER_METAL_BOOTS.get());
         setEternal(event, (Item)ModItems.TRANSCENDIUM_HELMET.get());
         setEternal(event, (Item)ModItems.TRANSCENDIUM_CHESTPLATE.get());
         setEternal(event, (Item)ModItems.TRANSCENDIUM_LEGGINGS.get());
         setEternal(event, (Item)ModItems.TRANSCENDIUM_BOOTS.get());
         setProvidence(event, (Item)ModItems.TRANSCENDIUM_HELMET.get());
         setProvidence(event, (Item)ModItems.TRANSCENDIUM_CHESTPLATE.get());
         setProvidence(event, (Item)ModItems.TRANSCENDIUM_LEGGINGS.get());
         setProvidence(event, (Item)ModItems.TRANSCENDIUM_BOOTS.get());
         setEternal(event, (Item)ModItems.CREATE_TEMPLATE.get());
         LOGGER.info(
            "[DingDongJi] 物品组件修改完成 (reforging={}, eternal={}, providence={})",
            new Object[]{cachedReforging != null, cachedEternal != null, cachedProvidence != null}
         );
      } else {
         LOGGER.info("[DingDongJi] AnvilCraft 组件未加载，跳过组件修改");
      }
   }

   private static void initComponents() {
      if (!initialized) {
         initialized = true;

         try {
            Class<?> modComponentsClass = Class.forName("dev.dubhe.anvilcraft.init.item.ModComponents");
            cachedReforging = readStaticField(modComponentsClass, "FIRE_REFORGING");
            if (cachedReforging == null) {
               cachedReforging = readStaticField(modComponentsClass, "REFORGING");
            }

            cachedEternal = readStaticField(modComponentsClass, "ETERNAL");
            cachedProvidence = readStaticField(modComponentsClass, "PROVIDENCE");
            LOGGER.info(
               "[DingDongJi] ModComponents 反射读取: reforging={}, eternal={}, providence={}",
               new Object[]{cachedReforging != null, cachedEternal != null, cachedProvidence != null}
            );
         } catch (ClassNotFoundException var4) {
            LOGGER.info("[DingDongJi] AnvilCraft ModComponents 类不存在，AnvilCraft 可能未安装");
            return;
         }

         if (cachedEternal != null) {
            try {
               Class<?> eternalClass = Class.forName("dev.dubhe.anvilcraft.item.property.component.Eternal");
               Field instanceField = eternalClass.getDeclaredField("INSTANCE");
               instanceField.setAccessible(true);
               eternalInstance = instanceField.get(null);
            } catch (Exception var3) {
               LOGGER.warn("[DingDongJi] 获取 Eternal.INSTANCE 失败", var3);
            }
         }

         if (cachedProvidence != null) {
            try {
               Class<?> providenceClass = Class.forName("dev.dubhe.anvilcraft.item.property.component.Providence");
               Field instanceField = providenceClass.getDeclaredField("INSTANCE");
               instanceField.setAccessible(true);
               providenceInstance = instanceField.get(null);
            } catch (Exception var2) {
               LOGGER.warn("[DingDongJi] 获取 Providence.INSTANCE 失败", var2);
            }
         }
      }
   }

   private static DataComponentType<?> readStaticField(Class<?> clazz, String fieldName) {
      try {
         Field field = clazz.getDeclaredField(fieldName);
         field.setAccessible(true);
         return (DataComponentType<?>)field.get(null);
      } catch (Exception var3) {
         LOGGER.warn("[DingDongJi] 读取 ModComponents.{} 失败: {}", fieldName, var3.getMessage());
         return null;
      }
   }

   @SuppressWarnings("unchecked")
   private static void setReforging(ModifyDefaultComponentsEvent event, Item item) {
      if (cachedReforging != null) {
         event.modify(item, builder -> builder.set((DataComponentType)cachedReforging, (Object)Unit.INSTANCE));
      }
   }

   @SuppressWarnings("unchecked")
   private static void setEternal(ModifyDefaultComponentsEvent event, Item item) {
      if (cachedEternal != null && eternalInstance != null) {
         event.modify(item, builder -> builder.set((DataComponentType)cachedEternal, eternalInstance));
      }
   }

   @SuppressWarnings("unchecked")
   private static void setProvidence(ModifyDefaultComponentsEvent event, Item item) {
      if (cachedProvidence != null && providenceInstance != null) {
         event.modify(item, builder -> builder.set((DataComponentType)cachedProvidence, providenceInstance));
      }
   }
}
