package com.dingdongji.mod.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.joml.Vector2f;

public class WheelWidget extends AbstractWidget {
   private static final float RADIUS = 48.0F;
   private static final int ANIM_MS = 150;
   private static final int CLOSE_MS = 100;
   private final Minecraft mc = Minecraft.getInstance();
   private final Vector2f center;
   private final List<WheelWidget.Section> sections = new ArrayList<>();
   private long openTime;
   private boolean opening = false;
   private boolean closing = false;
   private int selectedIndex = 0;

   public WheelWidget(int x, int y, int size, List<WheelWidget.SectionBuilder> builders) {
      super(x, y, size, size, Component.empty());
      this.center = new Vector2f((float)x + (float)size / 2.0F, (float)y + (float)size / 2.0F);
      float degreeEach = 360.0F / (float)builders.size();

      for (int i = 0; i < builders.size(); i++) {
         WheelWidget.SectionBuilder b = builders.get(i);
         float rad = (float)Math.toRadians((double)(degreeEach * (float)i));
         float sx = this.center.x + (float)Math.sin((double)rad) * 48.0F;
         float sy = this.center.y - (float)Math.cos((double)rad) * 48.0F;
         this.sections.add(new WheelWidget.Section(b.name(), b.renderer(), new Vector2f(sx, sy)));
      }
   }

   public WheelWidget setCurrentIndex(int index) {
      if (index >= 0 && index < this.sections.size()) {
         this.selectedIndex = index;
      }

      return this;
   }

   public int getSelectedIndex() {
      return this.selectedIndex;
   }

   public boolean isClosing() {
      return this.closing;
   }

   public void open() {
      this.openTime = System.currentTimeMillis();
      this.opening = true;
      this.closing = false;
   }

   public int close() {
      if (this.closing) {
         return this.selectedIndex;
      } else {
         this.openTime = System.currentTimeMillis();
         this.opening = false;
         this.closing = true;
         return this.selectedIndex;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.closing) {
         return false;
      } else {
         if (scrollY > 0.0) {
            this.selectedIndex = (this.selectedIndex + 1) % this.sections.size();
         } else if (scrollY < 0.0) {
            this.selectedIndex = (this.selectedIndex - 1 + this.sections.size()) % this.sections.size();
         }

         return true;
      }
   }

   private void updateHover(double mouseX, double mouseY) {
      if (!this.closing) {
         float bestDist = Float.MAX_VALUE;
         int bestIdx = -1;

         for (int i = 0; i < this.sections.size(); i++) {
            WheelWidget.Section s = this.sections.get(i);
            float dx = (float)mouseX - s.pos().x;
            float dy = (float)mouseY - s.pos().y;
            float dist = dx * dx + dy * dy;
            if (dist < bestDist) {
               bestDist = dist;
               bestIdx = i;
            }
         }

         if (bestIdx >= 0 && bestDist < 400.0F) {
            this.selectedIndex = bestIdx;
         }
      }
   }

   protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      this.updateHover((double)mouseX, (double)mouseY);
      long elapsed = System.currentTimeMillis() - this.openTime;
      if (this.closing) {
         float progress = Math.max(0.0F, 1.0F - (float)elapsed / 100.0F);
         progress = easeOutCubic(progress);
         this.renderSections(guiGraphics, progress);
         if (progress <= 0.0F) {
            this.mc.setScreen(null);
         }
      } else if (this.opening) {
         float progress = Math.min(1.0F, (float)elapsed / 150.0F);
         progress = easeOutCubic(progress);
         this.renderSections(guiGraphics, progress);
      }
   }

   private static float easeOutCubic(float t) {
      return (float)(1.0 - Math.pow((double)(1.0F - t), 3.0));
   }

   private void renderSections(GuiGraphics guiGraphics, float progress) {
      for (int i = 0; i < this.sections.size(); i++) {
         WheelWidget.Section s = this.sections.get(i);
         float sx = (s.pos().x - this.center.x) * progress + this.center.x;
         float sy = (s.pos().y - this.center.y) * progress + this.center.y;
         boolean hovered = i == this.selectedIndex;
         s.renderer().render(guiGraphics, (int)sx - 8, (int)sy - 8, 16, 16);
         String label = s.name().getString();
         int tw = this.mc.font.width(label);
         float tx = sx - (float)tw / 2.0F;
         float ty = sy + 11.0F;
         int alpha = Math.min(255, (int)(progress * 255.0F));
         int color = hovered ? 16777215 : 11184810;
         int argb = alpha << 24 | color & 16777215;
         guiGraphics.drawString(this.mc.font, label, (int)tx, (int)ty, argb, false);
      }
   }

   protected void updateWidgetNarration(NarrationElementOutput output) {
   }

   public static record Section(Component name, WheelWidget.SectionRenderer renderer, Vector2f pos) {
   }

   public static class SectionBuilder {
      private final Component name;
      private final WheelWidget.SectionRenderer renderer;

      public SectionBuilder(Component name, WheelWidget.SectionRenderer renderer) {
         this.name = name;
         this.renderer = renderer;
      }

      public Component name() {
         return this.name;
      }

      public WheelWidget.SectionRenderer renderer() {
         return this.renderer;
      }
   }

   @FunctionalInterface
   public interface SectionRenderer {
      void render(GuiGraphics var1, int var2, int var3, int var4, int var5);
   }
}
