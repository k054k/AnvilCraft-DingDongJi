package com.dingdongji.mod.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;

/**
 * 口袋物品（拓展口袋 / 拓展深口袋）。
 * <p>
 * tooltip 沿用铁砧耐候套（weatherproof_spacesuit_leggings）口袋功能
 * 三条描述中的前两条；"与护腿合成后，为护腿添加 N 格口袋"与
 * "将带了口袋的护腿单独合成可将口袋与护腿分离"由
 * {@link com.dingdongji.mod.event.ModEvents} 的 tooltip 事件连续插入
 * 在物品名正下方（分离说明紧跟容量说明）：
 * <ol>
 *   <li>与护腿合成后，为护腿添加 N 格口袋；</li>
 *   <li>将带了口袋的护腿单独合成可将口袋与护腿分离；</li>
 *   <li>使用口袋快捷键与副手交换物品；</li>
 *   <li>清空口袋后才能卸下护腿。</li>
 * </ol>
 * 文字灰色（ChatFormatting.GRAY），与铁砧 tooltip 描述风格一致。
 */
public class PouchItem extends Item {
   public PouchItem(Properties properties) {
      super(properties);
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("tooltip.dingdongji.pouch.swap").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("tooltip.dingdongji.pouch.clear").withStyle(ChatFormatting.GRAY));
   }
}
