package com.dingdongji.mod.client.layer;

import com.dingdongji.mod.ModClientConfig;
import com.dingdongji.mod.client.AfterimageManager;
import com.dingdongji.mod.client.GlowPhaseTracker;
import com.dingdongji.mod.item.ModItems;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.item.IonocraftBackpackItem;
import dev.dubhe.anvilcraft.item.WeatherproofChestplateItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Set;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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

   private static int breathColor(EmissiveArmorLayer.BreathSet set, float ageTicks, float partialTicks) {
      // Darkness is replayed from the actual block outline animation loaded
      // by the game (mcmeta frames/timings/interpolate), ticking on the same
      // 20 TPS client-tick clock and interpolated by partialTicks, so shader
      // pipelines that tick the atlas per-frame cannot speed it up. Fallback
      // only if the sprite definition is unavailable.
      float q = GlowPhaseTracker.darkness(set.outline, partialTicks);
      if (q < 0.0F) {
         float p = ageTicks % BREATH_PERIOD / BREATH_PERIOD;
         q = p < 0.5F ? 1.0F - p * 2.0F : p * 2.0F - 1.0F;
      }

      int r = Math.round((float)set.rgb[0] + (float)(set.rgb[3] - set.rgb[0]) * q);
      int g = Math.round((float)set.rgb[1] + (float)(set.rgb[4] - set.rgb[1]) * q);
      int b = Math.round((float)set.rgb[2] + (float)(set.rgb[5] - set.rgb[2]) * q);
      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   /**
    * 飘升机胸甲装饰牌的三态发光——颜色完全取自原贴图 UV 48-64×56-64 的
    * 真实色带像素，mask 只提供位置与形状，vertex color 用来调亮度/alpha：
    * <ul>
    *   <li>在电网中：vertex 全白 → 原贴图原色带完全透出，等于常亮；</li>
    *   <li>离网且电池有余电（仅橙白耐候胸甲有电池）：vertex 在 0.4 / 1.0
    *       之间呼吸插值 → 原贴图颜色按时间脉动；</li>
    *   <li>无电（紫色背包本身无法离网供电，或橙白电池耗尽）：vertex 亮度
    *       压到 0.15，呈暗淡色块。</li>
    * </ul>
    */
   private static int anvilBadgeColor(Item item, ItemStack stack, LivingEntity entity, float ageTicks) {
      boolean inGrid = Boolean.TRUE.equals(entity.getData(ModDataAttachments.IN_POWER_GRID));
      if (inGrid) {
         return 0xFFFFFFFF;
      }

      boolean hasEnergy = item instanceof WeatherproofChestplateItem
         && WeatherproofChestplateItem.getEnergyStored(stack) > 0;
      if (hasEnergy) {
         float p = ageTicks % BREATH_PERIOD / BREATH_PERIOD;
         float q = p < 0.5F ? 1.0F - p * 2.0F : p * 2.0F - 1.0F;
         float brightness = 0.4F + 0.6F * q;
         int b = Math.round(brightness * 255.0F);
         return 0xFF000000 | b << 16 | b << 8 | b;
      }

      int dim = Math.round(0.15F * 255.0F);
      return 0xFF000000 | dim << 16 | dim << 8 | dim;
   }

   private static EmissiveArmorLayer.BreathSet breathSetOf(Item item) {
      if (item == ModItems.EMBER_METAL_HELMET.get()
         || item == ModItems.EMBER_METAL_CHESTPLATE.get()
         || item == ModItems.EMBER_METAL_LEGGINGS.get()
         || item == ModItems.EMBER_METAL_BOOTS.get()) {
         return EmissiveArmorLayer.BreathSet.EMBER;
      } else {
         return item == ModItems.TRANSCENDIUM_HELMET.get()
            || item == ModItems.TRANSCENDIUM_CHESTPLATE.get()
            || item == ModItems.TRANSCENDIUM_LEGGINGS.get()
            || item == ModItems.TRANSCENDIUM_BOOTS.get()
         ? EmissiveArmorLayer.BreathSet.TRANS
         : EmissiveArmorLayer.BreathSet.FROST;
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
         this.renderPiece(pose, buffers, packedLight, entity, EquipmentSlot.CHEST, breathTicks, partialTicks);
         this.renderPiece(pose, buffers, packedLight, entity, EquipmentSlot.LEGS, breathTicks, partialTicks);
         this.renderPiece(pose, buffers, packedLight, entity, EquipmentSlot.FEET, breathTicks, partialTicks);
         this.renderPiece(pose, buffers, packedLight, entity, EquipmentSlot.HEAD, breathTicks, partialTicks);
      }
   }

   private void renderPiece(PoseStack pose, MultiBufferSource buffers, int packedLight, T entity, EquipmentSlot slot, float breathTicks, float partialTicks) {
      ItemStack stack = entity.getItemBySlot(slot);
      if (stack.getItem() instanceof ArmorItem armorItem && armorItem.getEquipmentSlot() == slot) {
         Item item = armorItem;
         boolean ddjGlow = GLOW_ARMOR.contains(item);
         // AnvilCraft's two spacesuit chestplates (purple ionocraft backpack and
         // orange-white weatherproof chestplate) carry the front+back diamond
         // badges modelled as extra body cubes. They share the same custom
         // HumanoidModel path, so the breathing overlay can be drawn exactly
         // like our own armor glow.
         boolean anvilBadge = slot == EquipmentSlot.CHEST && item instanceof IonocraftBackpackItem;
         if (!ddjGlow && !anvilBadge) {
            return;
         }

         boolean inner = slot == EquipmentSlot.LEGS;
         HumanoidModel<T> context = inner ? this.innerModel : this.outerModel;
         // Resolve the same custom armor model the base armor layer uses
         // (e.g. neutron suit / AnvilCraft spacesuit geometry), so the
         // dark underlay and emissive overlay align with the worn shape.
         HumanoidModel<T> model = (HumanoidModel<T>)net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(stack)
            .getHumanoidArmorModel(entity, stack, slot, context);
         ((HumanoidModel)this.getParentModel()).copyPropertiesTo(model);
         setPartVisibility(model, slot);
         BreathSet breathSet = anvilBadge ? null : breathSetOf(item);
         int color = anvilBadge ? anvilBadgeColor(item, stack, entity, breathTicks) : breathColor(breathSet, breathTicks, partialTicks);
         ArmorMaterial material = (ArmorMaterial)armorItem.getMaterial().value();

         for (Layer layer : material.layers()) {
            ResourceLocation armorTexture = ClientHooks.getArmorTexture(entity, stack, layer, inner, slot);
            ResourceLocation glowTexture = anvilBadge ? anvilBadgeGlowLocation(armorTexture) : toSuffixLocation(armorTexture, "_glow.png");
            if (ddjGlow) {
               // When the band is enabled the vanilla armor texture (which carries
               // a static bright band) is first overpainted with the "_dark"
               // variant whose band area is painted black, so only the animated
               // emissive layer below supplies the band color. The dark base only
               // exists for the outer model (layer_1); the leggings layer keeps
               // its vanilla texture. The AnvilCraft badges keep their original
               // base texture — only the badges are overlaid with light.
               if (!inner) {
                  ResourceLocation darkTexture = toSuffixLocation(armorTexture, "_dark.png");
                  VertexConsumer baseConsumer = buffers.getBuffer(RenderType.armorCutoutNoCull(darkTexture));
                  model.renderToBuffer(pose, baseConsumer, packedLight, OverlayTexture.NO_OVERLAY, -1);
               }
            }

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

   // The spacesuit badge glow masks live in our own namespace. Only the badge
   // UV box (u48-64, v56-64) is opaque in those masks, so redrawing the whole
   // spacesuit model lights exactly the two diamond badges and nothing else.
   private static ResourceLocation anvilBadgeGlowLocation(ResourceLocation armorTexture) {
      String path = armorTexture.getPath();
      String file = path.substring(path.lastIndexOf('/') + 1);
      if (file.endsWith(".png")) {
         file = file.substring(0, file.length() - 4);
      }

      return ResourceLocation.fromNamespaceAndPath("dingdongji", "textures/entity/equipment/" + file + "_glow.png");
   }

   private static ResourceLocation toSuffixLocation(ResourceLocation armorTexture, String suffix) {
      String path = armorTexture.getPath();
      if (path.endsWith(".png")) {
         path = path.substring(0, path.length() - 4) + suffix;
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
