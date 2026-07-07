package com.dingdongji.mod.item.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * 舒适组件 - 皇家钢靴子：行走更加便捷舒适
 */
public record ComfortableComponent() {
    public static final ComfortableComponent INSTANCE = new ComfortableComponent();
    public static final Codec<ComfortableComponent> CODEC = Codec.unit(INSTANCE);
    public static final StreamCodec<ByteBuf, ComfortableComponent> STREAM_CODEC = StreamCodec.unit(INSTANCE);
}
