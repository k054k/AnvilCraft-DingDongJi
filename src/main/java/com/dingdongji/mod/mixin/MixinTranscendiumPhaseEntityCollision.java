package com.dingdongji.mod.mixin;

import com.dingdongji.mod.event.ModArmorSetHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 超限合金套飞行相位偏移时豁免<b>实体</b>碰撞（方块碰撞由
 * MixinPlayerTranscendiumFlightPhase 设 noPhysics=true 解决）。
 * <p>
 * 已实证的碰撞链路（1.21.1）：任何实体移动时 Entity.collide →
 * EntityGetter.getEntityCollisions(movingEntity, box)，其候选谓词为
 * {@code NO_SPECTATORS.and(movingEntity::canCollideWith)}，即对每个候选
 * candidate 调用 {@code candidate.canCollideWith(movingEntity)}；命中后
 * candidate 的 AABB 作为 VoxelShape 阻挡移动方。
 * <ul>
 *   <li>相位玩家 {@code canCollideWith} 强制 false → 其 AABB 对所有移动
 *       实体（玩家/生物/矿车/船）不可见，别人能直接穿过他；</li>
 *   <li>{@code canBeCollidedWith} 强制 false → 覆盖 movingEntity==null 的
 *       查询路径（EntitySelector.CAN_BE_COLLIDED_WITH），也让默认
 *       canCollideWith 的双向检查一致；</li>
 *   <li>{@code isSwimming}/{@code isVisuallySwimming} 强制 false →
 *       相位穿水时不进入游泳状态与水平划水视觉（防游泳姿势的状态层，
 *       姿势层见 MixinTranscendiumPhasePose）。</li>
 * </ul>
 * 不动 isPickable/canBeHitByProjectile：攻击、右键交互、投射物命中保持正常。
 */
@Mixin(Entity.class)
public class MixinTranscendiumPhaseEntityCollision {

   @Inject(method = "canCollideWith", at = @At("HEAD"), cancellable = true)
   private void ddj$phaseCanCollideWith(Entity other, CallbackInfoReturnable<Boolean> cir) {
      Entity self = (Entity) (Object) this;
      if (self instanceof Player player && ModArmorSetHandler.isTranscendiumFlightPhasing(player)) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "canBeCollidedWith", at = @At("HEAD"), cancellable = true)
   private void ddj$phaseCanBeCollidedWith(CallbackInfoReturnable<Boolean> cir) {
      Entity self = (Entity) (Object) this;
      if (self instanceof Player player && ModArmorSetHandler.isTranscendiumFlightPhasing(player)) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "isSwimming", at = @At("HEAD"), cancellable = true)
   private void ddj$phaseNoSwimming(CallbackInfoReturnable<Boolean> cir) {
      Entity self = (Entity) (Object) this;
      if (self instanceof Player player && ModArmorSetHandler.isTranscendiumFlightPhasing(player)) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "isVisuallySwimming", at = @At("HEAD"), cancellable = true)
   private void ddj$phaseNoVisualSwimming(CallbackInfoReturnable<Boolean> cir) {
      Entity self = (Entity) (Object) this;
      if (self instanceof Player player && ModArmorSetHandler.isTranscendiumFlightPhasing(player)) {
         cir.setReturnValue(false);
      }
   }
}
