package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 创造模板方案 v6 — SmithingMenuMixin
 *
 * 功能：取成品时不消耗创造模板。
 * 注意：slotsChanged 和 matches 不再通过 Mixin 注入（v5c 确认无法匹配），
 * 改为在 SmithingTransformRecipeMixin 的 <init> 中修改 template Ingredient。
 */
@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin {

    @Unique
    private ItemStack dingdongji$savedCreateTemplate = ItemStack.EMPTY;

    @Inject(method = "onTake", at = @At("HEAD"))
    private void dingdongji$onTakeHead(Player player, ItemStack stack, CallbackInfo ci) {
        SmithingMenu self = (SmithingMenu) (Object) this;
        ItemStack template = self.getSlot(0).getItem();
        // 必须拷贝整份物品（含 mode 组件），否则合成后会掉回 α
        this.dingdongji$savedCreateTemplate = ModItems.isCreateTemplate(template)
                ? template.copy()
                : ItemStack.EMPTY;
    }

    @Inject(method = "onTake", at = @At("TAIL"))
    private void dingdongji$onTakeTail(Player player, ItemStack stack, CallbackInfo ci) {
        if (!this.dingdongji$savedCreateTemplate.isEmpty()) {
            SmithingMenu self = (SmithingMenu) (Object) this;
            self.getSlot(0).set(this.dingdongji$savedCreateTemplate.copy());
        }
        this.dingdongji$savedCreateTemplate = ItemStack.EMPTY;
    }
}
