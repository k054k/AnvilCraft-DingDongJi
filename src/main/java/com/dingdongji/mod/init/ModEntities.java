package com.dingdongji.mod.init;

import com.dingdongji.mod.KryptonMod;
import com.dingdongji.mod.entity.PhantomDecoyEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
   public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
      DeferredRegister.create(Registries.ENTITY_TYPE, KryptonMod.MODID);

   public static final Supplier<EntityType<PhantomDecoyEntity>> PHANTOM_DECOY =
      ENTITY_TYPES.register(
         "phantom_decoy",
         () -> EntityType.Builder.<PhantomDecoyEntity>of(PhantomDecoyEntity::new, MobCategory.MISC)
            .sized(0.6F, 1.8F)
            .eyeHeight(1.62F)
            .ridingOffset(-0.6F)
            .clientTrackingRange(32)
            .updateInterval(2)
            .build("phantom_decoy")
      );

   private ModEntities() {
   }

   @EventBusSubscriber(modid = KryptonMod.MODID, bus = Bus.MOD)
   public static class Attributes {
      @SubscribeEvent
      public static void onCreateAttributes(EntityAttributeCreationEvent event) {
         event.put(PHANTOM_DECOY.get(), PhantomDecoyEntity.createAttributes().build());
      }
   }
}
