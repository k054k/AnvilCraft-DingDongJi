package com.dingdongji.mod.ability;

import com.dingdongji.mod.event.ModArmorSetHandler;
import com.dingdongji.mod.item.ModItems;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed;

public class JiSetAbility {
   public static final Map<UUID, Boolean> JI_SET_ENABLED = new HashMap<>();
   public static final Set<UUID> JI_SNEAKING = new HashSet<>();
   public static final ResourceLocation JI_SET_BREAK_SPEED_ID = ResourceLocation.parse("dingdongji:ji_set_break_speed");
   public static final ResourceLocation JI_SET_REACH_ID = ResourceLocation.parse("dingdongji:ji_set_reach");

   public static void toggleJiSet(ServerPlayer player) {
      if (ArmorSets.hasFullJiSet(player)) {
         UUID uuid = player.getUUID();
         // 开关只管"下蛋"，默认关闭；挖掘速度/交互距离是全套常驻被动
         boolean enabled = !JI_SET_ENABLED.getOrDefault(uuid, false);
         JI_SET_ENABLED.put(uuid, enabled);
         player.displayClientMessage(ArmorSets.actionMessage("下蛋", enabled ? "开" : "关", enabled ? ChatFormatting.GREEN : ChatFormatting.RED), true);
         ModArmorSetHandler.saveToggleStates(player);
      }
   }

   public static void handleJiSet(Player player) {
      boolean fullSet = ArmorSets.hasFullJiSet(player);
      AttributeInstance breakSpeed = player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
      AttributeInstance reach = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
      if (fullSet) {
         // 挖掘速度与交互距离：穿齐全套即生效的常驻被动，不受下蛋开关影响
         if (breakSpeed != null) {
            breakSpeed.removeModifier(JI_SET_BREAK_SPEED_ID);
            breakSpeed.addTransientModifier(new AttributeModifier(JI_SET_BREAK_SPEED_ID, 2.0, Operation.ADD_VALUE));
         }

         if (reach != null) {
            reach.removeModifier(JI_SET_REACH_ID);
            reach.addTransientModifier(new AttributeModifier(JI_SET_REACH_ID, 4.0, Operation.ADD_VALUE));
         }

         // 下蛋默认关闭，由玩家按键开关
         if (JI_SET_ENABLED.getOrDefault(player.getUUID(), false)) {
            handleJiSetEgg(player);
         } else {
            JI_SNEAKING.remove(player.getUUID());
         }
      } else {
         if (breakSpeed != null) {
            breakSpeed.removeModifier(JI_SET_BREAK_SPEED_ID);
         }

         if (reach != null) {
            reach.removeModifier(JI_SET_REACH_ID);
         }

         JI_SNEAKING.remove(player.getUUID());
      }
   }

   // 潜行按下瞬间在身后下一枚蛋：99% 普通蛋 / 0.9% 金蛋 / 0.1% 附魔金蛋
   private static void handleJiSetEgg(Player player) {
      UUID uuid = player.getUUID();
      boolean sneaking = player.isShiftKeyDown();
      boolean wasSneaking = JI_SNEAKING.contains(uuid);
      if (sneaking) {
         JI_SNEAKING.add(uuid);
      } else {
         JI_SNEAKING.remove(uuid);
      }

      if (!sneaking || wasSneaking || !(player.level() instanceof ServerLevel level)) {
         return;
      }

      double yaw = (double)player.getYRot() * Math.PI / 180.0;
      double bx = Math.sin(yaw);
      double bz = -Math.cos(yaw);
      ItemEntity egg = new ItemEntity(level, player.getX() + bx * 1.5, player.getY() + 0.5, player.getZ() + bz * 1.5, new ItemStack(pickJiEgg()));
      egg.setDefaultPickUpDelay();
      level.addFreshEntity(egg);
      level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL, 1.0F, 1.0F);
      level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHICKEN_HURT, SoundSource.NEUTRAL, 1.0F, 1.0F);
      level.sendParticles(ParticleTypes.POOF, player.getX() + bx, player.getY() + 0.6, player.getZ() + bz, 10, bx * 0.3, 0.2, bz * 0.3, 0.05);
   }

   // 模组独立环境只下普通蛋；金蛋/附魔金蛋概率表仅在整合包（mynethersdelight 存在）时启用
   private static Item pickJiEgg() {
      if (!net.neoforged.fml.ModList.get().isLoaded("mynethersdelight")) {
         return Items.EGG;
      }
      double r = Math.random();
      ResourceLocation id;
      if (r < 0.001) {
         id = ResourceLocation.parse("mynethersdelight:enchanted_golden_egg");
      } else if (r < 0.01) {
         id = ResourceLocation.parse("mynethersdelight:golden_egg");
      } else {
         return Items.EGG;
      }

      Item item = BuiltInRegistries.ITEM.get(id);
      return item == Items.AIR ? Items.EGG : item;
   }

   public static void onBreakSpeed(BreakSpeed event) {
      Player speed = event.getEntity();
      if (speed instanceof Player) {
         float var6 = event.getOriginalSpeed();
         boolean modified = false;
         if (!speed.onGround()
            && speed.getAbilities().flying
            && !speed.getAbilities().instabuild
            && speed.getItemBySlot(EquipmentSlot.FEET).is((Item)ModItems.TRANSCENDIUM_BOOTS.get())) {
            var6 *= 5.0F;
            modified = true;
         }

         if (speed.isEyeInFluid(FluidTags.WATER) && speed.getItemBySlot(EquipmentSlot.HEAD).is((Item)ModItems.TRANSCENDIUM_HELMET.get())) {
            double submerged = speed.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED);
            if (submerged > 1.0E-5 && Math.abs(submerged - 1.0) > 1.0E-5) {
               var6 /= (float)submerged;
               modified = true;
            }
         }

         if (modified) {
            event.setNewSpeed(var6);
         }
      }
   }

   public static void clearOnLogout(UUID uuid) {
      JI_SET_ENABLED.remove(uuid);
      JI_SNEAKING.remove(uuid);
   }

   public static void save(CompoundTag data, UUID uuid) {
      data.putBoolean("dingdongji:ji_egg", JI_SET_ENABLED.getOrDefault(uuid, false));
   }

   public static void load(CompoundTag data, UUID uuid) {
      JI_SET_ENABLED.put(uuid, data.getBoolean("dingdongji:ji_egg"));
   }
}
