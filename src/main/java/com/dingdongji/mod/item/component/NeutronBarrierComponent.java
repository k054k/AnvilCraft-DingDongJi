package com.dingdongji.mod.item.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * 中子屏罩组件 - 超限合金护腿：清除靠近自身的弹射物，弹飞靠近自身的高威胁生物
 */
public record NeutronBarrierComponent() {
    public static final NeutronBarrierComponent INSTANCE = new NeutronBarrierComponent();
    public static final Codec<NeutronBarrierComponent> CODEC = Codec.unit(INSTANCE);
    public static final StreamCodec<ByteBuf, NeutronBarrierComponent> STREAM_CODEC = StreamCodec.unit(INSTANCE);
}
