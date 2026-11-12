package com.dingdongji.mod.accessor;

/**
 * 鸭子接口：暴露 LivingEntity.jumping，供服务端读取客户端同步的跳跃键状态。
 * 实现由 {@code MixinLivingEntity} 提供（@Accessor("jumping")）。
 */
public interface LivingEntityJumpingAccessor {
   boolean ddj$isJumping();
}
