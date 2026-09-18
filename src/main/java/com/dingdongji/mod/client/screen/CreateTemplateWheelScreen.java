package com.dingdongji.mod.client.screen;

import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.item.component.CreateTemplateMode;
import com.dingdongji.mod.network.SwitchTemplateModePacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.network.PacketDistributor;

public class CreateTemplateWheelScreen extends Screen {
   private final InteractionHand hand;
   public WheelWidget wheel;
   private static List<CreateTemplateWheelScreen.ModeEntry> cachedModes = null;
   private static List<WheelWidget.SectionBuilder> cachedBuilders = null;

   public CreateTemplateWheelScreen(InteractionHand hand) {
      super(Component.literal("Select Template Mode"));
      this.hand = hand;
   }

   public boolean isPauseScreen() {
      return false;
   }

   private static List<CreateTemplateWheelScreen.ModeEntry> getModes() {
      if (cachedModes != null) {
         return cachedModes;
      } else {
         boolean hasEZ = BuiltInRegistries.ITEM.get(ResourceLocation.parse("anvilcraft:permutation_smithing_template")) != Items.AIR;
         cachedModes = new ArrayList<>();
         cachedBuilders = new ArrayList<>();
         addMode(CreateTemplateMode.ALPHA, "α 普通");
         addMode(CreateTemplateMode.BETA, "β 二合一");
         addMode(CreateTemplateMode.GAMMA, "γ 四合一");
         addMode(CreateTemplateMode.DELTA, "δ 八合一");
         if (hasEZ) {
            addMode(CreateTemplateMode.EPSILON, "ε 嬗变");
            addMode(CreateTemplateMode.ZETA, "ζ 形变");
         }

         return cachedModes;
      }
   }

   private static void addMode(CreateTemplateMode mode, String label) {
      cachedModes.add(new CreateTemplateWheelScreen.ModeEntry(mode, label));
      ItemStack icon = new ItemStack((ItemLike)ModItems.CREATE_TEMPLATE.get());
      icon.set((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get(), mode);
      cachedBuilders.add(new WheelWidget.SectionBuilder(Component.literal(label), (graphics, x, y, w, h) -> graphics.renderItem(icon, x, y, 9910597)));
   }

   protected void init() {
      this.clearWidgets();
      getModes();
      int size = 140;
      int leftPos = (this.width - size) / 2;
      int topPos = (this.height - size) / 2;
      WheelWidget w = new WheelWidget(leftPos, topPos, size, cachedBuilders).setCurrentIndex(this.wheel != null ? this.wheel.getSelectedIndex() : 0);
      w.open();
      this.wheel = (WheelWidget)this.addRenderableWidget(w);
   }

   public void removed() {
      super.removed();
      if (this.wheel != null) {
         int index = this.wheel.getSelectedIndex();
         if (index >= 0 && index < cachedModes.size()) {
            PacketDistributor.sendToServer(new SwitchTemplateModePacket(this.hand, cachedModes.get(index).mode().mode()), new CustomPacketPayload[0]);
         }
      }
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      for (Renderable renderable : this.renderables) {
         renderable.render(guiGraphics, mouseX, mouseY, partialTick);
      }
   }

   public static void tryOpen(ItemStack stack) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.screen == null) {
         InteractionHand hand = InteractionHand.MAIN_HAND;
         if (!stack.is((Item)ModItems.CREATE_TEMPLATE.get())) {
            hand = InteractionHand.OFF_HAND;
            stack = mc.player.getOffhandItem();
         }

         if (stack.is((Item)ModItems.CREATE_TEMPLATE.get())) {
            if (mc.level != null && mc.level.getGameTime() % 200L == 0L) {
               cachedModes = null;
               cachedBuilders = null;
            }

            mc.setScreen(new CreateTemplateWheelScreen(hand));
         }
      }
   }

   private static record ModeEntry(CreateTemplateMode mode, String label) {
   }
}
