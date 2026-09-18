package com.dingdongji.mod.item.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record HeatInsulationComponent() {
   public static final HeatInsulationComponent INSTANCE = new HeatInsulationComponent();
   public static final Codec<HeatInsulationComponent> CODEC = Codec.unit(INSTANCE);
   public static final StreamCodec<ByteBuf, HeatInsulationComponent> STREAM_CODEC = StreamCodec.unit(INSTANCE);
}
