package com.dingdongji.mod.mixin;

import com.dingdongji.mod.accessor.LivingEntityJumpingAccessor;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 0.1.1 合并类：LivingEntityJumpingAccessor（幻灵虚化垂直按键判定用）。
 * 原 MixinCanStandOnFluid（余烬靴熔岩站立）已移除：canStandOnFluid 注入会
 * 在头露出熔岩后把 travel 切回地面物理，导致玩家无法从熔岩游回液面；
 * 余烬靴熔岩行走现与超限合金靴同套液面钳位方案（见 SurfaceWalking），
 * 不再触碰原版流体物理。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements LivingEntityJumpingAccessor {

   // ============ 超限相位禁跳已移除 ============
   // 相位开启时 EntityMixin 的 state==2 分支每 move 强制 onGround=false，
   // 原版跳跃条件不成立，无需在 jumpFromGround 拦截。

   // ============ 过冷浮霜金属块冻伤已改为方块 stepOn 直接 freeze 伤害 ============
   // 不再走细雪 ticksFrozen 累积（那会导致屏幕结霜糊脸，且 isInPowderSnow 标记
   // 在 aiStep 开头被无条件复位，旧注入方案实证无效）。生物站上即按原版受伤
   // 无敌帧节奏扣血（约每 0.5 秒 1 点），见 SupercooledFrostMetalBlock#stepOn。

   /** 读取 LivingEntity#jumping（幻灵虚化垂直按键判定用）。 */
   @Accessor("jumping")
   @Override
   public abstract boolean ddj$isJumping();
}
