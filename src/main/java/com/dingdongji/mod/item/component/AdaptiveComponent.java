package com.dingdongji.mod.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 自适应组件 - 标记胸甲可以自适应伤害
 * 记录每种伤害类型的计数和免疫冷却时间
 *
 * 注意：当前未被注册使用，保留供后续参考
 */
public record AdaptiveComponent(Map<String, AdaptiveEntry> entries) {

    public static final Codec<AdaptiveComponent> CODEC =
            Codec.unboundedMap(Codec.STRING, AdaptiveEntry.CODEC)
                    .xmap(AdaptiveComponent::new, AdaptiveComponent::entries);

    // 手动编解码 UTF-8 字符串（1.21.1 没有 StreamCodec.STRING_UTF8）
    private static final StreamCodec<ByteBuf, String> UTF8_CODEC = new StreamCodec<>() {
        @Override
        public String decode(ByteBuf buf) {
            int len = buf.readInt();
            byte[] bytes = new byte[len];
            buf.readBytes(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }

        @Override
        public void encode(ByteBuf buf, String value) {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            buf.writeInt(bytes.length);
            buf.writeBytes(bytes);
        }
    };

    // 手动实现 StreamCodec，使用 INT 编解码 map 大小
    public static final StreamCodec<ByteBuf, AdaptiveComponent> STREAM_CODEC =
            new StreamCodec<ByteBuf, AdaptiveComponent>() {
                @Override
                public AdaptiveComponent decode(ByteBuf buf) {
                    int size = buf.readInt();
                    Map<String, AdaptiveEntry> map = new HashMap<>(size);
                    for (int i = 0; i < size; i++) {
                        String key = UTF8_CODEC.decode(buf);
                        AdaptiveEntry value = AdaptiveEntry.STREAM_CODEC.decode(buf);
                        map.put(key, value);
                    }
                    return new AdaptiveComponent(map);
                }

                @Override
                public void encode(ByteBuf buf, AdaptiveComponent component) {
                    buf.writeInt(component.entries().size());
                    for (Map.Entry<String, AdaptiveEntry> entry : component.entries().entrySet()) {
                        UTF8_CODEC.encode(buf, entry.getKey());
                        AdaptiveEntry.STREAM_CODEC.encode(buf, entry.getValue());
                    }
                }
            };

    public AdaptiveComponent() {
        this(new HashMap<>());
    }

    /**
     * 伤害类型计数条目
     * @param count 当前计数（同类型伤害次数）
     * @param immuneUntil 免疫截止的游戏时间（-1 表示未免疫）
     */
    public record AdaptiveEntry(int count, long immuneUntil) {
        public static final Codec<AdaptiveEntry> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.INT.fieldOf("count").forGetter(AdaptiveEntry::count),
                        Codec.LONG.fieldOf("immune_until").forGetter(AdaptiveEntry::immuneUntil)
                ).apply(instance, AdaptiveEntry::new)
        );

        // 手动实现 StreamCodec，避免 StreamCodec.composite 在内部类中的初始化问题
        public static final StreamCodec<ByteBuf, AdaptiveEntry> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public AdaptiveEntry decode(ByteBuf buf) {
                int count = buf.readInt();
                long immuneUntil = buf.readLong();
                return new AdaptiveEntry(count, immuneUntil);
            }

            @Override
            public void encode(ByteBuf buf, AdaptiveEntry entry) {
                buf.writeInt(entry.count());
                buf.writeLong(entry.immuneUntil());
            }
        };
    }
}
