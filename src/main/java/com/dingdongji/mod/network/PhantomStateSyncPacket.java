package com.dingdongji.mod.network;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

/**
 * 幻影状态广播包：某玩家进入/退出幻影时，服务端把 {玩家UUID, 是否幻影}
 * 发给追踪该玩家的所有客户端（含本人），使"幻灵色半透明本体渲染"在
 * 所有观察者视角一致。
 *
 * <p>单独建包而非复用 {@link AbilityStateSyncPacket}：后者语义为"本人
 * 能力开关"，只发本人；幻影是"他人可见的世界状态"，方向与受众都不同。
 */
public record PhantomStateSyncPacket(UUID playerId, boolean phantom) implements CustomPacketPayload {
   public static final Type<PhantomStateSyncPacket> TYPE =
      new Type(ResourceLocation.fromNamespaceAndPath("dingdongji", "phantom_state_sync"));
   public static final StreamCodec<FriendlyByteBuf, PhantomStateSyncPacket> STREAM_CODEC = StreamCodec.of(
      (buf, packet) -> {
         buf.writeUUID(packet.playerId());
         buf.writeBoolean(packet.phantom());
      },
      buf -> new PhantomStateSyncPacket(buf.readUUID(), buf.readBoolean())
   );

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
