package com.dingdongji.mod.client;

import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.component.PouchCapacityComponent;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.ItemDecoratorHandler;
import org.slf4j.Logger;

/**
 * 带口袋组件的护腿：在物品图标右下角绘制口袋角标。
 * <ul>
 *   <li>容量 ≥12 → 大口袋贴图</li>
 *   <li>其他（6）→ 小口袋贴图</li>
 * </ul>
 * 物品格为 16×16，角标区域为右下角 8×8，用 pose 缩放 0.5 绘制原 16×16 贴图。
 * <h3>角标自适应让位</h3>
 * 若同一物品上还注册了<b>其他模组</b>的 {@link IItemDecorator}（NeoForge
 * 物品装饰器 API），角标自动不画，避免挤在一起。实现：
 * {@link ItemDecoratorHandler#of(ItemStack)} 拿到该物品的装饰器处理器，
 * 反射读其私有 {@code itemDecorators} 列表，存在非本类实例即让位。
 * <p>
 * 边界（已实证 NeoForge 21.1.248 内部结构）：
 * <ul>
 *   <li>只能检测走官方装饰器 API 的角标；用 Mixin / 渲染事件硬画的模组
 *       检测不到——渲染前无法获知，逐帧读帧缓冲（glReadPixels）性能与
 *       兼容性都不可接受，不采用；</li>
 *   <li>对方注册了但这一帧实际不画，我们也会让位（无法无副作用地探测
 *       其 render 结果），策略上"宁让不抢"；</li>
 *   <li>反射失败时回退为照常绘制（fail-open）。</li>
 * </ul>
 */
public class PouchDecoration implements IItemDecorator {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final PouchDecoration INSTANCE = new PouchDecoration();

   private static final ResourceLocation SMALL_TEXTURE =
      ResourceLocation.fromNamespaceAndPath("dingdongji", "textures/item/small_pouch_badge.png");
   private static final ResourceLocation BIG_TEXTURE =
      ResourceLocation.fromNamespaceAndPath("dingdongji", "textures/item/big_pouch_badge.png");

   private static Field decoratorListField;
   private static final Set<String> yieldLogged = new HashSet<>();

   private PouchDecoration() {
   }

   @Override
   public boolean render(GuiGraphics guiGraphics, Font font, ItemStack stack, int xOffset, int yOffset) {
      PouchCapacityComponent comp = stack.get(ModComponents.POUCH_CAPACITY.get());
      if (comp == null || comp.capacity() <= 0) {
         return false;
      }
      if (ddj$shouldYield(stack)) {
         return false;
      }
      ResourceLocation texture = comp.capacity() >= 12 ? BIG_TEXTURE : SMALL_TEXTURE;
      var pose = guiGraphics.pose();
      pose.pushPose();
      // 实测 GuiGraphics 层级：物品模型 z=150，物品装饰（数量/耐久）z=200，
      // tooltip z=400。角标取 200：盖在物品之上、被 tooltip 盖住。
      // 角标经 0.5 缩小后整体内缩，translate 取 +9（原 +10 向右下溢出 2px），
      // 角标整体向左上收 1px，更稳地落在物品格内右下角。
      pose.translate(xOffset + 9.0F, yOffset + 9.0F, 200.0F);
      pose.scale(0.5F, 0.5F, 1.0F);
      // 16×16 源纹理，经 0.5 缩放后为 8×8，落点向右下偏移 2px
      guiGraphics.blit(texture, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
      pose.popPose();
      return false;
   }

   /**
    * 同一物品上是否存在其他模组的物品装饰器。注册期结束后列表为不可变
    * 结构，渲染期只读安全。反射失败 fail-open（返回 false 照常绘制）。
    */
   private boolean ddj$shouldYield(ItemStack stack) {
      try {
         if (decoratorListField == null) {
            decoratorListField = ItemDecoratorHandler.class.getDeclaredField("itemDecorators");
            decoratorListField.setAccessible(true);
         }
         ItemDecoratorHandler handler = ItemDecoratorHandler.of(stack);
         @SuppressWarnings("unchecked")
         List<IItemDecorator> all =
            (List<IItemDecorator>) decoratorListField.get(handler);
         for (IItemDecorator other : all) {
            if (other != INSTANCE) {
               String key = stack.getItem() + " -> " + other.getClass().getName();
               if (yieldLogged.add(key)) {
                  LOGGER.info("[DingDongJi][角标] 检测到同物品其他装饰器，角标自动让位：{}", key);
               }
               return true;
            }
         }
      } catch (ReflectiveOperationException e) {
         LOGGER.warn("[DingDongJi][角标] 枚举装饰器失败，按默认绘制", e);
      }
      return false;
   }
}
