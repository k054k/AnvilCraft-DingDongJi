package com.dingdongji.mod.event;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import com.dingdongji.mod.KryptonMod;
import com.dingdongji.mod.util.AnvilCraftCompat;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * 服务端启动时初始化 AnvilCraft 组件缓存（用于运行时装备组件补齐）。
 * 创造标签页物品的默认组件由 ModifyDefaultComponentsHandler 处理。
 * 创造模板配方注入由 CreateTemplateRecipeInjector 处理。
 */
public class ModRecipeHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();

        // 初始化 AnvilCraft 组件缓存（用于运行时装备变更/玩家登录时的组件补齐）
        AnvilCraftCompat.init(server.registryAccess());

        LOGGER.info("[DingDongJi] 服务端启动完成, AnvilCraft={}", AnvilCraftCompat.isLoaded());
    }
}
