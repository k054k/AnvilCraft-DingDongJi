package com.dingdongji.mod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

/** 完整套装开关：超限相位偏移与幻灵幻影共用一个按键，服务端按当前穿着分发。 */
public record FullSetTogglePacket() implements CustomPacketPayload {
   public static final Type<FullSetTogglePacket> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("dingdongji", "full_set_toggle"));
   public static final StreamCodec<FriendlyByteBuf, FullSetTogglePacket> STREAM_CODEC = StreamCodec.unit(new FullSetTogglePacket());

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
