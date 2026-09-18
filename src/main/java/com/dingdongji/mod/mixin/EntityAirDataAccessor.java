package com.dingdongji.mod.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({Entity.class})
public interface EntityAirDataAccessor {
   @Accessor("entityData")
   SynchedEntityData ddj$getEntityData();

   @Accessor("DATA_AIR_SUPPLY_ID")
   static EntityDataAccessor<Integer> ddj$getAirSupplyId() {
      throw new AssertionError();
   }
}
