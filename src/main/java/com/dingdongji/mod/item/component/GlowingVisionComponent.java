package com.dingdongji.mod.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record GlowingVisionComponent(int range) {
   public static final Codec<GlowingVisionComponent> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(Codec.INT.fieldOf("range").forGetter(GlowingVisionComponent::range)).apply(instance, GlowingVisionComponent::new)
   );
   public static final StreamCodec<ByteBuf, GlowingVisionComponent> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.INT, GlowingVisionComponent::range, GlowingVisionComponent::new
   );
   public static final GlowingVisionComponent DEFAULT = new GlowingVisionComponent(20);
}
