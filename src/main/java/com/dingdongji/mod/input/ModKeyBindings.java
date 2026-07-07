package com.dingdongji.mod.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.dingdongji.mod.KryptonMod;
import com.dingdongji.mod.network.AbilityTogglePacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 叮咚叽自定义键位。
 * 单个功能键，同时用于余烬靴子（蹈火）和超限靴子（蹈虚）的切换。
 * 服务端根据当前穿着自动判断执行哪种切换。
 */
public class ModKeyBindings {

    public static final String CATEGORY = "key.categories.dingdongji";
    public static final String ABILITY_NAME = "key.dingdongji.ability_toggle";

    public static final KeyMapping ABILITY_KEY = new KeyMapping(
            ABILITY_NAME,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY
    );

    /** 注册键位映射（MOD 总线事件） */
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ABILITY_KEY);
    }

    /**
     * 在客户端 tick 中检测按键按下，发送网络包到服务端。
     */
    public static void tick(Minecraft mc) {
        while (ABILITY_KEY.consumeClick()) {
            PacketDistributor.sendToServer(new AbilityTogglePacket());
        }
    }
}
