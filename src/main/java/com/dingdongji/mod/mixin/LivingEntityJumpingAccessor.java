package com.dingdongji.mod.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 暴露 LivingEntity.jumping，供服务端读取客户端同步的跳跃键状态。 */
@Mixin(LivingEntity.class)
public interface LivingEntityJumpingAccessor {
   @Accessor("jumping")
   boolean ddj$isJumping();
}
