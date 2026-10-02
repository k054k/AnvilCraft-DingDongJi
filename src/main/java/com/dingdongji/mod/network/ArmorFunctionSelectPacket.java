package com.dingdongji.mod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

/**
 * 手持功能盔甲经轮盘直接设置功能状态：
 * state 含义按物品而定（布尔功能 0=关 1=开；头盔适应 0-5；中子屏罩 0-3；蹈虚 1/2）。
 */
public record ArmorFunctionSelectPacket(InteractionHand hand, int state) implements CustomPacketPayload {
   public static final Type<ArmorFunctionSelectPacket> TYPE = new Type(ResourceLocation.parse("dingdongji:armor_function_select"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ArmorFunctionSelectPacket> STREAM_CODEC = StreamCodec.composite(
      new StreamCodec<RegistryFriendlyByteBuf, InteractionHand>() {
         public InteractionHand decode(RegistryFriendlyByteBuf buf) {
            return (InteractionHand)buf.readEnum(InteractionHand.class);
         }

         public void encode(RegistryFriendlyByteBuf buf, InteractionHand hand) {
            buf.writeEnum(hand);
         }
      }, ArmorFunctionSelectPacket::hand, ByteBufCodecs.VAR_INT, ArmorFunctionSelectPacket::state, ArmorFunctionSelectPacket::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
