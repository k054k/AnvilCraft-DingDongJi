package com.dingdongji.mod.network;

import com.dingdongji.mod.client.IonocraftBootsClientHandler;
import com.dingdongji.mod.event.ModArmorSetHandler;
import com.dingdongji.mod.item.ModItems;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 网络包注册与服务端处理器。
 * 通过 RegisterPayloadHandlersEvent 注册 payload（参考 AnvilCraft）。
 */
public class ModNetwork {

    /** 注册所有自定义网络包（在 MOD 总线 RegisterPayloadHandlersEvent 上调用） */
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
                AbilityTogglePacket.TYPE,
                AbilityTogglePacket.STREAM_CODEC,
                new AbilityToggleHandler()
        );
        registrar.playToServer(
                GlowingVisionTogglePacket.TYPE,
                GlowingVisionTogglePacket.STREAM_CODEC,
                new GlowingVisionToggleHandler()
        );
        registrar.playToServer(
                NeutronBarrierTogglePacket.TYPE,
                NeutronBarrierTogglePacket.STREAM_CODEC,
                new NeutronBarrierToggleHandler()
        );
        registrar.playToServer(
                SwitchTemplateModePacket.TYPE,
                SwitchTemplateModePacket.STREAM_CODEC,
                SwitchTemplateModePacket::handle
        );
        registrar.playToServer(
                SelectTemplateModePacket.TYPE,
                SelectTemplateModePacket.STREAM_CODEC,
                SelectTemplateModePacket::handle
        );
        registrar.playToClient(
                IonocraftBootsFlyingPacket.TYPE,
                IonocraftBootsFlyingPacket.STREAM_CODEC,
                new IonocraftBootsFlyingHandler()
        );

    }

    // ===== 超限靴子蹈虚飞行状态同步处理器（服务端 → 客户端）=====
    public static class IonocraftBootsFlyingHandler implements IPayloadHandler<IonocraftBootsFlyingPacket> {
        @Override
        public void handle(IonocraftBootsFlyingPacket packet, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
                    IonocraftBootsClientHandler.onFlyingSync(packet.playerId(), packet.flying());
                }
            });
        }
    }

    // ===== 中子屏罩切换处理器 =====
    public static class NeutronBarrierToggleHandler implements IPayloadHandler<NeutronBarrierTogglePacket> {
        @Override
        public void handle(NeutronBarrierTogglePacket packet, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer serverPlayer) {
                    ModArmorSetHandler.toggleNeutronBarrier(serverPlayer);
                }
            });
        }
    }

    // ===== 高亮切换处理器 =====
    public static class GlowingVisionToggleHandler implements IPayloadHandler<GlowingVisionTogglePacket> {
        @Override
        public void handle(GlowingVisionTogglePacket packet, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer serverPlayer) {
                    ModArmorSetHandler.toggleGlowingVision(serverPlayer);
                }
            });
        }
    }

    // ===== 能力切换处理器 =====
    public static class AbilityToggleHandler implements IPayloadHandler<AbilityTogglePacket> {
        @Override
        public void handle(AbilityTogglePacket packet, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer serverPlayer) {
                    handleAbilityToggle(serverPlayer, packet.doublePress());
                }
            });
        }
    }

    /**
     * 根据当前穿着自动判断执行哪种切换：
     * 1. 穿着皇家钢靴子 → 舒适切换
     * 2. 穿着余烬靴子 → 蹈火切换
     * 3. 穿着超限靴子 → 量子隧穿切换
     * 4. 两双都没穿 → 静默忽略
     */
    private static void handleAbilityToggle(ServerPlayer player, boolean doublePress) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);

        if (boots.is(ModItems.ROYAL_STEEL_BOOTS.get())) {
            ModArmorSetHandler.toggleComfortable(player);
        } else if (boots.is(ModItems.EMBER_METAL_BOOTS.get())) {
            ModArmorSetHandler.toggleLavaWalker(player);
        } else if (boots.is(ModItems.TRANSCENDIUM_BOOTS.get())) {
            ModArmorSetHandler.toggleIonocraftFlight(player);
        } // 未穿戴对应靴子时静默忽略
    }
}
