package com.dingdongji.mod.mixin;

import com.dingdongji.mod.event.ModArmorSetHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 幻灵套垂直限制（恼鬼式 noPhysics=true 基础上的唯一限制点）。
 *
 * noPhysics=true 时 {@link Entity#move} 第 608-609 行直接 setPos，不做
 * 任何碰撞，也不更新 onGround / walkDist（屏幕晃动）/ fallDistance。
 *
 * 本 Mixin：
 *   HEAD（@ModifyVariable）：mode 1 或 mode 2 地表（身体不在方块内）时，
 *     只对 movement 的 y 分量调用原版 collideBoundingBox 做垂直碰撞，
 *     水平分量原封不动（穿墙）；mode 2 身体处于方块内时 y 不碰撞（垂直
 *     穿透），并强制 onGround=false 防止原版 jumpFromGround 干扰。
 *   609 setPos 之后：补回 onGround、垂直速度同步、fallDistance 清零、
 *     walkDist/moveDist 累积（行走屏幕晃动）。
 *
 * 这样 onGround 在空中=false、在地面=true，原版跳跃、坠落、自动跳跃、
 * 屏幕晃动全部自然成立，无需抑制 jumpFromGround。
 */
@Mixin(Entity.class)
public class MixinSpectralMoveCollision {
   // 0=未激活 1=垂直碰撞 2=完全穿透（mode2 方块内）
   private int ddj$state;
   private boolean ddj$groundHit;
   private Vec3 ddj$actualMove = Vec3.ZERO;

   @org.spongepowered.asm.mixin.injection.ModifyVariable(
      method = "move",
      at = @At("HEAD"),
      argsOnly = true
   )
   private Vec3 ddj$applyVerticalCollision(Vec3 movement) {
      ddj$state = 0;
      ddj$groundHit = false;
      ddj$actualMove = movement;

      Entity self = (Entity) (Object) this;
      if (!(self instanceof Player player)) {
         self.setNoGravity(false);
         return movement;
      }
      // mode 1 / mode 2 未激活垂直穿透时（陆地与流体一致），只对 y 做
      // 原版碰撞：脚触底停在方块顶（不陷入），水平穿透与浮力（travel
      // 流体逻辑）不受影响。mode 2 激活（身体在方块内或触底按 shift）
      // 时走上方 state=2 分支，流体中同样适用。

      int mode = ModArmorSetHandler.spectralPhaseMode(player);
      if (mode == 2 && ModArmorSetHandler.isVerticalPhaseActive(player)) {
         // 虚化垂直穿透：身体在方块内，或地表按 shift 开始下潜
         ddj$state = 2;
         self.setNoGravity(true);
         return movement;
      }
      if (mode >= 1) {
         // mode 1，或 mode 2 地表未下潜：y 分量原版碰撞，保留重力
         ddj$state = 1;
         self.setNoGravity(false);
         return collideAndStore(self, movement);
      }
      self.setNoGravity(false);
      return movement;
   }

   private Vec3 collideAndStore(Entity self, Vec3 movement) {
      double collidedY = collideVertical(self, movement.y);
      ddj$groundHit = movement.y < 0.0 && movement.y != collidedY;
      ddj$actualMove = new Vec3(movement.x, collidedY, movement.z);
      return ddj$actualMove;
   }

   /** 只对 y 分量做原版碰撞计算（方块 + 实体），与 Entity.collide 同源。 */
   private static double collideVertical(Entity entity, double dy) {
      if (Math.abs(dy) < 1.0E-7) {
         return 0.0;
      }
      Level level = entity.level();
      AABB box = entity.getBoundingBox();
      List<VoxelShape> entityShapes = level.getEntityCollisions(entity, box.expandTowards(0.0, dy, 0.0));
      Vec3 result = Entity.collideBoundingBox(entity, new Vec3(0.0, dy, 0.0), box, level, entityShapes);
      return result.y;
   }

   /** noPhysics 分支 setPos（move 中第一个 setPos，ordinal=0）之后补状态。 */
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
         // groundHit：本帧向下移动被碰撞挡住。
         // 吸附到顶面的情况 y=0，用 -0.05 探测是否贴地（下不去即贴地）。
         boolean onGround = ddj$groundHit;
         if (!onGround) {
            double probe = collideVertical(self, -0.05);
            if (probe > -0.05 + 1.0E-5) {
               onGround = true;
            }
         }
         self.setOnGround(onGround);
         Vec3 moved = ddj$actualMove;
         if (onGround) {
            self.fallDistance = 0.0F;
         }
         // 垂直速度同步为实际移动量，防止下一帧继续尝试穿入支撑面
         Vec3 dm = self.getDeltaMovement();
         self.setDeltaMovement(dm.x, moved.y, dm.z);
         // 行走统计：walkDist 驱动屏幕视角晃动
         self.walkDist += (float) moved.horizontalDistance() * 0.6F;
         self.moveDist += (float) moved.length() * 0.6F;
      } else if (ddj$state == 2) {
         // 垂直穿透中：onGround 必须为 false，否则 aiStep 自动 jumpFromGround
         self.setOnGround(false);
      }
   }

   /** 穿墙后身体与方块重叠，防止 isInWall 触发窒息。 */
   @Inject(method = "isInWall", at = @At("HEAD"), cancellable = true)
   private void ddj$noSuffocation(CallbackInfoReturnable<Boolean> cir) {
      Entity self = (Entity) (Object) this;
      if (self instanceof Player player && ModArmorSetHandler.spectralPhaseMode(player) >= 1) {
         cir.setReturnValue(false);
      }
   }
}
