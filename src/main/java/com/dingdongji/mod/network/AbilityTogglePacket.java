package com.dingdongji.mod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record AbilityTogglePacket(boolean doublePress) implements CustomPacketPayload {
   public static final Type<AbilityTogglePacket> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("dingdongji", "ability_toggle"));
   public static final StreamCodec<FriendlyByteBuf, AbilityTogglePacket> STREAM_CODEC = StreamCodec.of(
      (buf, packet) -> buf.writeBoolean(packet.doublePress), buf -> new AbilityTogglePacket(buf.readBoolean())
   );

   public AbilityTogglePacket() {
      this(false);
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
