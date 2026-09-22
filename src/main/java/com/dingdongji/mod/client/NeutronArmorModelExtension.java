package com.dingdongji.mod.client;

import com.dingdongji.mod.item.ModItems;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Routes the neutron spacesuit armor pieces to AnvilCraft's baked weatherproof
 * spacesuit models. The neutron set shares the exact custom geometry
 * (helmet/chest/legs/boots including the back thruster blocks); only the bound
 * texture differs, and that is resolved independently from our armor material
 * layer. AnvilCraft classes are resolved reflectively so the project keeps
 * compiling without an AnvilCraft compile dependency.
 */
public final class NeutronArmorModelExtension implements IClientItemExtensions {
   public static final NeutronArmorModelExtension INSTANCE = new NeutronArmorModelExtension();
   private static final Logger LOGGER = LoggerFactory.getLogger("dingdongji");

   private static boolean lookupFailed;
   private static Method anvilcraftGetModel;
   private static final Map<EquipmentSlot, ItemStack> ANVIL_REFERENCE = new EnumMap<>(EquipmentSlot.class);

   private NeutronArmorModelExtension() {
   }

   @Override
   public HumanoidModel<?> getHumanoidArmorModel(
      LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> defaultModel
   ) {
      HumanoidModel<?> model = fetchAnvilModel(slot, defaultModel);
      return model != null ? model : defaultModel;
   }

   private static synchronized HumanoidModel<?> fetchAnvilModel(EquipmentSlot slot, HumanoidModel<?> fallback) {
      if (lookupFailed) {
         return null;
      }
      try {
         if (anvilcraftGetModel == null) {
            Class<?> equipmentModels = Class.forName("dev.dubhe.anvilcraft.client.renderer.entity.model.EquipmentModels");
            anvilcraftGetModel = equipmentModels.getMethod("get", ItemStack.class, HumanoidModel.class);
         }
         ItemStack reference = ANVIL_REFERENCE.computeIfAbsent(slot, s -> referenceStack(s));
         if (reference.isEmpty()) {
            return null;
         }
         Object result = anvilcraftGetModel.invoke(null, reference, fallback);
         return result instanceof HumanoidModel<?> humanoid ? humanoid : null;
      } catch (ReflectiveOperationException | LinkageError e) {
         lookupFailed = true;
         LOGGER.warn("[DingDongJi] AnvilCraft equipment models unavailable; neutron spacesuit falls back to vanilla armor shape", e);
         return null;
      }
   }

   private static ItemStack referenceStack(EquipmentSlot slot) {
      String part = switch (slot) {
         case HEAD -> "weatherproof_spacesuit_helmet";
         case CHEST -> "weatherproof_spacesuit_chestplate";
         case LEGS -> "weatherproof_spacesuit_leggings";
         case FEET -> "weatherproof_spacesuit_boots";
         default -> "";
      };
      if (part.isEmpty()) {
         return ItemStack.EMPTY;
      }
      Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("anvilcraft", part));
      return item == null || item == net.minecraft.world.item.Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
   }

   public static boolean isNeutronPiece(ItemStack stack) {
      return stack.is(ModItems.NEUTRON_SPACESUIT_HELMET.get())
         || stack.is(ModItems.NEUTRON_SPACESUIT_CHESTPLATE.get())
         || stack.is(ModItems.NEUTRON_SPACESUIT_LEGGINGS.get())
         || stack.is(ModItems.NEUTRON_SPACESUIT_BOOTS.get());
   }
}
