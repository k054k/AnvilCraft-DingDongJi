package com.dingdongji.mod.item.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * 壁垒II组件 - 超限合金胸甲：对大部分伤害大幅减伤，无视魔法/虚空/接触/爆炸伤害；生命值低时紧急治愈
 */
public record BarrierIIComponent() {
    public static final BarrierIIComponent INSTANCE = new BarrierIIComponent();
    public static final Codec<BarrierIIComponent> CODEC = Codec.unit(INSTANCE);
    public static final StreamCodec<ByteBuf, BarrierIIComponent> STREAM_CODEC = StreamCodec.unit(INSTANCE);
}
