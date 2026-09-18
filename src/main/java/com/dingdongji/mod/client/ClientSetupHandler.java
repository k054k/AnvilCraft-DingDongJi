package com.dingdongji.mod.client;

import com.dingdongji.mod.client.particle.IonocraftBootsExhaustParticle;
import com.dingdongji.mod.client.particle.NeutronBarrierParticle;
import com.dingdongji.mod.init.ModParticles;
import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.item.component.CreateTemplateMode;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

public class ClientSetupHandler {
   public static void onClientSetup(FMLClientSetupEvent event) {
      event.enqueueWork(
         () -> ItemProperties.register(
               (Item)ModItems.CREATE_TEMPLATE.get(), ResourceLocation.fromNamespaceAndPath("dingdongji", "mode"), (stack, level, entity, seed) -> {
                  CreateTemplateMode mode = (CreateTemplateMode)stack.get((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get());
                  if (mode == null) {
                     mode = CreateTemplateMode.DEFAULT;
                  }

                  String var5 = mode.mode();

                  return switch (var5) {
                     case "alpha" -> 0.0F;
                     case "beta" -> 1.0F;
                     case "gamma" -> 2.0F;
                     case "delta" -> 3.0F;
                     case "epsilon" -> 4.0F;
                     case "zeta" -> 5.0F;
                     default -> 0.0F;
                  };
               }
            )
      );
   }

   public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
      event.registerSpriteSet((ParticleType)ModParticles.IONOCRAFT_BOOTS_EXHAUST.get(), IonocraftBootsExhaustParticle.Provider::new);
      event.registerSpriteSet((ParticleType)ModParticles.NEUTRON_BARRIER_REPEL.get(), NeutronBarrierParticle.Provider::new);
      event.registerSpriteSet((ParticleType)ModParticles.NEUTRON_BARRIER_REPEL_BIG.get(), NeutronBarrierParticle.Provider::new);
      event.registerSpriteSet((ParticleType)ModParticles.NEUTRON_BARRIER_ABSORB.get(), NeutronBarrierParticle.Provider::new);
   }
}
