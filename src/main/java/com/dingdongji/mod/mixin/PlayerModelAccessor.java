package com.dingdongji.mod.mixin;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 暴露 {@link PlayerModel#cloak}（private）：幻灵替身借主人玩家渲染器绘制，
 * 渲染窗口内通过 cloak.visible=false 让披风层（CloakLayer）跳过绘制，
 * 使替身披风不随本体移动飘动。
 */
@Mixin(PlayerModel.class)
public interface PlayerModelAccessor {
   @Accessor("cloak")
   ModelPart ddj$getCloak();

   @Accessor("cloak")
   void ddj$setCloak(ModelPart cloak);
}
