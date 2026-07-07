package com.dingdongji.mod.item.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * 蹈空组件 - 标记靴子可以在虚空中行走
 */
public record StrideVoidComponent() {
    public static final StrideVoidComponent INSTANCE = new StrideVoidComponent();
    public static final Codec<StrideVoidComponent> CODEC = Codec.unit(INSTANCE);
    public static final StreamCodec<ByteBuf, StrideVoidComponent> STREAM_CODEC = StreamCodec.unit(INSTANCE);
}
