package com.dingdongji.mod.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({HumanoidArmorLayer.class})
public interface AccessorHumanoidArmorLayer {
   @Accessor("innerModel")
   HumanoidModel<?> ddj$getInnerModel();

   @Accessor("outerModel")
   HumanoidModel<?> ddj$getOuterModel();
}
