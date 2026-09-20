package com.dingdongji.mod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record AbilityStateSyncPacket(boolean lavaWalker, boolean frostSlide, int helmetMode, boolean phaseVertical) implements CustomPacketPayload {
   public static final Type<AbilityStateSyncPacket> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("dingdongji", "ability_state_sync"));
   public static final StreamCodec<FriendlyByteBuf, AbilityStateSyncPacket> STREAM_CODEC = StreamCodec.of((buf, packet) -> {
      buf.writeBoolean(packet.lavaWalker);
      buf.writeBoolean(packet.frostSlide);
      buf.writeVarInt(packet.helmetMode);
      buf.writeBoolean(packet.phaseVertical);
   }, buf -> new AbilityStateSyncPacket(buf.readBoolean(), buf.readBoolean(), buf.readVarInt(), buf.readBoolean()));

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
