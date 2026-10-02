package com.dingdongji.mod.network;

import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.item.component.CreateTemplateMode;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SelectTemplateModePacket(int containerId, String mode) implements CustomPacketPayload {
   public static final Type<SelectTemplateModePacket> TYPE = new Type(ResourceLocation.parse("dingdongji:select_template_mode"));
   private static final ResourceLocation CREATE_TEMPLATE_ID = ResourceLocation.parse("dingdongji:create_template");
   public static final StreamCodec<RegistryFriendlyByteBuf, SelectTemplateModePacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, SelectTemplateModePacket::containerId, ByteBufCodecs.STRING_UTF8, SelectTemplateModePacket::mode, SelectTemplateModePacket::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handle(SelectTemplateModePacket packet, IPayloadContext context) {
      context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer serverPlayer) {
            AbstractContainerMenu menu = serverPlayer.containerMenu;
            if (menu != null && menu.containerId == packet.containerId()) {
               applyMode(menu, packet.mode(), serverPlayer);
            }
         }
      });
   }

   private static void applyMode(AbstractContainerMenu menu, String mode, ServerPlayer serverPlayer) {
      if (!CreateTemplateMode.isValid(mode)) {
         return;
      }
      try {
         Class<?> adj = Class.forName("dev.dubhe.anvilcraft.inventory.AdjacentSmithingMenu");
         if (adj.isInstance(menu)) {
            boolean isRoyal = Class.forName("dev.dubhe.anvilcraft.inventory.RoyalSmithingMenu").isInstance(menu);
            String targetMode = isRoyal ? "alpha" : mode;
            if (!ModItems.isCreateTemplate(menu.getSlot(0).getItem())) {
               try {
                  Method borrow = adj.getDeclaredMethod("borrowTemplate", ServerPlayer.class, ResourceLocation.class);
                  borrow.setAccessible(true);
                  borrow.invoke(menu, serverPlayer, CREATE_TEMPLATE_ID);
               } catch (Exception var15) {
               }
            }

            ItemStack template = menu.getSlot(0).getItem();
            if (ModItems.isCreateTemplate(template)) {
               template.set((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get(), new CreateTemplateMode(targetMode));
            }

            try {
               Field bf = adj.getDeclaredField("borrowedTemplateStack");
               bf.setAccessible(true);
               if (bf.get(menu) instanceof ItemStack bs && ModItems.isCreateTemplate(bs)) {
                  bs.set((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get(), new CreateTemplateMode(targetMode));
               }
            } catch (Exception var14) {
            }

            menu.slotsChanged(menu.getSlot(0).container);
            menu.broadcastChanges();
            return;
         }
      } catch (Exception var16) {
      }

      try {
         Class<?> tc = Class.forName("dev.dubhe.anvilcraft.inventory.TranscendenceSmithingMenu");
         if (tc.isInstance(menu)) {
            ItemStack target = null;

            try {
               Field tf = tc.getDeclaredField("templates");
               tf.setAccessible(true);
               Object templatesObj = tf.get(menu);
               if (templatesObj instanceof List) {
                  for (Object o : (List)templatesObj) {
                     if (o instanceof ItemStack) {
                        ItemStack t = (ItemStack)o;
                        if (ModItems.isCreateTemplate(t)) {
                           CreateTemplateMode m = (CreateTemplateMode)t.getOrDefault(
                              (DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get(), CreateTemplateMode.DEFAULT
                           );
                           if (m.mode().equals(mode)) {
                              target = t;
                              break;
                           }
                        }
                     }
                  }
               }
            } catch (Exception var17) {
            }

            Field sf = tc.getDeclaredField("selectedTemplate");
            sf.setAccessible(true);
            if (target == null && sf.get(menu) instanceof ItemStack cs && ModItems.isCreateTemplate(cs)) {
               target = cs.copyWithCount(1);
            }

            if (target != null) {
               target.set((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get(), new CreateTemplateMode(mode));
               sf.set(menu, target.copyWithCount(1));
               menu.broadcastChanges();

               try {
                  menu.slotsChanged(menu.getSlot(0).container);
               } catch (Exception var13) {
               }

               try {
                  menu.slotsChanged(menu.getSlot(3).container);
               } catch (Exception var12) {
               }
            }
         }
      } catch (Exception var18) {
      }
   }
}
