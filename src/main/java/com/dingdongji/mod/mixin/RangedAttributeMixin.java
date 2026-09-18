package com.dingdongji.mod.mixin;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({RangedAttribute.class})
public class RangedAttributeMixin {
   @ModifyVariable(
      method = {"<init>(Ljava/lang/String;DDD)V"},
      at = @At("HEAD"),
      argsOnly = true,
      index = 3
   )
   private static double removeMaxCap(double max, String descriptionId, double defaultValue, double min) {
      return !"attribute.name.generic.armor".equals(descriptionId) && !"attribute.name.generic.armor_toughness".equals(descriptionId) ? max : Double.MAX_VALUE;
   }
}
