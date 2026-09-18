package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   targets = {"dev.dubhe.anvilcraft.util.GravityManager"}
)
public abstract class GravityManagerMixin {
   @Unique
   private static boolean ddj$hasFullTranscendiumSet(Entity entity) {
      return !(entity instanceof Player player)
         ? false
         : player.getItemBySlot(EquipmentSlot.HEAD).is((Item)ModItems.TRANSCENDIUM_HELMET.get())
            && player.getItemBySlot(EquipmentSlot.CHEST).is((Item)ModItems.TRANSCENDIUM_CHESTPLATE.get())
            && player.getItemBySlot(EquipmentSlot.LEGS).is((Item)ModItems.TRANSCENDIUM_LEGGINGS.get())
            && player.getItemBySlot(EquipmentSlot.FEET).is((Item)ModItems.TRANSCENDIUM_BOOTS.get());
   }

   @Inject(
      method = {"getGravityVector(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/Vec3;"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private static void dingdongji$cancelGravity1(Entity entity, CallbackInfoReturnable<Vec3> cir) {
      if (ddj$hasFullTranscendiumSet(entity)) {
         cir.setReturnValue(Vec3.ZERO);
      }
   }

   @Inject(
      method = {"getGravityVector(Lnet/minecraft/world/entity/Entity;D)Lnet/minecraft/world/phys/Vec3;"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private static void dingdongji$cancelGravity2(Entity entity, double movement, CallbackInfoReturnable<Vec3> cir) {
      if (ddj$hasFullTranscendiumSet(entity)) {
         cir.setReturnValue(Vec3.ZERO);
      }
   }
}
