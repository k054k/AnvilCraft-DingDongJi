package com.dingdongji.mod.input;

import com.dingdongji.mod.client.ArmorFunctionWheel;
import com.dingdongji.mod.client.CreateTemplateWheel;
import com.dingdongji.mod.item.ModItems;
import dev.dubhe.anvilcraft.client.init.ModKeyMappings;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.InputEvent.Key;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(
   value = {Dist.CLIENT},
   bus = Bus.GAME
)
public class KeyBindingHandler {
   private static boolean altWheelOpened = false;
   private static boolean armorWheelOpened = false;

   @SubscribeEvent
   public static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         ModKeyBindings.tick(mc);
         long window = mc.getWindow().getWindow();
         // 轮盘触发键：本体"切换工具模式"键位绑定（默认左Alt）+ 左/右 Alt 硬编码兜底
         boolean wheelDown = GLFW.glfwGetKey(window, 342) == 1
            || GLFW.glfwGetKey(window, 346) == 1
            || ModKeyMappings.SWITCH_TOOL_MODE.get().isDown();
         if (wheelDown && !altWheelOpened && !armorWheelOpened && mc.screen == null) {
            ItemStack mainHand = mc.player.getMainHandItem();
            ItemStack offhand = mc.player.getOffhandItem();
            if (mainHand.is((Item)ModItems.CREATE_TEMPLATE.get())) {
               CreateTemplateWheel.press(InteractionHand.MAIN_HAND);
               altWheelOpened = true;
            } else if (offhand.is((Item)ModItems.CREATE_TEMPLATE.get())) {
               CreateTemplateWheel.press(InteractionHand.OFF_HAND);
               altWheelOpened = true;
            } else if (ArmorFunctionWheel.supports(mainHand)) {
               ArmorFunctionWheel.press(InteractionHand.MAIN_HAND);
               armorWheelOpened = true;
            } else if (ArmorFunctionWheel.supports(offhand)) {
               ArmorFunctionWheel.press(InteractionHand.OFF_HAND);
               armorWheelOpened = true;
            }
         } else if (!wheelDown) {
            if (altWheelOpened) {
               CreateTemplateWheel.release();
               altWheelOpened = false;
            }
            if (armorWheelOpened) {
               ArmorFunctionWheel.release();
               armorWheelOpened = false;
            }
         }
      }
   }

   @SubscribeEvent
   public static void onKeyInput(Key event) {
      if ((event.getKey() == 342 || event.getKey() == 346) && event.getAction() == 0) {
         if (altWheelOpened) {
            CreateTemplateWheel.release();
            altWheelOpened = false;
         }
         if (armorWheelOpened) {
            ArmorFunctionWheel.release();
            armorWheelOpened = false;
         }
      }
   }
}
