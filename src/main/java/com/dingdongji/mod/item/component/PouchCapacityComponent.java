package com.dingdongji.mod.item.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 护腿上的口袋容量标记。
 * <ul>
 *   <li>0 = 无口袋</li>
 *   <li>6 = 小口袋</li>
 *   <li>12 = 大口袋</li>
 *   <li>24 = 耐候航天套 + 大口袋（铁砧本体 SpaceSuit 12 + DDJ 大口袋 +12）</li>
 * </ul>
 * 实际栏位数由铁砧工艺 PocketInventory.capacity() + DDJ 扩展 Mixin 联合决定。
 */
public record PouchCapacityComponent(int capacity) {
   public static final PouchCapacityComponent NONE = new PouchCapacityComponent(0);
   public static final PouchCapacityComponent SMALL = new PouchCapacityComponent(6);
   public static final PouchCapacityComponent BIG = new PouchCapacityComponent(12);
   public static final PouchCapacityComponent SPACE_SUIT_BIG = new PouchCapacityComponent(24);

   public static final Codec<PouchCapacityComponent> CODEC =
      Codec.intRange(0, 24).xmap(PouchCapacityComponent::new, PouchCapacityComponent::capacity);

   public static final StreamCodec<ByteBuf, PouchCapacityComponent> STREAM_CODEC =
      ByteBufCodecs.VAR_INT.map(PouchCapacityComponent::new, PouchCapacityComponent::capacity);
}
