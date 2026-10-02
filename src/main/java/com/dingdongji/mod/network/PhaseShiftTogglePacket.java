package com.dingdongji.mod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record PhaseShiftTogglePacket() implements CustomPacketPayload {
   public static final Type<PhaseShiftTogglePacket> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("dingdongji", "phase_shift_toggle"));
   public static final StreamCodec<FriendlyByteBuf, PhaseShiftTogglePacket> STREAM_CODEC = StreamCodec.unit(new PhaseShiftTogglePacket());

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
