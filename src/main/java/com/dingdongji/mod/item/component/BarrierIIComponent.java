package com.dingdongji.mod.item.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record BarrierIIComponent() {
   public static final BarrierIIComponent INSTANCE = new BarrierIIComponent();
   public static final Codec<BarrierIIComponent> CODEC = Codec.unit(INSTANCE);
   public static final StreamCodec<ByteBuf, BarrierIIComponent> STREAM_CODEC = StreamCodec.unit(INSTANCE);
}
