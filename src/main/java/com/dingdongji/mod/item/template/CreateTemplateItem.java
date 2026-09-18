package com.dingdongji.mod.item.template;

import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.component.CreateTemplateMode;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;

public class CreateTemplateItem extends Item {
   public CreateTemplateItem(Properties properties) {
      super(properties);
   }

   public Component getName(ItemStack stack) {
      CreateTemplateMode mode = (CreateTemplateMode)stack.get((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get());
      if (mode == null) {
         mode = CreateTemplateMode.DEFAULT;
      }

      return Component.literal(mode.getDisplayName());
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (level.isClientSide) {
         return InteractionResultHolder.success(stack);
      } else {
         CreateTemplateMode current = (CreateTemplateMode)stack.get((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get());
         if (current == null) {
            current = CreateTemplateMode.DEFAULT;
         }

         CreateTemplateMode next = current.next();
         stack.set((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get(), next);
         return InteractionResultHolder.success(stack);
      }
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      CreateTemplateMode mode = (CreateTemplateMode)stack.get((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get());
      if (mode == null) {
         mode = CreateTemplateMode.DEFAULT;
      }

      tooltip.add(
         Component.translatable("tooltip.dingdongji.create_template.desc")
            .withStyle(style -> style.withColor(TextColor.fromRgb(5900287)))
            .append(Component.literal(mode.getDescription()).withStyle(style -> style.withColor(TextColor.fromRgb(12353791))))
      );
      tooltip.add(Component.translatable("tooltip.dingdongji.create_template.switch").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("screen.dingdongji.create_template").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.empty());
      tooltip.add(Component.translatable("tooltip.dingdongji.create_template.applies").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("screen.dingdongji.create_template.applies_to").withStyle(ChatFormatting.BLUE));
      tooltip.add(Component.empty());
      tooltip.add(Component.translatable("tooltip.dingdongji.create_template.material").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("screen.dingdongji.create_template.upgrade_ingredients").withStyle(ChatFormatting.BLUE));
   }
}
