package com.dingdongji.mod.network;

import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.item.component.CreateTemplateMode;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SwitchTemplateModePacket(InteractionHand hand, String mode) implements CustomPacketPayload {
   public static final Type<SwitchTemplateModePacket> TYPE = new Type(ResourceLocation.parse("dingdongji:switch_template_mode"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SwitchTemplateModePacket> STREAM_CODEC = StreamCodec.composite(
      new StreamCodec<RegistryFriendlyByteBuf, InteractionHand>() {
         public InteractionHand decode(RegistryFriendlyByteBuf buf) {
            return (InteractionHand)buf.readEnum(InteractionHand.class);
         }

         public void encode(RegistryFriendlyByteBuf buf, InteractionHand hand) {
            buf.writeEnum(hand);
         }
      }, SwitchTemplateModePacket::hand, ByteBufCodecs.STRING_UTF8, SwitchTemplateModePacket::mode, SwitchTemplateModePacket::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handle(SwitchTemplateModePacket packet, IPayloadContext context) {
      context.enqueueWork(() -> {
         Player player = context.player();
         if (player != null) {
            ItemStack stack = player.getItemInHand(packet.hand());
            if (stack.is((Item)ModItems.CREATE_TEMPLATE.get())) {
               CreateTemplateMode newMode = new CreateTemplateMode(packet.mode());
               stack.set((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get(), newMode);
            }
         }
      });
   }
}
