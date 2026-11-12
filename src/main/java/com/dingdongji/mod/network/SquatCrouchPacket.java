package com.dingdongji.mod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

/**
 * 蹲起式发电器：客户端蹲下状态上报。
 * 创造模式飞行时服务端 isCrouching() 不同步（Shift 被用作下降），
 * 故由客户端本地检测并在状态变化时上报，生存模式下作冗余。
 */
public record SquatCrouchPacket(boolean crouching) implements CustomPacketPayload {
   public static final Type<SquatCrouchPacket> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("dingdongji", "squat_crouch"));
   public static final StreamCodec<FriendlyByteBuf, SquatCrouchPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, SquatCrouchPacket::crouching, SquatCrouchPacket::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
