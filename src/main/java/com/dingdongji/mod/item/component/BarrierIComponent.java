package com.dingdongji.mod.item.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record BarrierIComponent() {
   public static final BarrierIComponent INSTANCE = new BarrierIComponent();
   public static final Codec<BarrierIComponent> CODEC = Codec.unit(INSTANCE);
   public static final StreamCodec<ByteBuf, BarrierIComponent> STREAM_CODEC = StreamCodec.unit(INSTANCE);
}
