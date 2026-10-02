package com.dingdongji.mod.client;

import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.item.component.CreateTemplateMode;
import com.dingdongji.mod.network.SwitchTemplateModePacket;
import dev.anvilcraft.lib.v2.wheel.api.WheelMenuBuilder;
import dev.anvilcraft.lib.v2.wheel.api.WheelMenuModel;
import dev.anvilcraft.lib.v2.wheel.api.WheelSelectionEffect;
import dev.anvilcraft.lib.v2.wheel.client.input.WheelScreenController;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 创造模板模式选择轮盘，复用铁砧本体（共振器/多功能工具/口袋）同一套
 * anvillib wheel API：按住 Alt 由 {@link WheelScreenController} 弹出，
 * 松手时触发高亮项动作并关闭，外观为毛玻璃圆盘 + 扇形高亮。
 * 运行期 anvillib-wheel 由铁砧 jarJar 提供，本模组不打包该库。
 */
public final class CreateTemplateWheel {
   private static final WheelScreenController CONTROLLER = new WheelScreenController();

   private CreateTemplateWheel() {
   }

   public static void press(InteractionHand hand) {
      CONTROLLER.onHoldKeyPressed(buildModel(hand));
   }

   public static void release() {
      CONTROLLER.onHoldKeyReleased();
   }

   private static WheelMenuModel buildModel(InteractionHand hand) {
      boolean hasEpsilonZeta = BuiltInRegistries.ITEM.get(ResourceLocation.parse("anvilcraft:permutation_smithing_template")) != Items.AIR;
      List<CreateTemplateMode> modes = new ArrayList<>();
      modes.add(CreateTemplateMode.ALPHA);
      modes.add(CreateTemplateMode.BETA);
      modes.add(CreateTemplateMode.GAMMA);
      modes.add(CreateTemplateMode.DELTA);
      if (hasEpsilonZeta) {
         modes.add(CreateTemplateMode.EPSILON);
         modes.add(CreateTemplateMode.ZETA);
      }

      WheelMenuBuilder builder = WheelMenuBuilder.create()
         .selectionEffect(WheelSelectionEffect.ANNULAR_SECTOR)
         .slotsPerPage(modes.size());

      for (CreateTemplateMode mode : modes) {
         addEntry(builder, hand, mode);
      }

      return builder.build();
   }

   private static void addEntry(WheelMenuBuilder builder, InteractionHand hand, CreateTemplateMode mode) {
      ItemStack icon = new ItemStack((Item) ModItems.CREATE_TEMPLATE.get());
      icon.set((DataComponentType) ModComponents.CREATE_TEMPLATE_MODE.get(), mode);
      Component label = Component.literal(displayLabel(mode));
      // WheelWidget 已将 pose 平移到扇区中心，按本体 renderWheelItem 的做法在 (-8,-8) 绘制 16x16 图标
      builder.action(mode.mode(), label, (graphics, pose, x, y) -> graphics.renderItem(icon, -8, -8),
         ctx -> PacketDistributor.sendToServer(new SwitchTemplateModePacket(hand, mode.mode())));
   }

   private static String displayLabel(CreateTemplateMode mode) {
      return switch (mode.mode()) {
         case "alpha" -> "α 普通";
         case "beta" -> "β 二合一";
         case "gamma" -> "γ 四合一";
         case "delta" -> "δ 八合一";
         case "epsilon" -> "ε 嬗变";
         case "zeta" -> "ζ 形变";
         default -> mode.getDisplayName();
      };
   }
}
