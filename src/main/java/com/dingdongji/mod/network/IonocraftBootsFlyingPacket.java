package com.dingdongji.mod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record IonocraftBootsFlyingPacket(int playerId, int mode) implements CustomPacketPayload {
   public static final Type<IonocraftBootsFlyingPacket> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("dingdongji", "ionocraft_boots_flying"));
   public static final StreamCodec<FriendlyByteBuf, IonocraftBootsFlyingPacket> STREAM_CODEC = StreamCodec.of((buf, packet) -> {
      buf.writeVarInt(packet.playerId);
      buf.writeVarInt(packet.mode);
   }, buf -> new IonocraftBootsFlyingPacket(buf.readVarInt(), buf.readVarInt()));

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
