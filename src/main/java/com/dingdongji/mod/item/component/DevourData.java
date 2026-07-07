package com.dingdongji.mod.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record DevourData(int kills) {
    public static final Codec<DevourData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("kills").forGetter(DevourData::kills)
            ).apply(instance, DevourData::new)
    );

    public static final StreamCodec<ByteBuf, DevourData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, DevourData::kills,
                    DevourData::new
            );
}
