package com.dingdongji.mod.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record AccumulateData(int ticks) {
   public static final Codec<AccumulateData> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(Codec.INT.fieldOf("ticks").forGetter(AccumulateData::ticks)).apply(instance, AccumulateData::new)
   );
   public static final StreamCodec<ByteBuf, AccumulateData> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT, AccumulateData::ticks, AccumulateData::new);
   public static final AccumulateData DEFAULT = new AccumulateData(0);

   public AccumulateData(int ticks) {
      ticks = Math.max(0, ticks);
      this.ticks = ticks;
   }
}
