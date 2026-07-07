package com.dingdongji.mod.network;

import com.dingdongji.mod.event.ModArmorSetHandler;
import com.dingdongji.mod.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 网络包注册与服务端处理器。
 * 注册单个能力切换包，服务端根据穿着自动判断执行哪种切换。
 */
public class ModNetwork {

    /** 注册所有自定义网络包（在 FML 构造阶段调用） */
    public static void register() {
        PayloadRegistrar registrar = new PayloadRegistrar("dingdongji");
        registrar.playToServer(
                AbilityTogglePacket.TYPE,
                AbilityTogglePacket.STREAM_CODEC,
                new AbilityToggleHandler()
        );
    }

    // ===== 能力切换处理器 =====
    public static class AbilityToggleHandler implements IPayloadHandler<AbilityTogglePacket> {
        @Override
        public void handle(AbilityTogglePacket packet, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer serverPlayer) {
                    handleAbilityToggle(serverPlayer);
                }
            });
        }
    }

    /**
     * 根据当前穿着自动判断执行哪种切换：
     * 1. 穿着余烬靴子 → 蹈火切换
     * 2. 穿着超限靴子 → 蹈虚切换
     * 3. 两双都没穿 → 提示
     */
    private static void handleAbilityToggle(ServerPlayer player) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);

        if (boots.is(ModItems.EMBER_METAL_BOOTS.get())) {
            ModArmorSetHandler.toggleLavaWalker(player);
        } else if (boots.is(ModItems.TRANSCENDIUM_BOOTS.get())) {
            ModArmorSetHandler.toggleStrideVoid(player);
        } else {
            player.displayClientMessage(
                    Component.literal("[叮咚叽] 未穿戴可切换的靴子（余烬靴子/超限靴子）").withStyle(ChatFormatting.RED),
                    true
            );
        }
    }
}
