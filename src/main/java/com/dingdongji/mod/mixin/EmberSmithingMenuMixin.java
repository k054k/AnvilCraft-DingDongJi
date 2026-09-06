package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.component.CreateTemplateMode;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 余烬锻造台 Mixin
 *
 * 1. @Redirect 拦截 lambda 方法中的 is(Holder) 调用，让创造模板 δ 模式通过八合一检查
 * 2. @Inject getInputSize 返回正确的输入数量
 * 3. @Inject canCreateResult 检查正确的槽位数量
 *
 * 关键：不用 @Shadow inputSlots（无 refMap 会失败），用 getSlot(i).getItem() 代替。
 */
@Mixin(targets = "dev.dubhe.anvilcraft.inventory.EmberSmithingMenu")
public abstract class EmberSmithingMenuMixin {

    @Redirect(
        method = "*",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/core/Holder;)Z")
    )
    private boolean redirectIsHolder(ItemStack stack, Holder<Item> holder) {
        boolean original = stack.getItem() == holder.value();
        if (!original) {
            ResourceLocation holderId = BuiltInRegistries.ITEM.getKey(holder.value());
            if (holderId.equals(ResourceLocation.parse("anvilcraft:eight_to_one_smithing_template"))) {
                if (ModItems.isCreateTemplate(stack)) {
                    CreateTemplateMode mode = stack.get(ModComponents.CREATE_TEMPLATE_MODE.get());
                    if (mode != null && "delta".equals(mode.mode())) {
                        return true;
                    }
                }
            }
        }
        return original;
    }

    @Inject(method = "isUsableTemplate", at = @At("HEAD"), cancellable = true, remap = false)
    private void ddj$usableTemplate(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        // 创造模板视为可用模板，使其能出现在模板面板（配合展开逻辑显示 β/γ/δ）
        if (ModItems.isCreateTemplate(stack)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getInputSize", at = @At("HEAD"), cancellable = true, remap = false)
    private void onGetInputSize(CallbackInfoReturnable<Integer> cir) {
        ItemStack t = ((net.minecraft.world.inventory.AbstractContainerMenu)(Object)this).getSlot(0).getItem();
        if (!t.is(ModItems.CREATE_TEMPLATE.get())) return;
        CreateTemplateMode m = t.get(ModComponents.CREATE_TEMPLATE_MODE.get());
        if (m == null) return;
        cir.setReturnValue(switch (m.mode()) {
            case "delta" -> 8; case "gamma" -> 4; case "beta" -> 2; default -> 0;
        });
    }

    @Inject(method = "canCreateResult", at = @At("HEAD"), cancellable = true, remap = false)
    private void onCanCreateResult(CallbackInfoReturnable<Boolean> cir) {
        net.minecraft.world.inventory.AbstractContainerMenu self = (net.minecraft.world.inventory.AbstractContainerMenu)(Object)this;
        ItemStack t = self.getSlot(0).getItem();
        if (!t.is(ModItems.CREATE_TEMPLATE.get())) return;
        if (self.getSlot(1).getItem().isEmpty()) { cir.setReturnValue(false); return; }
        CreateTemplateMode m = t.get(ModComponents.CREATE_TEMPLATE_MODE.get());
        if (m == null) { cir.setReturnValue(false); return; }
        int size = switch (m.mode()) {
            case "delta" -> 8; case "gamma" -> 4; case "beta" -> 2; default -> 0;
        };
        if (size == 0) { cir.setReturnValue(false); return; }
        for (int i = 0; i < size; i++) {
            if (self.getSlot(2 + i).getItem().isEmpty()) { cir.setReturnValue(false); return; }
        }
        cir.setReturnValue(true);
    }
}
