package com.dingdongji.mod.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.lang.reflect.Field;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Keeps creative-inventory pocket SlotWrapper positions in sync with the
 * real PocketSlot coordinates for capacity > 12 (18/24 pockets).
 * <p>
 * Wrappers are created once in selectTab, snapshotting PocketSlot.x/y at a
 * time when the leggings data may not be synced yet. Anvilcraft's own
 * CreativePocketSlotMixin refreshes wrapper.x on every isActive() call, but
 * never wrapper.y — which was fine for the vanilla 12-slot layout whose y is
 * fixed in the PocketSlot constructor. Our upward extra rows change y to
 * 66/48/30, so without this the wrappers keep stale y values: several slots
 * overlap on the bottom three rows and findSlot always picks the lowest
 * index, making the right pocket (and the upper rows) unusable ("糠").
 * <p>
 * The -30 offset mirrors anvilcraft's selectTab ModifyArgs (wrapper y =
 * pocket.y - 30 for the creative layout). The x coordinate is left to
 * anvilcraft. SlotWrapper is package-private, so the target is referenced by
 * name and its "target" field is located via the runtime class.
 */
@Mixin(
   targets = "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen$SlotWrapper",
   priority = 2000
)
public abstract class MixinCreativeSlotWrapper {
   private static final int CREATIVE_Y_SHIFT = 30;

   private static Field ddj$targetField;
   private static Field ddj$slotYField;
   private static Class<?> ddj$pocketSlotClass;

   @ModifyReturnValue(method = "isActive", at = @At("RETURN"))
   private boolean ddj$syncPocketWrapperY(boolean active) {
      try {
         if (ddj$pocketSlotClass == null) {
            ddj$pocketSlotClass = Class.forName("dev.dubhe.anvilcraft.inventory.PocketSlot");
         }

         if (ddj$targetField == null) {
            ddj$targetField = this.getClass().getDeclaredField("target");
            ddj$targetField.setAccessible(true);
         }

         Object target = ddj$targetField.get(this);
         if (target != null && ddj$pocketSlotClass.isInstance(target)) {
            if (ddj$slotYField == null) {
               ddj$slotYField = Slot.class.getDeclaredField("y");
               ddj$slotYField.setAccessible(true);
            }

            ddj$slotYField.setInt(this, ((Slot)target).y - CREATIVE_Y_SHIFT);
         }
      } catch (Throwable ignored) {
      }

      return active;
   }
}
