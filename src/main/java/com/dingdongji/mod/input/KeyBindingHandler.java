package com.dingdongji.mod.input;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 在客户端侧注册自定义键位并处理 tick 检测。
 * 通过 @EventBusSubscriber(Dist.CLIENT) 自动注册到 GAME 总线。
 */
@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class KeyBindingHandler {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ModKeyBindings.tick(net.minecraft.client.Minecraft.getInstance());
    }
}
