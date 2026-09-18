package com.dingdongji.mod.item.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record ComfortableComponent() {
   public static final ComfortableComponent INSTANCE = new ComfortableComponent();
   public static final Codec<ComfortableComponent> CODEC = Codec.unit(INSTANCE);
   public static final StreamCodec<ByteBuf, ComfortableComponent> STREAM_CODEC = StreamCodec.unit(INSTANCE);
}
