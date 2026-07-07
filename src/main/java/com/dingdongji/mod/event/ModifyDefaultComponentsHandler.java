package com.dingdongji.mod.event;

import com.dingdongji.mod.KryptonMod;
import com.dingdongji.mod.item.ModItems;
import com.mojang.datafixers.util.Unit;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

import java.lang.reflect.Field;

/**
 * 使用 ModifyDefaultComponentsEvent 修改物品默认组件。
 * 通过反射读取 AnvilCraft 的 ModComponents 静态字段（类型为 DataComponentType 本身，非 DeferredHolder），
 * 然后通过 event.modify() 安全设置。
 *
 * 参考 AnvilCraft AlloyExtension 的做法：
 *   new Item.Properties().component(ModComponents.ETERNAL, Eternal.INSTANCE)
 * 其中 ModComponents.ETERNAL 是 public static final DataComponentType<?> 字段（已注册完成的实例）。
 */
public class ModifyDefaultComponentsHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    // 缓存组件类型引用（AnvilCraft ModComponents 的静态字段值本身即是 DataComponentType）
    private static DataComponentType<?> cachedReforging = null;
    private static DataComponentType<?> cachedEternal = null;
    private static DataComponentType<?> cachedProvidence = null;
    private static Object eternalInstance = null;
    private static Object providenceInstance = null;
    private static boolean initialized = false;

    @SubscribeEvent
    public static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        initComponents();

        if (!initialized || (cachedReforging == null && cachedEternal == null && cachedProvidence == null)) {
            LOGGER.info("[DingDongJi] AnvilCraft 组件未加载，跳过组件修改");
            return;
        }

        // 叽武器/叽套 → 强运 (Providence)
        setProvidence(event, ModItems.JI_SWORD.get());
        setProvidence(event, ModItems.JI_PICKAXE.get());
        setProvidence(event, ModItems.JI_HELMET.get());
        setProvidence(event, ModItems.JI_CHESTPLATE.get());
        setProvidence(event, ModItems.JI_LEGGINGS.get());
        setProvidence(event, ModItems.JI_BOOTS.get());

        // 余烬金属套 → 重铸 (Reforging)
        setReforging(event, ModItems.EMBER_METAL_HELMET.get());
        setReforging(event, ModItems.EMBER_METAL_CHESTPLATE.get());
        setReforging(event, ModItems.EMBER_METAL_LEGGINGS.get());
        setReforging(event, ModItems.EMBER_METAL_BOOTS.get());

        // 超限合金套 → 永恒 (Eternal) + 强运 (Providence)
        setEternal(event, ModItems.TRANSCENDIUM_HELMET.get());
        setEternal(event, ModItems.TRANSCENDIUM_CHESTPLATE.get());
        setEternal(event, ModItems.TRANSCENDIUM_LEGGINGS.get());
        setEternal(event, ModItems.TRANSCENDIUM_BOOTS.get());
        setProvidence(event, ModItems.TRANSCENDIUM_HELMET.get());
        setProvidence(event, ModItems.TRANSCENDIUM_CHESTPLATE.get());
        setProvidence(event, ModItems.TRANSCENDIUM_LEGGINGS.get());
        setProvidence(event, ModItems.TRANSCENDIUM_BOOTS.get());

        // 创造模板 → 永恒 (Eternal)（自身不消耗）
        setEternal(event, ModItems.CREATE_TEMPLATE.get());

        LOGGER.info("[DingDongJi] 物品组件修改完成 (reforging={}, eternal={}, providence={})",
                cachedReforging != null, cachedEternal != null, cachedProvidence != null);
    }

    /**
     * 反射读取 AnvilCraft ModComponents 的静态字段。
     *
     * 关键区别（与之前错误的方案）：
     * - ModComponents.ETERNAL 的 Java 类型是 DataComponentType（已注册完成的实例），不是 DeferredHolder
     * - 所以直接 field.get(null) 就是 DataComponentType，不需要调用 .get()
     * - AlloyExtension 就是这么做的：Item.Properties.component(ModComponents.ETERNAL, Eternal.INSTANCE)
     */
    @SuppressWarnings("unchecked")
    private static void initComponents() {
        if (initialized) return;
        initialized = true;

        try {
            Class<?> modComponentsClass = Class.forName("dev.dubhe.anvilcraft.init.item.ModComponents");

            // 直接读取静态字段值——它们已经是 DataComponentType 实例
            // AnvilCraft 1.5.1 中字段名为 FIRE_REFORGING（不是 REFORGING）
            cachedReforging = readStaticField(modComponentsClass, "FIRE_REFORGING");
            if (cachedReforging == null) {
                cachedReforging = readStaticField(modComponentsClass, "REFORGING"); // 兼容旧版
            }
            cachedEternal = readStaticField(modComponentsClass, "ETERNAL");
            cachedProvidence = readStaticField(modComponentsClass, "PROVIDENCE");

            LOGGER.info("[DingDongJi] ModComponents 反射读取: reforging={}, eternal={}, providence={}",
                    cachedReforging != null, cachedEternal != null, cachedProvidence != null);

        } catch (ClassNotFoundException e) {
            LOGGER.info("[DingDongJi] AnvilCraft ModComponents 类不存在，AnvilCraft 可能未安装");
            return;
        }

        // 获取 Eternal.INSTANCE 单例
        if (cachedEternal != null) {
            try {
                Class<?> eternalClass = Class.forName("dev.dubhe.anvilcraft.item.property.component.Eternal");
                Field instanceField = eternalClass.getDeclaredField("INSTANCE");
                instanceField.setAccessible(true);
                eternalInstance = instanceField.get(null);
            } catch (Exception e) {
                LOGGER.warn("[DingDongJi] 获取 Eternal.INSTANCE 失败", e);
            }
        }

        // 获取 Providence.INSTANCE 单例
        if (cachedProvidence != null) {
            try {
                Class<?> providenceClass = Class.forName("dev.dubhe.anvilcraft.item.property.component.Providence");
                Field instanceField = providenceClass.getDeclaredField("INSTANCE");
                instanceField.setAccessible(true);
                providenceInstance = instanceField.get(null);
            } catch (Exception e) {
                LOGGER.warn("[DingDongJi] 获取 Providence.INSTANCE 失败", e);
            }
        }
    }

    /** 直接读取静态字段值（ModComponents 的字段已经是 DataComponentType 实例，不是 DeferredHolder） */
    @SuppressWarnings("unchecked")
    private static DataComponentType<?> readStaticField(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (DataComponentType<?>) field.get(null);
        } catch (Exception e) {
            LOGGER.warn("[DingDongJi] 读取 ModComponents.{} 失败: {}", fieldName, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setReforging(ModifyDefaultComponentsEvent event, net.minecraft.world.item.Item item) {
        if (cachedReforging != null) {
            event.modify(item, builder -> builder.set((DataComponentType) cachedReforging, Unit.INSTANCE));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setEternal(ModifyDefaultComponentsEvent event, net.minecraft.world.item.Item item) {
        if (cachedEternal != null && eternalInstance != null) {
            event.modify(item, builder -> builder.set((DataComponentType) cachedEternal, eternalInstance));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setProvidence(ModifyDefaultComponentsEvent event, net.minecraft.world.item.Item item) {
        if (cachedProvidence != null && providenceInstance != null) {
            event.modify(item, builder -> builder.set((DataComponentType) cachedProvidence, providenceInstance));
        }
    }
}
