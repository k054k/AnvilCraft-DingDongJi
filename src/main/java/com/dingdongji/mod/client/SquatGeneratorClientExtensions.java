package com.dingdongji.mod.client;

import com.dingdongji.mod.client.model.SquatGeneratorModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * 蹲起式发电器客户端扩展：只负责自定义穿戴模型；贴图路径由物品子类 SquatGeneratorLeggingsItem.getArmorTexture 提供。
 */
public class SquatGeneratorClientExtensions implements IClientItemExtensions {
   private SquatGeneratorModel model;

   @Override
   public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
      if (model == null) {
         EntityModelSet models = Minecraft.getInstance().getEntityModels();
         model = new SquatGeneratorModel(models.bakeLayer(SquatGeneratorModel.LAYER));
      }
      return model;
   }
}
