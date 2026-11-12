package com.dingdongji.mod.client.accessor;

import net.minecraft.client.model.HumanoidModel;

/**
 * 鸭子接口：暴露 HumanoidArmorLayer 的内/外层护甲模型。
 * 实现由 {@code HumanoidArmorLayerRenderTypeMixin} 提供（客户端 mixin）。
 */
public interface HumanoidArmorLayerAccess {
   HumanoidModel<?> ddj$getInnerModel();

   HumanoidModel<?> ddj$getOuterModel();
}
