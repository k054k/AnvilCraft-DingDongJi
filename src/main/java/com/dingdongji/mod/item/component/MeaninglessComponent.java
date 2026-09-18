package com.dingdongji.mod.item.component;

import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record MeaninglessComponent() {
   public static final MeaninglessComponent DEFAULT = new MeaninglessComponent();
   public static final MapCodec<MeaninglessComponent> CODEC = MapCodec.unit(DEFAULT);
   public static final StreamCodec<ByteBuf, MeaninglessComponent> STREAM_CODEC = StreamCodec.unit(DEFAULT);
}
