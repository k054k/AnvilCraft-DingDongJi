package com.dingdongji.mod.client.accessor;

import java.util.List;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/**
 * 鸭子接口：暴露 LivingEntityRenderer 的 layers 列表。
 * 外部代码（ClientLayerSetup）引用本接口；mixin 实现由
 * {@code com.dingdongji.mod.mixin.LivingEntityRendererAccessor} 提供（客户端 mixin）。
 */
public interface LivingEntityRendererAccess {
   List<RenderLayer<?, ?>> ddj$getLayers();
}

