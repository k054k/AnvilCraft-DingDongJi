package com.dingdongji.mod.client.render;

import com.dingdongji.mod.client.AfterimageManager;
import com.dingdongji.mod.entity.PhantomDecoyEntity;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;

/**
 * 幻灵替身渲染：直接借用主人玩家自己的 {@link EntityRenderer}（PlayerRenderer）
 * 在替身位置再画一次。这样替身与玩家"一模一样"由原版渲染器天然保证——
 * 皮肤（含 slim 模型/帽层）、护甲、手持物、鞘翅、披风、肩上鹦鹉、三叉戟
 * 激流层，以及 curios 等所有模组通过 AddLayers 挂到玩家渲染器上的饰品层，
 * 全部原样呈现，无需在此逐个复刻。
 *
 * 直接调 renderer.render 会再次触发 RenderPlayerEvent.Pre：用
 * AfterimageManager 的渲染中标志让 PhantomPlayerRenderHandler 放行，
 * 否则处于幻影状态的主人会把替身也染成半透明幻影色。
 *
 * 主人不在线（换维度/退出）时回退到本地 HumanoidMob 模型渲染。
 */
public class PhantomDecoyRenderer extends HumanoidMobRenderer<PhantomDecoyEntity, PlayerModel<PhantomDecoyEntity>> {
   private final PlayerModel<PhantomDecoyEntity> slimModel;

   public PhantomDecoyRenderer(EntityRendererProvider.Context context) {
      super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
      this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
      this.addLayer(
         new HumanoidArmorLayer<>(
            this,
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
            context.getModelManager()
         )
      );
      this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
      this.addLayer(new ElytraLayer<>(this, context.getModelSet()));
   }

   @Override
   public void render(
      PhantomDecoyEntity entity,
      float entityYaw,
      float partialTicks,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight
   ) {
      AbstractClientPlayer owner = resolveOwner(entity.getOwner());
      if (owner == null) {
         this.model = resolveSlim(entity.getOwner()) ? this.slimModel : this.getModel();
         super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
         return;
      }

      Minecraft mc = Minecraft.getInstance();
      EntityRenderer<? super AbstractClientPlayer> playerRenderer =
         mc.getEntityRenderDispatcher().getRenderer(owner);

      // 替身的受击红闪由自己的 hurtTime 驱动（创造模式下主人不受伤），
      // 渲染期间临时同步给主人，结束后恢复，避免污染主人本体的渲染状态。
      int savedHurtTime = owner.hurtTime;
      int savedHurtDuration = owner.hurtDuration;
      var savedPicked = mc.getEntityRenderDispatcher().crosshairPickEntity;
      // 冻结主人的实时动画状态：直接渲染活体主人会把他当前的走路摆臂、
      // 转头、身体滞后旋转全部投射到替身上（替身乱晃的根因）。渲染期间
      // 把主人的旋转/攻击/走路动画快照成替身自己的静止状态，结束后恢复。
      float savedYRot = owner.getYRot();
      float savedXRot = owner.getXRot();
      float savedYRotO = owner.yRotO;
      float savedXRotO = owner.xRotO;
      float savedYHeadRot = owner.yHeadRot;
      float savedYHeadRotO = owner.yHeadRotO;
      float savedYBodyRot = owner.yBodyRot;
      float savedYBodyRotO = owner.yBodyRotO;
      // 右臂挥动跟随本体：替身借主人渲染器绘制，保留主人的 attackAnim
      // 即可让替身挥臂与本体同步（旧版将其清零，替身手臂僵直）
      float savedAttackAnim = owner.attackAnim;
      float savedOAttackAnim = owner.oAttackAnim;
      // 披风：借主人渲染器绘制时披风层会继承本体的移动/摆动，字段级冻结
      // （xCloak/xo/bob）只能盖住部分来源——改为在渲染层直接隐藏披风：
      // CloakLayer 检查模型 cloak.visible，窗口内置 false 即完全不绘制
      net.minecraft.client.renderer.entity.player.PlayerRenderer playerRendererCast =
         (net.minecraft.client.renderer.entity.player.PlayerRenderer) playerRenderer;
      var playerModel = (com.dingdongji.mod.mixin.PlayerModelAccessor) playerRendererCast.getModel();
      boolean savedCloakVisible = playerModel.ddj$getCloak().visible;
      playerModel.ddj$getCloak().visible = false;
      net.minecraft.world.entity.Pose savedPose = owner.getPose();
      Object[] savedWalk = AfterimageManager.freezeWalkAnimation(owner, 0.0F, 0.0F);
      owner.hurtTime = entity.hurtTime;
      owner.hurtDuration = entity.hurtDuration;
      owner.setYRot(entity.getYRot());
      owner.yRotO = entity.yRotO;
      owner.setXRot(entity.getXRot());
      owner.xRotO = entity.xRotO;
      owner.yHeadRot = entity.yHeadRot;
      owner.yHeadRotO = entity.yHeadRotO;
      owner.yBodyRot = entity.yBodyRot;
      owner.yBodyRotO = entity.yBodyRotO;
      if (savedPose != net.minecraft.world.entity.Pose.STANDING) {
         owner.setPose(net.minecraft.world.entity.Pose.STANDING);
      }
      mc.getEntityRenderDispatcher().crosshairPickEntity = null;
      AfterimageManager.setRenderingAfterimage(true);
      try {
         playerRenderer.render(owner, entityYaw, partialTicks, poseStack, buffer, packedLight);
      } finally {
         AfterimageManager.setRenderingAfterimage(false);
         mc.getEntityRenderDispatcher().crosshairPickEntity = savedPicked;
         owner.hurtTime = savedHurtTime;
         owner.hurtDuration = savedHurtDuration;
         owner.setYRot(savedYRot);
         owner.yRotO = savedYRotO;
         owner.setXRot(savedXRot);
         owner.xRotO = savedXRotO;
         owner.yHeadRot = savedYHeadRot;
         owner.yHeadRotO = savedYHeadRotO;
         owner.yBodyRot = savedYBodyRot;
         owner.yBodyRotO = savedYBodyRotO;
         playerModel.ddj$getCloak().visible = savedCloakVisible;
         owner.setPose(savedPose);
         AfterimageManager.restoreWalkAnimation(owner, savedWalk);
      }
   }

   /** 与本体 PlayerRenderer 一致的 0.9375 缩放（仅回退路径使用）。 */
   @Override
   protected void scale(PhantomDecoyEntity entity, PoseStack poseStack, float partialTickTime) {
      poseStack.scale(0.9375F, 0.9375F, 0.9375F);
   }

   private static AbstractClientPlayer resolveOwner(UUID owner) {
      if (owner == null) {
         return null;
      }
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         return null;
      }
      for (AbstractClientPlayer player : mc.level.players()) {
         if (player.getUUID().equals(owner)) {
            return player;
         }
      }
      return null;
   }

   private static boolean resolveSlim(UUID owner) {
      return resolveSkin(owner).model() == PlayerSkin.Model.SLIM;
   }

   private static PlayerSkin resolveSkin(UUID owner) {
      if (owner != null) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.getConnection() != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(owner);
            if (info != null) {
               return info.getSkin();
            }
         }
         return DefaultPlayerSkin.get(owner);
      }
      return DefaultPlayerSkin.get(net.minecraft.Util.NIL_UUID);
   }

   @Override
   public ResourceLocation getTextureLocation(PhantomDecoyEntity entity) {
      return resolveSkin(entity.getOwner()).texture();
   }
}
