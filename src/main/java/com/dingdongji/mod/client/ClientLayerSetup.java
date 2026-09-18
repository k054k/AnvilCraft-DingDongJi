package com.dingdongji.mod.client;

import com.dingdongji.mod.client.layer.EmissiveArmorLayer;
import com.dingdongji.mod.mixin.AccessorHumanoidArmorLayer;
import com.dingdongji.mod.mixin.AccessorLivingEntityRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers;

@EventBusSubscriber(
   modid = "dingdongji",
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public final class ClientLayerSetup {
   private ClientLayerSetup() {
   }

   @SubscribeEvent
   public static void onAddLayers(AddLayers event) {
      for (EntityType<?> entityType : event.getEntityTypes()) {
         EntityRenderer<?> renderer = event.getRenderer(entityType);
         if (renderer != null) {
            attach(renderer);
         }
      }

      for (Model skin : event.getSkins()) {
         attach(event.getSkin(skin));
      }
   }

   private static void attach(EntityRenderer<?> renderer) {
      if (renderer instanceof LivingEntityRenderer livingRenderer) {
         if (livingRenderer.getModel() instanceof HumanoidModel) {
            for (RenderLayer<?, ?> layer : ((AccessorLivingEntityRenderer)livingRenderer).ddj$getLayers()) {
               if (layer instanceof HumanoidArmorLayer armorLayer) {
                  HumanoidModel<?> inner = ((AccessorHumanoidArmorLayer)armorLayer).ddj$getInnerModel();
                  HumanoidModel<?> outer = ((AccessorHumanoidArmorLayer)armorLayer).ddj$getOuterModel();
                  livingRenderer.addLayer(new EmissiveArmorLayer(livingRenderer, inner, outer));
                  break;
               }
            }
         }
      }
   }
}
