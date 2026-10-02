package com.dingdongji.mod.mixin;

import com.dingdongji.mod.event.ModArmorSetHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 原版 {@link BlockItem#canPlace} 除 canSurvive 外还会做实体碰撞检查
 * （Level#isUnobstructed）：目标方块的碰撞箱与任何实体（含放置者自己）
 * 重叠即拒绝放置。穿墙时玩家身体处于方块内部，想在自身位置垫方块会被
 * 这条规则拦截。
 *
 * 本 Mixin：玩家穿着全套幻灵套或全套超限合金套时，跳过实体碰撞检查，
 * 仅保留 canSurvive 校验。目标位置必须可替换（空气/可替换方块）由
 * {@link BlockItem#place} 上游的 BlockPlaceContext#canPlace 保证，
 * 不会因此把方块塞进基岩等不可替换位置。
 */
@Mixin(BlockItem.class)
public abstract class MixinBlockItemSelfPlace {
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
      if (!ModArmorSetHandler.hasFullSpectralSet(player) && !ModArmorSetHandler.hasFullTranscendiumSet(player)) {
         return;
      }
      cir.setReturnValue(!this.mustSurvive() || state.canSurvive(context.getLevel(), context.getClickedPos()));
   }
}
