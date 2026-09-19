package com.dingdongji.mod.client.layer;

import com.dingdongji.mod.ModClientConfig;
import com.dingdongji.mod.client.AfterimageManager;
import com.dingdongji.mod.client.GlowPhaseTracker;
import com.dingdongji.mod.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Set;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorMaterial.Layer;
import net.neoforged.neoforge.client.ClientHooks;

public class EmissiveArmorLayer<T extends LivingEntity> extends RenderLayer<T, HumanoidModel<T>> {
   private static final Set<Item> GLOW_ARMOR = Set.of(
      (Item)ModItems.FROST_METAL_HELMET.get(),
      (Item)ModItems.FROST_METAL_CHESTPLATE.get(),
      (Item)ModItems.FROST_METAL_LEGGINGS.get(),
      (Item)ModItems.FROST_METAL_BOOTS.get(),
      (Item)ModItems.EMBER_METAL_HELMET.get(),
      (Item)ModItems.EMBER_METAL_CHESTPLATE.get(),
      (Item)ModItems.EMBER_METAL_LEGGINGS.get(),
      (Item)ModItems.EMBER_METAL_BOOTS.get(),
      (Item)ModItems.TRANSCENDIUM_HELMET.get(),
      (Item)ModItems.TRANSCENDIUM_CHESTPLATE.get(),
      (Item)ModItems.TRANSCENDIUM_LEGGINGS.get(),
      (Item)ModItems.TRANSCENDIUM_BOOTS.get()
   );
   private static final float BREATH_PERIOD = 40.0F;
   private final HumanoidModel<T> innerModel;
   private final HumanoidModel<T> outerModel;

   private static int breathColor(EmissiveArmorLayer.BreathSet set, float ageTicks) {
      // Darkness is replayed from the actual block outline animation loaded
      // by the game (mcmeta frames/timings/interpolate), ticking on the same
      // atlas pulse. Fallback only if the sprite definition is unavailable.
      float q = GlowPhaseTracker.darkness(set.outline);
      if (q < 0.0F) {
         float p = ageTicks % BREATH_PERIOD / BREATH_PERIOD;
         q = p < 0.5F ? 1.0F - p * 2.0F : p * 2.0F - 1.0F;
      }

      int r = Math.round((float)set.rgb[0] + (float)(set.rgb[3] - set.rgb[0]) * q);
      int g = Math.round((float)set.rgb[1] + (float)(set.rgb[4] - set.rgb[1]) * q);
      int b = Math.round((float)set.rgb[2] + (float)(set.rgb[5] - set.rgb[2]) * q);
      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   private static EmissiveArmorLayer.BreathSet breathSetOf(Item item) {
      if (item == ModItems.EMBER_METAL_HELMET.get()
         || item == ModItems.EMBER_METAL_CHESTPLATE.get()
         || item == ModItems.EMBER_METAL_LEGGINGS.get()
         || item == ModItems.EMBER_METAL_BOOTS.get()) {
         return EmissiveArmorLayer.BreathSet.EMBER;
      } else {
         return item != ModItems.TRANSCENDIUM_HELMET.get()
               && item != ModItems.TRANSCENDIUM_CHESTPLATE.get()
               && item != ModItems.TRANSCENDIUM_LEGGINGS.get()
               && item != ModItems.TRANSCENDIUM_BOOTS.get()
            ? EmissiveArmorLayer.BreathSet.FROST
            : EmissiveArmorLayer.BreathSet.TRANS;
      }
   }

   public EmissiveArmorLayer(RenderLayerParent<T, HumanoidModel<T>> parent, HumanoidModel<?> innerModel, HumanoidModel<?> outerModel) {
      super(parent);
      this.innerModel = (HumanoidModel<T>)innerModel;
      this.outerModel = (HumanoidModel<T>)outerModel;
   }

   public void render(
      PoseStack pose,
      MultiBufferSource buffers,
      int packedLight,
      T entity,
      float limbSwing,
      float limbSwingAmount,
      float partialTicks,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      if (!ModClientConfig.glowBandEnabled()) {
         return;
      }

      if (!AfterimageManager.isRenderingAfterimage()) {
         float breathTicks = (float)entity.level().getGameTime() + partialTicks;
         this.renderPiece(pose, buffers, entity, EquipmentSlot.CHEST, breathTicks);
         this.renderPiece(pose, buffers, entity, EquipmentSlot.LEGS, breathTicks);
         this.renderPiece(pose, buffers, entity, EquipmentSlot.FEET, breathTicks);
         this.renderPiece(pose, buffers, entity, EquipmentSlot.HEAD, breathTicks);
      }
   }

   private void renderPiece(PoseStack pose, MultiBufferSource buffers, T entity, EquipmentSlot slot, float breathTicks) {
      ItemStack stack = entity.getItemBySlot(slot);
      if (stack.getItem() instanceof ArmorItem armorItem) {
         if (armorItem.getEquipmentSlot() == slot && GLOW_ARMOR.contains(armorItem)) {
            boolean inner = slot == EquipmentSlot.LEGS;
            HumanoidModel<T> model = inner ? this.innerModel : this.outerModel;
            ((HumanoidModel)this.getParentModel()).copyPropertiesTo(model);
            setPartVisibility(model, slot);
            int color = breathColor(breathSetOf(armorItem), breathTicks);
            ArmorMaterial material = (ArmorMaterial)armorItem.getMaterial().value();

            for (Layer layer : material.layers()) {
               ResourceLocation armorTexture = ClientHooks.getArmorTexture(entity, stack, layer, inner, slot);
               ResourceLocation glowTexture = toGlowLocation(armorTexture);
               VertexConsumer consumer = buffers.getBuffer(GlowArmorRenderType.glowArmor(glowTexture));
               // Vanilla eyes-layer recipe (spider / enderman): redraw the exact
               // same model with the identical pose. Bit-identical vertices give
               // bit-identical rasterized depth, so the LEQUAL depth test always
               // passes - no z-fighting, no dropped pixels while limbs swing, no
               // offset seam at any angle. Any scaling would break bit equality
               // and reintroduce all three artifacts, hence deliberately none.
               model.renderToBuffer(pose, consumer, 15728880, OverlayTexture.NO_OVERLAY, color);
            }
         }
      }
   }

   private static ResourceLocation toGlowLocation(ResourceLocation armorTexture) {
      String path = armorTexture.getPath();
      if (path.endsWith(".png")) {
         path = path.substring(0, path.length() - 4) + "_glow.png";
      }

      return ResourceLocation.fromNamespaceAndPath(armorTexture.getNamespace(), path);
   }

   private static void setPartVisibility(HumanoidModel<?> model, EquipmentSlot slot) {
      model.setAllVisible(false);
      switch (slot) {
         case HEAD:
            model.head.visible = true;
            model.hat.visible = true;
            break;
         case CHEST:
            model.body.visible = true;
            model.rightArm.visible = true;
            model.leftArm.visible = true;
            break;
         case LEGS:
            model.body.visible = true;
            model.rightLeg.visible = true;
            model.leftLeg.visible = true;
            break;
         case FEET:
            model.rightLeg.visible = true;
            model.leftLeg.visible = true;
      }
   }

   private static enum BreathSet {
      EMBER(255, 250, 180, 215, 129, 3, GlowPhaseTracker.Outline.EMBER),
      FROST(255, 255, 255, 183, 197, 207, GlowPhaseTracker.Outline.FROST),
      TRANS(255, 230, 255, 145, 25, 255, GlowPhaseTracker.Outline.TRANS);

      private final int[] rgb = new int[6];
      private final GlowPhaseTracker.Outline outline;

      private BreathSet(int rHi, int gHi, int bHi, int rLo, int gLo, int bLo, GlowPhaseTracker.Outline outline) {
         this.rgb[0] = rHi;
         this.rgb[1] = gHi;
         this.rgb[2] = bHi;
         this.rgb[3] = rLo;
         this.rgb[4] = gLo;
         this.rgb[5] = bLo;
         this.outline = outline;
      }
   }
}
