package com.dingdongji.mod.item.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record LavaWalkerComponent() {
   public static final LavaWalkerComponent INSTANCE = new LavaWalkerComponent();
   public static final Codec<LavaWalkerComponent> CODEC = Codec.unit(INSTANCE);
   public static final StreamCodec<ByteBuf, LavaWalkerComponent> STREAM_CODEC = StreamCodec.unit(INSTANCE);
}
