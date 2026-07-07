package com.dingdongji.mod.item.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * 浴火重生组件 - 余烬护腿：持续恢复处于熔岩或火焰中佩戴者的生命，处于灵魂火时恢复效果翻倍
 */
public record EmberRegenComponent() {
    public static final EmberRegenComponent INSTANCE = new EmberRegenComponent();
    public static final Codec<EmberRegenComponent> CODEC = Codec.unit(INSTANCE);
    public static final StreamCodec<ByteBuf, EmberRegenComponent> STREAM_CODEC = StreamCodec.unit(INSTANCE);
}
