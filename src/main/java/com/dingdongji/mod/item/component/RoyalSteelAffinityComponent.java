package com.dingdongji.mod.item.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record RoyalSteelAffinityComponent() {
   public static final RoyalSteelAffinityComponent INSTANCE = new RoyalSteelAffinityComponent();
   public static final Codec<RoyalSteelAffinityComponent> CODEC = Codec.unit(INSTANCE);
   public static final StreamCodec<ByteBuf, RoyalSteelAffinityComponent> STREAM_CODEC = StreamCodec.unit(INSTANCE);
}
