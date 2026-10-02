package com.dingdongji.mod.client;

import com.dingdongji.mod.ModClientConfig;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.client.renderer.RenderType.CompositeState.CompositeStateBuilder;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;

@EventBusSubscriber({Dist.CLIENT})
public final class AfterimageManager {
   private static final int CAPTURE_INTERVAL = 2;
   private static final int LIFETIME = 10;
   private static final double MAX_DISTANCE = 96.0;
   private static final int TINT_R = 170;
   private static final int TINT_G = 80;
   private static final int TINT_B = 255;
   private static List<AfterimageManager.Snapshot> SNAPSHOTS = new ArrayList<>();
   private static boolean renderingAfterimage;
   private static long lastCaptureMs;
   private static ClientLevel trackedLevel;
   private static Field walkSpeedOldField;
   private static Field walkSpeedField;
   private static Field walkPositionField;
   private static boolean walkReflectionFailed;
   private static Field renderTypeStateField;
   private static Field compositeTextureField;
   private static final Function<ResourceLocation, RenderType> AFTERIMAGE_SKIN = Util.memoize(tex -> createAfterimageType("ddj_afterimage_skin", tex, false));
   private static final Function<ResourceLocation, RenderType> AFTERIMAGE_GEAR = Util.memoize(tex -> createAfterimageType("ddj_afterimage_gear", tex, true));

   private AfterimageManager() {
   }

   public static boolean isRenderingAfterimage() {
      return renderingAfterimage;
   }

   @SubscribeEvent
   public static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      ClientLevel level = mc.level;
      if (level != null && !mc.isPaused() && mc.player != null) {
         // Client config gate: when the afterimage is disabled, drop every
         // captured snapshot so the trail disappears on the next frame and no
         // capture/render work is done at all (FPS saver on low-end machines).
         if (!ModClientConfig.flightAfterimageEnabled()) {
            if (!SNAPSHOTS.isEmpty()) {
               SNAPSHOTS.clear();
            }
            return;
         }

         if (level != trackedLevel) {
            SNAPSHOTS.clear();
            trackedLevel = level;
         }

         // Use wall-clock millis: gameTime jumps around during login sync,
         // which used to corrupt snapshot ages right after joining a world.
         long now = Util.getMillis();
         SNAPSHOTS.removeIf(img -> now - img.bornAt > 500L || level.getEntity(img.ownerId) == null);
         if (now - lastCaptureMs >= 100L) {
            lastCaptureMs = now;
            for (Player player : level.players()) {
               // Skip the first second after join/respawn: entity interpolation
               // state is still converging and would record bogus positions.
               if (player instanceof AbstractClientPlayer
                  && !player.isCreative()
                  && !player.isSpectator()
                  && player.isAlive()
                  && player.tickCount >= 20
                  && IonocraftBootsClientHandler.getSyncedMode(player.getId()) == 2
                  && !(player.distanceToSqr(mc.player) > 9216.0)) {
                  Vec3 mv = player.getDeltaMovement();
                  if (!(mv.x * mv.x + mv.z * mv.z < 9.0E-4)) {
                     SNAPSHOTS.add(new AfterimageManager.Snapshot(player, now));
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_PARTICLES) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null && mc.player != null && !SNAPSHOTS.isEmpty()) {
            boolean firstPerson = mc.options.getCameraType() == CameraType.FIRST_PERSON;
            int localId = mc.player.getId();
            long now = Util.getMillis();
            float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            Camera cam = event.getCamera();
            Vec3 camPos = cam.getPosition();
            PoseStack pose = event.getPoseStack();
            BufferSource immediate = mc.renderBuffers().bufferSource();
            List<AfterimageManager.RenderEntry> entries = new ArrayList<>();

            for (AfterimageManager.Snapshot snap : SNAPSHOTS) {
               if (snap.ownerId != localId || !firstPerson) {
                  Entity raw = mc.level.getEntity(snap.ownerId);
                  if (raw instanceof AbstractClientPlayer) {
                     AbstractClientPlayer player = (AbstractClientPlayer)raw;
                     float age = (float)(now - snap.bornAt) / 50.0F;
                     float lifeFactor = Math.max(0.0F, 1.0F - age / 10.0F);
                     if (!(lifeFactor <= 0.0F)) {
                        // Replay along the visual trail: the interpolated
                        // render position minus the distance covered since
                        // capture. The wall-clock age (not gameTime) and the
                        // tickCount capture gate keep login-time ages sane,
                        // while this keeps a freshly spawned afterimage on
                        // the player instead of one tick ahead of it.
                        Vec3 cur = player.getPosition(partialTick);
                        Vec3 off = snap.vel.scale((double)age);
                        double rx = cur.x - off.x;
                        double ry = cur.y - off.y;
                        double rz = cur.z - off.z;
                        double distSqr = (rx - camPos.x) * (rx - camPos.x) + (ry - camPos.y) * (ry - camPos.y) + (rz - camPos.z) * (rz - camPos.z);
                        entries.add(new AfterimageManager.RenderEntry(snap, player, rx, ry, rz, lifeFactor, distSqr));
                     }
                  }
               }
            }

            entries.sort((a, b) -> Double.compare(b.distSqr, a.distSqr));

            for (AfterimageManager.RenderEntry e : entries) {
               float lifeFactor = e.lifeFactor;
               // Skin alpha deliberately lower than gear so armor always draws
               // on top during translucent sorting — fixes "player outside armor"
               // layering inversion users saw after the 0.0.8→0.0.9 update.
               int skinAlpha = (int)(80.0F * lifeFactor);
               int gearAlpha = (int)(150.0F * Math.pow((double)lifeFactor, 1.5));
               AfterimageManager.TintBufferSource tinted = new AfterimageManager.TintBufferSource(
                  immediate, e.player.getSkin().texture(), 170, 80, 255, skinAlpha, gearAlpha
               );
               pose.pushPose();
               pose.translate(e.rx - camPos.x, e.ry - camPos.y, e.rz - camPos.z);
               renderFrozen(mc, e.player, e.snap, pose, tinted);
               pose.popPose();
               immediate.endBatch();
            }
         }
      }
   }

   private static void renderFrozen(Minecraft mc, Player player, AfterimageManager.Snapshot snap, PoseStack pose, MultiBufferSource buffers) {
      int savedTickCount = player.tickCount;
      float yRot = player.getYRot();
      float xRot = player.getXRot();
      float yRotO = player.yRotO;
      float xRotO = player.xRotO;
      float yHeadRot = player.yHeadRot;
      float yHeadRotO = player.yHeadRotO;
      float yBodyRot = player.yBodyRot;
      float yBodyRotO = player.yBodyRotO;
      float attackAnim = player.attackAnim;
      float oAttackAnim = player.oAttackAnim;
      int hurtTime = player.hurtTime;
      int deathTime = player.deathTime;
      Pose poseEnum = player.getPose();
      Object[] savedWalk = freezeWalkAnimation(player, snap.limbPos, snap.limbSpeed);
      Entity savedPicked = mc.getEntityRenderDispatcher().crosshairPickEntity;

      try {
         player.tickCount = snap.tickCount;
         player.setYRot(snap.yRot);
         player.yRotO = snap.yRot;
         player.setXRot(snap.xRot);
         player.xRotO = snap.xRot;
         player.yHeadRot = snap.yHeadRot;
         player.yHeadRotO = snap.yHeadRot;
         player.yBodyRot = snap.yBodyRot;
         player.yBodyRotO = snap.yBodyRot;
         player.attackAnim = snap.attackAnim;
         player.oAttackAnim = snap.attackAnim;
         player.hurtTime = 0;
         player.deathTime = 0;
         if (poseEnum != Pose.STANDING) {
            player.setPose(Pose.STANDING);
         }

         mc.getEntityRenderDispatcher().crosshairPickEntity = null;
         EntityRenderer renderer = mc.getEntityRenderDispatcher().getRenderer(player);
         HumanoidModel<?> skinModel = null;
         boolean[] savedPartVisibility = null;
         int fullBright = 15728880;
         renderingAfterimage = true;

         try {
            // 护甲覆盖处只保留护甲虚影、不渲染皮肤模型，避免皮肤透过护甲
            // 缝隙外露形成错误层次；护甲层使用各自独立模型，不受此可见性影响。
            if (renderer instanceof LivingEntityRenderer<?, ?> livingRenderer
               && livingRenderer.getModel() instanceof HumanoidModel<?> humanoidModel) {
               skinModel = humanoidModel;
               savedPartVisibility = hideSkinUnderArmor(skinModel, snap);
            }
            renderer.render(player, snap.yRot, 0.0F, pose, buffers, fullBright);
         } finally {
            renderingAfterimage = false;
            if (skinModel != null) {
               restoreSkinVisibility(skinModel, savedPartVisibility);
            }
         }
      } finally {
         player.tickCount = savedTickCount;
         player.setYRot(yRot);
         player.yRotO = yRotO;
         player.setXRot(xRot);
         player.xRotO = xRotO;
         player.yHeadRot = yHeadRot;
         player.yHeadRotO = yHeadRotO;
         player.yBodyRot = yBodyRot;
         player.yBodyRotO = yBodyRotO;
         player.attackAnim = attackAnim;
         player.oAttackAnim = oAttackAnim;
         player.hurtTime = hurtTime;
         player.deathTime = deathTime;
         player.setPose(poseEnum);
         mc.getEntityRenderDispatcher().crosshairPickEntity = savedPicked;
         restoreWalkAnimation(player, savedWalk);
      }
   }

   /**
    * 按快照时的护甲穿戴隐藏皮肤模型被覆盖的部件，返回各部件原可见性。
    * 覆盖关系与原版 {@code HumanoidArmorLayer#setPartVisibility} 一致：
    * 头盔覆盖头(含帽子层)；胸甲覆盖躯干+双臂；护腿覆盖躯干+双腿；靴子覆盖双腿。
    */
   private static boolean[] hideSkinUnderArmor(HumanoidModel<?> model, Snapshot snap) {
      ModelPart[] parts = new ModelPart[]{
         model.head, model.hat, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg
      };
      boolean[] saved = new boolean[parts.length];
      for (int i = 0; i < parts.length; i++) {
         saved[i] = parts[i].visible;
      }
      if (snap.helmet) {
         model.head.visible = false;
         model.hat.visible = false;
      }
      if (snap.chestplate) {
         model.body.visible = false;
         model.rightArm.visible = false;
         model.leftArm.visible = false;
      }
      if (snap.leggings) {
         model.body.visible = false;
         model.rightLeg.visible = false;
         model.leftLeg.visible = false;
      }
      if (snap.boots) {
         model.rightLeg.visible = false;
         model.leftLeg.visible = false;
      }
      return saved;
   }

   private static void restoreSkinVisibility(HumanoidModel<?> model, boolean[] saved) {
      if (saved == null) {
         return;
      }
      ModelPart[] parts = new ModelPart[]{
         model.head, model.hat, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg
      };
      for (int i = 0; i < parts.length; i++) {
         parts[i].visible = saved[i];
      }
   }

   private static void initWalkReflection() {
      if (!walkReflectionFailed) {
         try {
            Class<?> cls = Class.forName("net.minecraft.world.entity.WalkAnimationState");
            walkSpeedOldField = cls.getDeclaredField("speedOld");
            walkSpeedField = cls.getDeclaredField("speed");
            walkPositionField = cls.getDeclaredField("position");
            walkSpeedOldField.setAccessible(true);
            walkSpeedField.setAccessible(true);
            walkPositionField.setAccessible(true);
         } catch (Exception var1) {
            walkReflectionFailed = true;
         }
      }
   }

   private static Object[] freezeWalkAnimation(Player player, float limbPos, float limbSpeed) {
      initWalkReflection();
      if (walkReflectionFailed) {
         return null;
      } else {
         try {
            Object state = player.walkAnimation;
            Object[] old = new Object[]{walkSpeedOldField.get(state), walkSpeedField.get(state), walkPositionField.get(state)};
            walkSpeedOldField.setFloat(state, limbSpeed);
            walkSpeedField.setFloat(state, limbSpeed);
            walkPositionField.setFloat(state, limbPos);
            return old;
         } catch (Exception var5) {
            walkReflectionFailed = true;
            return null;
         }
      }
   }

   private static void restoreWalkAnimation(Player player, Object[] saved) {
      if (saved != null && !walkReflectionFailed) {
         try {
            Object state = player.walkAnimation;
            walkSpeedOldField.setFloat(state, (Float)saved[0]);
            walkSpeedField.setFloat(state, (Float)saved[1]);
            walkPositionField.setFloat(state, (Float)saved[2]);
         } catch (Exception var3) {
         }
      }
   }

   private static ResourceLocation textureOf(RenderType type) {
      try {
         if (renderTypeStateField == null) {
            Class<?> crt = Class.forName("net.minecraft.client.renderer.RenderType$CompositeRenderType");
            renderTypeStateField = crt.getDeclaredField("state");
            renderTypeStateField.setAccessible(true);
            Class<?> cs = Class.forName("net.minecraft.client.renderer.RenderType$CompositeState");
            compositeTextureField = cs.getDeclaredField("textureState");
            compositeTextureField.setAccessible(true);
         }

         if (!renderTypeStateField.getDeclaringClass().isInstance(type)) {
            return null;
         } else {
            Object state = renderTypeStateField.get(type);
            Object texShard = compositeTextureField.get(state);
            Method m = texShard.getClass().getDeclaredMethod("cutoutTexture");
            m.setAccessible(true);
            Optional<?> opt = (Optional<?>)m.invoke(texShard);
            return (ResourceLocation)opt.orElse(null);
         }
      } catch (Throwable var5) {
         return null;
      }
   }

   private static RenderType createAfterimageType(String name, ResourceLocation tex, boolean polygonOffset) {
      CompositeStateBuilder b = CompositeState.builder()
         .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
         .setTextureState(new TextureStateShard(tex, false, false))
         .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
         // Default CULL_BACK: opaque renderers enable it to skip rear faces;
         // translucent ones commonly disable it (NO_CULL) so you see back
         // faces through front. That's exactly what causes the "player
         // inside armor" overlap. For the afterimage we deliberately leave
         // culling enabled — only front faces of both skin and armor draw,
         // so the armor's front half never has the skin's front half bleed
         // through on the same side. This is the same trick
         // MixinHumanoidArmorLayerRenderType uses for the spectral suit,
         // applied here to the full player+armor pipeline.
         .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
         .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE);
      if (polygonOffset) {
         b.setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING);
      }

      return RenderType.create(name, DefaultVertexFormat.NEW_ENTITY, Mode.QUADS, 1536, false, true, b.createCompositeState(false));
   }

   private static final class RenderEntry {
      final AfterimageManager.Snapshot snap;
      final AbstractClientPlayer player;
      final double rx;
      final double ry;
      final double rz;
      final float lifeFactor;
      final double distSqr;

      RenderEntry(AfterimageManager.Snapshot snap, AbstractClientPlayer player, double rx, double ry, double rz, float lifeFactor, double distSqr) {
         this.snap = snap;
         this.player = player;
         this.rx = rx;
         this.ry = ry;
         this.rz = rz;
         this.lifeFactor = lifeFactor;
         this.distSqr = distSqr;
      }
   }

   public static final class Snapshot {
      final double x;
      final double y;
      final double z;
      final int ownerId;
      final long bornAt;
      final int tickCount;
      final float yRot;
      final float xRot;
      final float yHeadRot;
      final float yBodyRot;
      final float attackAnim;
      final float limbPos;
      final float limbSpeed;
      final Pose pose;
      final Vec3 vel;
      final boolean helmet;
      final boolean chestplate;
      final boolean leggings;
      final boolean boots;

      Snapshot(Player p, long bornAt) {
         Vec3 pos = p.getPosition(1.0F);
         this.x = pos.x;
         this.y = pos.y;
         this.z = pos.z;
         this.ownerId = p.getId();
         this.bornAt = bornAt;
         this.vel = p.getDeltaMovement();
         this.tickCount = p.tickCount;
         this.yRot = p.getYRot();
         this.xRot = p.getXRot();
         this.yHeadRot = p.yHeadRot;
         this.yBodyRot = p.yBodyRot;
         this.attackAnim = p.attackAnim;
         this.limbPos = p.walkAnimation.position(1.0F);
         this.limbSpeed = p.walkAnimation.speed(1.0F);
         this.pose = p.getPose();
         this.helmet = p.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof ArmorItem;
         this.chestplate = p.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ArmorItem;
         this.leggings = p.getItemBySlot(EquipmentSlot.LEGS).getItem() instanceof ArmorItem;
         this.boots = p.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof ArmorItem;
      }
   }

   private static final class TintBufferSource implements MultiBufferSource {
      private final MultiBufferSource delegate;
      private final ResourceLocation skinTexture;
      private final int r;
      private final int g;
      private final int b;
      private final int skinAlpha;
      private final int gearAlpha;

      TintBufferSource(MultiBufferSource delegate, ResourceLocation skinTexture, int r, int g, int b, int skinAlpha, int gearAlpha) {
         this.delegate = delegate;
         this.skinTexture = skinTexture;
         this.r = r;
         this.g = g;
         this.b = b;
         this.skinAlpha = skinAlpha;
         this.gearAlpha = gearAlpha;
      }

      public VertexConsumer getBuffer(RenderType type) {
         // Only swap NEW_ENTITY types: glint types are POSITION_TEX and held
         // block items use the BLOCK format. Rerouting those into a
         // NEW_ENTITY buffer leaves required vertex elements unwritten and
         // corrupts the batch (black shard geometry / build failure).
         ResourceLocation tex = type.format() == DefaultVertexFormat.NEW_ENTITY
            ? AfterimageManager.textureOf(type)
            : null;
         RenderType out;
         int alpha;
         if (tex != null) {
            boolean skin = tex.equals(this.skinTexture);
            out = skin ? AfterimageManager.AFTERIMAGE_SKIN.apply(tex) : AfterimageManager.AFTERIMAGE_GEAR.apply(tex);
            alpha = skin ? this.skinAlpha : this.gearAlpha;
         } else {
            out = type;
            alpha = this.skinAlpha;
         }

         return new AfterimageManager.TintVertexConsumer(this.delegate.getBuffer(out), this.r, this.g, this.b, alpha);
      }
   }

   private static final class TintVertexConsumer implements VertexConsumer {
      private final VertexConsumer delegate;
      private final int r;
      private final int g;
      private final int b;
      private final int a;

      TintVertexConsumer(VertexConsumer delegate, int r, int g, int b, int a) {
         this.delegate = delegate;
         this.r = r;
         this.g = g;
         this.b = b;
         this.a = a;
      }

      public VertexConsumer addVertex(float x, float y, float z) {
         this.delegate.addVertex(x, y, z);
         return this;
      }

      public VertexConsumer setColor(int red, int green, int blue, int alpha) {
         this.delegate.setColor(red * this.r / 255, green * this.g / 255, blue * this.b / 255, alpha * this.a / 255);
         return this;
      }

      public VertexConsumer setUv(float u, float v) {
         this.delegate.setUv(u, v);
         return this;
      }

      public VertexConsumer setUv1(int u, int v) {
         this.delegate.setUv1(u, v);
         return this;
      }

      public VertexConsumer setUv2(int u, int v) {
         this.delegate.setUv2(u, v);
         return this;
      }

      public VertexConsumer setNormal(float x, float y, float z) {
         this.delegate.setNormal(x, y, z);
         return this;
      }
   }
}
