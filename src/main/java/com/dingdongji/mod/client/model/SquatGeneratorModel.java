package com.dingdongji.mod.client.model;

import com.dingdongji.mod.KryptonMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.world.entity.LivingEntity;

/**
 * 蹲起式发电器穿戴模型（桌面 squat_generator.bbmodel 移植，64x64）：
 * 腰部履带盒 + 双腿内外两层发电机外壳。护腿槽渲染时 HumanoidArmorLayer
 * 只显示 body/两条腿，head/hat/双臂留空占位（HumanoidModel 构造必须）。
 */
public class SquatGeneratorModel extends HumanoidModel<LivingEntity> {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(KryptonMod.modLoc("squat_generator"), "main");

   public SquatGeneratorModel(ModelPart root) {
      super(root);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      var root = mesh.getRoot();

      // 空占位部件（HumanoidModel 要求六个标准部件齐全）
      root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
      root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
      root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
      root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));

      // Belt：身体下段（腰）8x5x4，inflate 0.51
      root.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(16, 5)
            .addBox(-4.0F, 7.0F, -2.0F, 8.0F, 5.0F, 4.0F, new CubeDeformation(0.51F)),
         PartPose.ZERO);

      // 右腿：内层 inflate 0.5 + 外层 inflate 0.75（uv 40,0）
      root.addOrReplaceChild("right_leg", CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.5F))
            .texOffs(40, 0)
            .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.75F)),
         PartPose.offset(-1.9F, 12.0F, 0.0F));

      // 左腿：同上，镜像 UV
      root.addOrReplaceChild("left_leg", CubeListBuilder.create().mirror()
            .texOffs(0, 0)
            .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.5F))
            .texOffs(40, 0)
            .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.75F)),
         PartPose.offset(1.9F, 12.0F, 0.0F));

      return LayerDefinition.create(mesh, 64, 64);
   }
}
