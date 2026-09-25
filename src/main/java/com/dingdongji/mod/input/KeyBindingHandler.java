package com.dingdongji.mod.input;

import com.dingdongji.mod.client.CreateTemplateWheel;
import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.item.component.CreateTemplateMode;
import com.dingdongji.mod.network.SwitchTemplateModePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.InputEvent.Key;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(
   value = {Dist.CLIENT},
   bus = Bus.GAME
)
public class KeyBindingHandler {
   private static boolean altWheelOpened = false;

   @SubscribeEvent
   public static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         ModKeyBindings.tick(mc);
         long window = mc.getWindow().getWindow();
         boolean altDown = GLFW.glfwGetKey(window, 342) == 1 || GLFW.glfwGetKey(window, 346) == 1;
         if (altDown && !altWheelOpened && mc.screen == null) {
            ItemStack mainHand = mc.player.getMainHandItem();
            ItemStack offhand = mc.player.getOffhandItem();
            if (mainHand.is((Item)ModItems.CREATE_TEMPLATE.get())) {
               CreateTemplateWheel.press(InteractionHand.MAIN_HAND);
               altWheelOpened = true;
            } else if (offhand.is((Item)ModItems.CREATE_TEMPLATE.get())) {
               CreateTemplateWheel.press(InteractionHand.OFF_HAND);
               altWheelOpened = true;
            }
         } else if (!altDown && altWheelOpened) {
            CreateTemplateWheel.release();
            altWheelOpened = false;
         }
      }
   }

   @SubscribeEvent
   public static void onKeyInput(Key event) {
      if ((event.getKey() == 342 || event.getKey() == 346) && event.getAction() == 0 && altWheelOpened) {
         CreateTemplateWheel.release();
         altWheelOpened = false;
      }
   }

   @SubscribeEvent
   public static void onMouseButton(net.neoforged.neoforge.client.event.InputEvent.MouseButton.Post event) {
      if (event.getButton() == 0 && event.getAction() == 1) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.screen == null) {
            for (InteractionHand hand : InteractionHand.values()) {
               ItemStack stack = mc.player.getItemInHand(hand);
               if (stack.is((Item)ModItems.CREATE_TEMPLATE.get())) {
                  CreateTemplateMode current = (CreateTemplateMode)stack.get((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get());
                  if (current != null && !CreateTemplateMode.ALPHA.equals(current)) {
                     stack.set((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get(), CreateTemplateMode.ALPHA);
                     PacketDistributor.sendToServer(new SwitchTemplateModePacket(hand, CreateTemplateMode.ALPHA.mode()), new CustomPacketPayload[0]);
                     return;
                  }

                  return;
               }
            }
         }
      }
   }
}
