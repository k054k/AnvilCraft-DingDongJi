package com.dingdongji.mod.mixin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.advancements.AdvancementNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 固定叮咚叽进度树的兄弟节点上下顺序。
 *
 * <p>原版 {@code AdvancementNode#children()} 是 {@code ReferenceOpenHashSet}，
 * 遍历顺序由 identity hash 决定，每次开局可能不同，导致"叽线上 / 盔甲线居中 /
 * 铭化信标在下"以及"余烬在上、浮霜在下"的分叉布局随机翻转。
 *
 * <p>这里仅在同一组兄弟中存在 dingdongji 节点时，按下表显式序号重排，
 * 未列出的节点按 id 字母序兜底；其他模组/原版进度完全不受影响。
 * （位置由服务端 {@code TreeNodePosition} 的 Walker 算法按此顺序计算，
 * 首者在上、末者在下。）
 */
@Mixin(AdvancementNode.class)
public abstract class AdvancementNodeOrderMixin {
   /** 同一兄弟组内的期望上下顺序（小在上，大在下）。 */
   private static final Map<String, Integer> DDJ_SIBLING_ORDER = Map.of(
      // 根节点下三个分支：叽线在上，盔甲主线居中，铭化信标在下
      "ji_ingot", 0,
      "durable_goods", 1,
      "anchored_beacon", 2,
      // 皇家钢分叉：余烬金属在上，浮霜金属在下
      "ember_armor", 0,
      "frost_armor", 1
   );

   @Inject(method = "children", at = @At("RETURN"), cancellable = true)
   private void ddj$stableSiblingOrder(CallbackInfoReturnable<Iterable<AdvancementNode>> cir) {
      Iterable<AdvancementNode> original = cir.getReturnValue();
      List<AdvancementNode> siblings = new ArrayList<>();
      original.forEach(siblings::add);
      if (siblings.size() <= 1) {
         return;
      }
      boolean hasDingdongji = false;
      for (AdvancementNode node : siblings) {
         if ("dingdongji".equals(node.holder().id().getNamespace())) {
            hasDingdongji = true;
            break;
         }
      }
      if (!hasDingdongji) {
         return;
      }
      siblings.sort(
         Comparator.comparingInt((AdvancementNode node) ->
               DDJ_SIBLING_ORDER.getOrDefault(node.holder().id().getPath(), 100))
            .thenComparing(node -> node.holder().id().getPath())
      );
      cir.setReturnValue(siblings);
   }
}
