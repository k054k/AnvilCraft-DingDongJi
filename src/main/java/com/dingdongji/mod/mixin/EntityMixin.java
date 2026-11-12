package com.dingdongji.mod.mixin;

import com.dingdongji.mod.event.ModArmorSetHandler;
import com.dingdongji.mod.event.PhantomDecoyManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 0.1.0 合并类：所有注入原版 Entity 的逻辑集中于此。
 * 含：余烬/超限全套点燃免疫、浮霜/超限全套防冻结、方块减速豁免、
 * 幻灵套移动垂直碰撞、幻灵/超限防窒息、超限相位实体碰撞豁免。
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

   @Shadow
   protected abstract Vec3 maybeBackOffFromEdge(Vec3 movement, MoverType type);

   // ============ 点燃免疫（余烬全套 / 超限全套） ============
   @Inject(method = "setRemainingFireTicks", at = @At("HEAD"), cancellable = true)
   private void ddj$blockFireTicks(int fireTicks, CallbackInfo ci) {
      if (fireTicks > 0 && (Object) this instanceof Player player
            && (ModArmorSetHandler.hasFullEmberSet(player) || ModArmorSetHandler.hasFullTranscendiumSet(player))) {
         ci.cancel();
      }
   }

   // ============ 防细雪冻结（浮霜全套 / 超限全套） ============
   @Inject(method = "canFreeze", at = @At("HEAD"), cancellable = true)
   private void ddj$noFreeze(CallbackInfoReturnable<Boolean> cir) {
      if ((Object) this instanceof Player player
            && (ModArmorSetHandler.hasFullFrostSet(player) || ModArmorSetHandler.hasFullTranscendiumSet(player))) {
         cir.setReturnValue(false);
      }
   }

   // ============ 过冷浮霜金属块的冰冻累积 ============
   // 见 LivingEntityMixin#ddj$syncSupercooledFreezeState：必须注入在
   // LivingEntity.aiStep 的 freezing 块（服务端玩家静止时不执行 travel/move，
   // 旧的 tryCheckInsideBlocks 注入点对静止玩家永不触发）。

   // ============ 方块减速豁免（凌霜靴/叽套等，判定沿用原 helper） ============
   @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
   private void ddj$cancelStuckInBlock(BlockState state, Vec3 motionMultiplier, CallbackInfo ci) {
      if (ModArmorSetHandler.ignoresBlockSlowdown((Entity) (Object) this)) {
         ci.cancel();
      }
   }

   @Inject(method = "getBlockSpeedFactor", at = @At("HEAD"), cancellable = true)
   private void ddj$cancelBlockSpeedFactor(CallbackInfoReturnable<Float> cir) {
      if (ModArmorSetHandler.ignoresBlockSlowdown((Entity) (Object) this)) {
         cir.setReturnValue(1.0F);
      }
   }

   @Inject(method = "getBlockJumpFactor", at = @At("HEAD"), cancellable = true)
   private void ddj$cancelBlockJumpFactor(CallbackInfoReturnable<Float> cir) {
      if (ModArmorSetHandler.ignoresBlockSlowdown((Entity) (Object) this)) {
         cir.setReturnValue(1.0F);
      }
   }

   @Inject(method = "isSuppressingBounce", at = @At("HEAD"), cancellable = true)
   private void ddj$conditionalBounceSuppress(CallbackInfoReturnable<Boolean> cir) {
      Entity self = (Entity) (Object) this;
      if (ModArmorSetHandler.ignoresBlockSlowdown(self) && self.getDeltaMovement().y >= -0.25) {
         cir.setReturnValue(true);
      }
   }

   // ============ 幻灵套垂直限制（恼鬼式 noPhysics=true 基础上的唯一限制点） ============
   // 0=未激活 1=垂直碰撞 2=完全穿透（mode2 方块内）
   private int ddj$state;
   private boolean ddj$groundHit;
   private boolean ddj$verticalHit;
   private Vec3 ddj$actualMove = Vec3.ZERO;
   private boolean ddj$gravityForced = false; // 本 mixin 是否曾经强制 noGravity=true

   @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
   private Vec3 ddj$applyVerticalCollision(Vec3 movement, MoverType type) {
      ddj$state = 0;
      ddj$groundHit = false;
      ddj$actualMove = movement;

      Entity self = (Entity) (Object) this;
      if (!(self instanceof Player player)) {
         return movement; // 非玩家实体不碰重力，由原版/其他 mod 自行管理
      }

      int mode = ModArmorSetHandler.spectralPhaseMode(player);
      // 超限相位：飞行时完全穿透（noPhysics 自由移动）；非飞行仅横向穿墙，
      // 垂直碰撞走下方同款路径——脚下方块照常支撑，不再坠入地下。
      // 幻灵幻影开启（mode>=1）即横向穿墙；虚化垂直激活（mode 2）时再全穿透。
      boolean transPhase = ModArmorSetHandler.isTranscendiumFlightPhasing(player) && !player.isSpectator();
      if (transPhase && player.getAbilities().flying) {
         if (ddj$gravityForced) {
            self.setNoGravity(false);
            ddj$gravityForced = false;
         }
         // noPhysics 分支不刷新碰撞标志，手动复位防残留 true 卡死疾跑
         self.horizontalCollision = false;
         self.minorHorizontalCollision = false;
         self.verticalCollision = false;
         return movement;
      }
      if (mode == 2 && ModArmorSetHandler.isVerticalPhaseActive(player)) {
         ddj$state = 2;
         if (!ddj$gravityForced) {
            self.setNoGravity(true);
            ddj$gravityForced = true;
         }
         return movement;
      }
      if (mode >= 1 || transPhase) {
         ddj$state = 1;
         if (ddj$gravityForced) {
            self.setNoGravity(false);
            ddj$gravityForced = false;
         }
         Vec3 horizontal = movement;
         // 潜行且身体没有穿入方块时：启用原版"方块边缘不探出"保护，
         // 幻灵套水平无碰撞，否则潜行走到方块边缘会直接走出悬空。
         if (player.isShiftKeyDown() && !ModArmorSetHandler.isBodyClippingBlock(self)) {
            horizontal = maybeBackOffFromEdge(movement, type);
         }
         double collidedY = ddj$collideVertical(self, horizontal.y);
         ddj$groundHit = horizontal.y < 0.0 && horizontal.y != collidedY;
         ddj$verticalHit = horizontal.y != collidedY;
         ddj$actualMove = new Vec3(horizontal.x, collidedY, horizontal.z);
         return ddj$actualMove;
      }
      if (ddj$gravityForced) {
         self.setNoGravity(false);
         ddj$gravityForced = false;
      }
      return movement;
   }

   /** 只对 y 分量做原版碰撞计算（方块 + 实体），与 Entity.collide 同源。 */
   private static double ddj$collideVertical(Entity entity, double dy) {
      if (Math.abs(dy) < 1.0E-7) {
         return 0.0;
      }
      Level level = entity.level();
      AABB box = entity.getBoundingBox();
      List<VoxelShape> entityShapes = level.getEntityCollisions(entity, box.expandTowards(0.0, dy, 0.0));
      Vec3 result = Entity.collideBoundingBox(entity, new Vec3(0.0, dy, 0.0), box, level, entityShapes);
      return result.y;
   }

   @Inject(
      method = "move",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;setPos(DDD)V",
         ordinal = 0,
         shift = At.Shift.AFTER
      )
   )
   private void ddj$afterNoPhysicsSetPos(MoverType type, Vec3 movement, CallbackInfo ci) {
      Entity self = (Entity) (Object) this;
      if (ddj$state == 1) {
         boolean onGround = ddj$groundHit;
         if (!onGround && ddj$collideVertical(self, -0.05) > -0.05 + 1.0E-5) {
            onGround = true;
         }
         self.setOnGround(onGround);
         // noPhysics 分支不刷新碰撞标志：水平方向穿墙必然无碰撞，必须复位，
         // 否则开启模式瞬间残留的 horizontalCollision=true 会每 tick 取消疾跑
         self.horizontalCollision = false;
         self.minorHorizontalCollision = false;
         self.verticalCollision = ddj$verticalHit;
         Vec3 moved = ddj$actualMove;
         if (onGround) {
            self.fallDistance = 0.0F;
         }
         Vec3 dm = self.getDeltaMovement();
         self.setDeltaMovement(dm.x, moved.y, dm.z);
         self.walkDist += (float) moved.horizontalDistance() * 0.6F;
         self.moveDist += (float) moved.length() * 0.6F;
      } else if (ddj$state == 2) {
         self.setOnGround(false);
         self.horizontalCollision = false;
         self.minorHorizontalCollision = false;
         self.verticalCollision = false;
      }
   }

   // ============ 超限靴流体液面精确钳位（旧 fix5 方案，move TAIL） ============
   @Inject(method = "move", at = @At("TAIL"))
   private void ddj$landOnFluidSurface(MoverType type, Vec3 movement, CallbackInfo ci) {
      if (type == MoverType.SELF && (Object) this instanceof Player player) {
         ModArmorSetHandler.landOnWalkableFluid(player);
      }
   }

   // ============ 防窒息（横向穿墙/虚化/超限相位穿墙，身体可进入方块） ============
   @Inject(method = "isInWall", at = @At("HEAD"), cancellable = true)
   private void ddj$noSuffocation(CallbackInfoReturnable<Boolean> cir) {
      Entity self = (Entity) (Object) this;
      if (self instanceof Player player && ModArmorSetHandler.isHorizontalPhaseActive(player)) {
         cir.setReturnValue(false);
      }
   }

   // ============ 超限相位：实体碰撞豁免 + 不进入游泳姿势 ============
   @Inject(method = "canCollideWith", at = @At("HEAD"), cancellable = true)
   private void ddj$phaseCanCollideWith(Entity other, CallbackInfoReturnable<Boolean> cir) {
      if ((Object) this instanceof Player player
            && (ModArmorSetHandler.isTranscendiumFlightPhasing(player)
                || PhantomDecoyManager.isPhantomState(player))) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "canBeCollidedWith", at = @At("HEAD"), cancellable = true)
   private void ddj$phaseCanBeCollidedWith(CallbackInfoReturnable<Boolean> cir) {
      if ((Object) this instanceof Player player
            && (ModArmorSetHandler.isTranscendiumFlightPhasing(player)
                || PhantomDecoyManager.isPhantomState(player))) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "isSwimming", at = @At("HEAD"), cancellable = true)
   private void ddj$phaseNoSwimming(CallbackInfoReturnable<Boolean> cir) {
      if ((Object) this instanceof Player player && ModArmorSetHandler.isTranscendiumFlightPhasing(player)) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "isVisuallySwimming", at = @At("HEAD"), cancellable = true)
   private void ddj$phaseNoVisualSwimming(CallbackInfoReturnable<Boolean> cir) {
      if ((Object) this instanceof Player player && ModArmorSetHandler.isTranscendiumFlightPhasing(player)) {
         cir.setReturnValue(false);
      }
   }
}
