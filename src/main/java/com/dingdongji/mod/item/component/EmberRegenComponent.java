package com.dingdongji.mod.item.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record EmberRegenComponent() {
   public static final EmberRegenComponent INSTANCE = new EmberRegenComponent();
   public static final Codec<EmberRegenComponent> CODEC = Codec.unit(INSTANCE);
   public static final StreamCodec<ByteBuf, EmberRegenComponent> STREAM_CODEC = StreamCodec.unit(INSTANCE);
}
