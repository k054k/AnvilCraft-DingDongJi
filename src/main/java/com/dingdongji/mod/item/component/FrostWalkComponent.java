package com.dingdongji.mod.item.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record FrostWalkComponent() {
   public static final FrostWalkComponent INSTANCE = new FrostWalkComponent();
   public static final Codec<FrostWalkComponent> CODEC = Codec.unit(INSTANCE);
   public static final StreamCodec<ByteBuf, FrostWalkComponent> STREAM_CODEC = StreamCodec.unit(INSTANCE);
}
