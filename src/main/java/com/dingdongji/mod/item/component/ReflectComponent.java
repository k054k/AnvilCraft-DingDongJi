package com.dingdongji.mod.item.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * 反射组件 - 标记护腿可以反射弹射物
 */
public record ReflectComponent() {
    public static final ReflectComponent INSTANCE = new ReflectComponent();
    public static final Codec<ReflectComponent> CODEC = Codec.unit(INSTANCE);
    public static final StreamCodec<ByteBuf, ReflectComponent> STREAM_CODEC = StreamCodec.unit(INSTANCE);
}
