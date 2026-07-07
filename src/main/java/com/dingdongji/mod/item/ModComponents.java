package com.dingdongji.mod.item;

import com.dingdongji.mod.KryptonMod;
import com.dingdongji.mod.item.component.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, KryptonMod.MODID);

    // ===== 武器组件 =====

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DevourData>> DEVOUR =
            COMPONENTS.register("devour", () -> DataComponentType.<DevourData>builder()
                    .persistent(DevourData.CODEC)
                    .networkSynchronized(DevourData.STREAM_CODEC)
                    .build()
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<AccumulateData>> ACCUMULATE =
            COMPONENTS.register("accumulate", () -> DataComponentType.<AccumulateData>builder()
                    .persistent(AccumulateData.CODEC)
                    .networkSynchronized(AccumulateData.STREAM_CODEC)
                    .build()
            );

    // ===== 超限合金套组件 =====

    /** 适应 - 头盔：适应黑暗，高温，水下环境；高亮敌对生物 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlowingVisionComponent>> GLOWING_VISION =
            COMPONENTS.register("glowing_vision", () -> DataComponentType.<GlowingVisionComponent>builder()
                    .persistent(GlowingVisionComponent.CODEC)
                    .networkSynchronized(GlowingVisionComponent.STREAM_CODEC)
                    .build()
            );

    /** 壁垒II - 胸甲：大幅减伤，无视魔法/虚空/接触/爆炸伤害；低血紧急治愈 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BarrierIIComponent>> BARRIER_II =
            COMPONENTS.register("barrier_ii", () -> DataComponentType.<BarrierIIComponent>builder()
                    .persistent(BarrierIIComponent.CODEC)
                    .networkSynchronized(BarrierIIComponent.STREAM_CODEC)
                    .build()
            );

    /** 中子屏罩 - 护腿：清除弹射物，弹飞高威胁生物 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<NeutronBarrierComponent>> NEUTRON_BARRIER =
            COMPONENTS.register("neutron_barrier", () -> DataComponentType.<NeutronBarrierComponent>builder()
                    .persistent(NeutronBarrierComponent.CODEC)
                    .networkSynchronized(NeutronBarrierComponent.STREAM_CODEC)
                    .build()
            );

    /** 蹈虚 - 靴子：手动开关重力模式，常驻摔落伤害归零 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StrideVoidComponent>> STRIDE_VOID =
            COMPONENTS.register("stride_void", () -> DataComponentType.<StrideVoidComponent>builder()
                    .persistent(StrideVoidComponent.CODEC)
                    .networkSynchronized(StrideVoidComponent.STREAM_CODEC)
                    .build()
            );

    // ===== 皇家钢套组件 =====

    /** 皇家亲和 - 胸甲：持续恢复穿戴者的生命 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RoyalSteelAffinityComponent>> ROYAL_STEEL_AFFINITY =
            COMPONENTS.register("royal_steel_affinity", () -> DataComponentType.<RoyalSteelAffinityComponent>builder()
                    .persistent(RoyalSteelAffinityComponent.CODEC)
                    .networkSynchronized(RoyalSteelAffinityComponent.STREAM_CODEC)
                    .build()
            );

    /** 舒适 - 靴子：行走更加便捷舒适 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ComfortableComponent>> COMFORTABLE =
            COMPONENTS.register("comfortable", () -> DataComponentType.<ComfortableComponent>builder()
                    .persistent(ComfortableComponent.CODEC)
                    .networkSynchronized(ComfortableComponent.STREAM_CODEC)
                    .build()
            );

    // ===== 余烬金属套组件 =====

    /** 隔热 - 头盔：隔绝高温环境带来的灼烧 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<HeatInsulationComponent>> HEAT_INSULATION =
            COMPONENTS.register("heat_insulation", () -> DataComponentType.<HeatInsulationComponent>builder()
                    .persistent(HeatInsulationComponent.CODEC)
                    .networkSynchronized(HeatInsulationComponent.STREAM_CODEC)
                    .build()
            );

    /** 壁垒I - 胸甲：对大部分伤害明显减伤 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BarrierIComponent>> BARRIER_I =
            COMPONENTS.register("barrier_i", () -> DataComponentType.<BarrierIComponent>builder()
                    .persistent(BarrierIComponent.CODEC)
                    .networkSynchronized(BarrierIComponent.STREAM_CODEC)
                    .build()
            );

    /** 浴火重生 - 护腿：火中持续恢复生命 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EmberRegenComponent>> EMBER_REGEN =
            COMPONENTS.register("ember_regen", () -> DataComponentType.<EmberRegenComponent>builder()
                    .persistent(EmberRegenComponent.CODEC)
                    .networkSynchronized(EmberRegenComponent.STREAM_CODEC)
                    .build()
            );

    /** 蹈火 - 靴子：按 [V] 切换，行走自带火焰效果 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LavaWalkerComponent>> LAVA_WALKER =
            COMPONENTS.register("lava_walker", () -> DataComponentType.<LavaWalkerComponent>builder()
                    .persistent(LavaWalkerComponent.CODEC)
                    .networkSynchronized(LavaWalkerComponent.STREAM_CODEC)
                    .build()
            );
}
