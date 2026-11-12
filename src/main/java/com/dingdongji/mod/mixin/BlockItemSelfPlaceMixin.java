package com.dingdongji.mod.mixin;

import com.dingdongji.mod.event.ModArmorSetHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 原版 {@link BlockItem#canPlace} 除 canSurvive 外还会做实体碰撞检查
 * （Level#isUnobstructed 传入 null，不排除任何实体）：目标方块的碰撞箱与
 * 任何实体（含放置者自己）重叠即拒绝放置。穿墙时玩家身体处于方块内部，
 * 想在自身位置垫方块会被这条规则拦截。
 *
 * 本 Mixin：玩家处于墙体相位（幻灵虚化开启 mode 2，或超限相位偏移开启）
 * 时，把实体碰撞检查换成"仅排除放置者自身"的版本（isUnobstructed 传入
 * 玩家自身，原版语义会排除该实体及其骑乘者），其他实体的碰撞箱照常拦截
 * 放置。仅穿全套而未开启穿墙状态时不生效。
 * 目标位置必须可替换（空气/可替换方块）由 {@link BlockItem#place} 上游的
 * BlockPlaceContext#canPlace 保证，不会因此把方块塞进基岩等不可替换位置。
 */
@Mixin(BlockItem.class)
public abstract class BlockItemSelfPlaceMixin {
   @Shadow
   protected boolean mustSurvive() {
      return true;
   }

   @Inject(method = "canPlace", at = @At("HEAD"), cancellable = true)
   private void ddj$allowSelfPlace(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
      Player player = context.getPlayer();
      if (player == null) {
         return;
      }
      if (!ModArmorSetHandler.isWallPhaseActive(player)) {
         return;
      }
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      if (this.mustSurvive() && !state.canSurvive(level, pos)) {
         cir.setReturnValue(false);
         return;
      }
      // 与原版 Level#isUnobstructed(BlockState, BlockPos, CollisionContext)
      // 相同，仅把排除实体从 null 换成放置者自身
      VoxelShape shape = state.getCollisionShape(level, pos, CollisionContext.of(player));
      cir.setReturnValue(shape.isEmpty() || level.isUnobstructed(player, shape.move(pos.getX(), pos.getY(), pos.getZ())));
   }
}
