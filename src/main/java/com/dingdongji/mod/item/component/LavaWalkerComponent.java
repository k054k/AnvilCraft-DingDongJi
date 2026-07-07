package com.dingdongji.mod.item.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * 蹈火组件 - 余烬靴子：按 [V] 切换，行走自带火焰效果
 */
public record LavaWalkerComponent() {
    public static final LavaWalkerComponent INSTANCE = new LavaWalkerComponent();
    public static final Codec<LavaWalkerComponent> CODEC = Codec.unit(INSTANCE);
    public static final StreamCodec<ByteBuf, LavaWalkerComponent> STREAM_CODEC = StreamCodec.unit(INSTANCE);
}
