package com.dingdongji.mod.client;

import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.network.ArmorFunctionSelectPacket;
import dev.anvilcraft.lib.v2.wheel.api.WheelMenuBuilder;
import dev.anvilcraft.lib.v2.wheel.api.WheelMenuModel;
import dev.anvilcraft.lib.v2.wheel.api.WheelSelectionEffect;
import dev.anvilcraft.lib.v2.wheel.client.input.WheelScreenController;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 功能盔甲开关轮盘，与 {@link CreateTemplateWheel} 同一套 anvillib wheel API：
 * 手持功能盔甲按住本体"切换工具模式"键弹出，松手触发高亮项直接设置状态。
 * 套装功能（相位偏移）不在此列——套装无法通过单独一件手持物品切换。
 */
public final class ArmorFunctionWheel {
   private static final WheelScreenController CONTROLLER = new WheelScreenController();

   private ArmorFunctionWheel() {
   }

   /** 该物品是否有可经轮盘切换的手持功能。 */
   public static boolean supports(ItemStack stack) {
      return stack.is((Item)ModItems.ROYAL_STEEL_BOOTS.get())
         || stack.is((Item)ModItems.EMBER_METAL_BOOTS.get())
         || stack.is((Item)ModItems.FROST_METAL_BOOTS.get())
         || stack.is((Item)ModItems.SPECTRAL_BOOTS.get())
         || stack.is((Item)ModItems.TRANSCENDIUM_BOOTS.get())
         || stack.is((Item)ModItems.TRANSCENDIUM_HELMET.get())
         || stack.is((Item)ModItems.TRANSCENDIUM_LEGGINGS.get());
   }

   public static void press(InteractionHand hand) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         CONTROLLER.onHoldKeyPressed(buildModel(hand, mc.player.getItemInHand(hand)));
      }
   }

   public static void release() {
      CONTROLLER.onHoldKeyReleased();
   }

   private record Entry(String label, int state, ItemStack icon, ResourceLocation texture) {
      private Entry(String label, int state, ItemStack icon) {
         this(label, state, icon, null);
      }

      /** 使用原版状态效果贴图（18x18）作为图标的项，如 glowing / night_vision。 */
      private static Entry ofEffect(String label, int state, String effectPath) {
         return new Entry(label, state, ItemStack.EMPTY, ResourceLocation.withDefaultNamespace("textures/mob_effect/" + effectPath + ".png"));
      }
   }

   private static WheelMenuModel buildModel(InteractionHand hand, ItemStack stack) {
      List<Entry> entries = new ArrayList<>();
      if (stack.is((Item)ModItems.TRANSCENDIUM_HELMET.get())) {
         entries.add(Entry.ofEffect("高亮开", 0, "glowing"));
         entries.add(Entry.ofEffect("高亮关", 1, "invisibility"));
         entries.add(Entry.ofEffect("夜视开", 2, "night_vision"));
         entries.add(Entry.ofEffect("夜视关", 3, "blindness"));
         entries.add(new Entry("全开", 4, markIcon("anvilcraft:check_mark", stack)));
         entries.add(new Entry("全关", 5, markIcon("anvilcraft:cross_mark", stack)));
      } else if (stack.is((Item)ModItems.TRANSCENDIUM_LEGGINGS.get())) {
         entries.add(new Entry("关闭屏蔽", 0, markIcon("anvilcraft:cross_mark", stack)));
         entries.add(new Entry("屏蔽敌对生物", 1, new ItemStack(Items.SHIELD)));
         entries.add(new Entry("屏蔽弹射物", 2, new ItemStack(Items.ARROW)));
         entries.add(new Entry("全部屏蔽", 3, markIcon("anvilcraft:check_mark", stack)));
      } else if (stack.is((Item)ModItems.TRANSCENDIUM_BOOTS.get())) {
         entries.add(new Entry("开（超速飞行）", 2, stack));
         entries.add(new Entry("关（普通飞行）", 1, new ItemStack(Items.BARRIER)));
      } else {
         entries.add(new Entry("开", 1, stack));
         entries.add(new Entry("关", 0, new ItemStack(Items.BARRIER)));
      }

      WheelMenuBuilder builder = WheelMenuBuilder.create()
         .selectionEffect(WheelSelectionEffect.ANNULAR_SECTOR)
         .slotsPerPage(entries.size());

      for (Entry entry : entries) {
         Component label = Component.literal(entry.label());
         // WheelWidget 已将 pose 平移到扇区中心，在 (-8,-8) 绘制 16x16 图标
         builder.action("state_" + entry.state(), label, (graphics, pose, x, y) -> {
            if (entry.texture() != null) {
               graphics.blit(entry.texture(), -8, -8, 16, 16, 0.0F, 0.0F, 18, 18, 18, 18);
            } else {
               graphics.renderItem(entry.icon(), -8, -8);
            }
         }, ctx -> PacketDistributor.sendToServer(new ArmorFunctionSelectPacket(hand, entry.state())));
      }

      return builder.build();
   }

   /** 铁砧工艺的 √ / X 标记物品；意外缺失时回退为盔甲本身。 */
   private static ItemStack markIcon(String id, ItemStack fallback) {
      Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
      return item == Items.AIR ? fallback : new ItemStack(item);
   }
}
