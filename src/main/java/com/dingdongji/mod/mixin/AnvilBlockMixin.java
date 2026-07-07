package com.dingdongji.mod.mixin;

import com.dingdongji.mod.block.ModBlockTags;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilBlock.class)
public abstract class AnvilBlockMixin {

    @Inject(method = "damage", at = @At("RETURN"), cancellable = true)
    private static void preventDamage(BlockState state, CallbackInfoReturnable<BlockState> cir) {
        if (state.is(ModBlockTags.CANT_BROKEN_ANVIL)) {
            cir.setReturnValue(state);
        }
    }
}
