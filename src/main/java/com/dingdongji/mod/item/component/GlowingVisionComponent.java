package com.dingdongji.mod.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 透视组件 - 标记头盔可以透视敌对生物
 * @param range 检测范围（格）
 */
public record GlowingVisionComponent(int range) {
    public static final Codec<GlowingVisionComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("range").forGetter(GlowingVisionComponent::range)
            ).apply(instance, GlowingVisionComponent::new)
    );

    public static final StreamCodec<ByteBuf, GlowingVisionComponent> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, GlowingVisionComponent::range,
                    GlowingVisionComponent::new
            );

    /** 默认范围 20 格 */
    public static final GlowingVisionComponent DEFAULT = new GlowingVisionComponent(20);
}
