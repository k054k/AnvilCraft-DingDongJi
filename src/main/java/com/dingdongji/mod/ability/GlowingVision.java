package com.dingdongji.mod.ability;

import com.dingdongji.mod.event.ModArmorSetHandler;
import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.item.component.GlowingVisionComponent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;

public class GlowingVision {
   public static final Map<UUID, Boolean> GLOWING_VISION_ENABLED = new HashMap<>();
   /** 高亮视野：每个玩家当前一轮仍在范围内、由我们施加发光的目标，掉队立即移除效果。 */
   private static final Map<UUID, Set<UUID>> GLOWING_TARGETS = new HashMap<>();

   public static void handleTranscendiumHelmet(Player player) {
      ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
      boolean wearingHelmet = helmet.is((Item) ModItems.TRANSCENDIUM_HELMET.get());
      if (wearingHelmet && player.hasEffect(MobEffects.DARKNESS)) {
         player.removeEffect(MobEffects.DARKNESS);
      }

      // 节流：每 20 tick（1 秒）扫描一轮
      if (player.tickCount % 20 != 0) {
         return;
      }
      UUID uuid = player.getUUID();
      if (!wearingHelmet || !GLOWING_VISION_ENABLED.getOrDefault(uuid, false)) {
         // 摘下头盔或关闭高亮：上一轮所有目标的发光立即清除
         clearGlowingTargets(player);
         return;
      }

      GlowingVisionComponent vision = (GlowingVisionComponent) helmet.get((DataComponentType) ModComponents.GLOWING_VISION.get());
      if (vision == null) {
         vision = GlowingVisionComponent.DEFAULT;
      }

      int range = vision.range() * 2;
      AABB area = player.getBoundingBox().inflate((double) range);

      Set<UUID> current = new HashSet<>();
      for (LivingEntity living : player.level()
         .getEntitiesOfClass(
            LivingEntity.class,
            area,
            e -> e != player && e.isAlive() && (e instanceof Enemy || e instanceof Mob mob && mob.getTarget() == player) && isHostileTo(e, player)
         )) {
         current.add(living.getUUID());
         MobEffectInstance existing = living.getEffect(MobEffects.GLOWING);
         if (existing == null || existing.getDuration() <= 100) {
            living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false, true));
         }
      }

      // 离开范围/死亡/转为非敌对的目标：立即摘掉发光，不等 200 tick 效果自然结束
      Set<UUID> previous = GLOWING_TARGETS.put(uuid, current);
      if (previous != null && player.level() instanceof ServerLevel serverLevel) {
         for (UUID targetId : previous) {
            if (current.contains(targetId)) {
               continue;
            }
            if (serverLevel.getEntity(targetId) instanceof LivingEntity living
                  && living.hasEffect(MobEffects.GLOWING)) {
               living.removeEffect(MobEffects.GLOWING);
            }
         }
      }
   }

   /** 立即清除该玩家高亮视野施加过的全部发光效果（关开关/摘头盔/下线）。 */
   public static void clearGlowingTargets(Player player) {
      Set<UUID> previous = GLOWING_TARGETS.remove(player.getUUID());
      if (previous == null || previous.isEmpty()) {
         return;
      }
      if (player.level() instanceof ServerLevel serverLevel) {
         for (UUID targetId : previous) {
            if (serverLevel.getEntity(targetId) instanceof LivingEntity living
                  && living.hasEffect(MobEffects.GLOWING)) {
               living.removeEffect(MobEffects.GLOWING);
            }
         }
      }
   }

   public static void onLivingBreathe(LivingBreatheEvent event) {
      if (event.getEntity() instanceof Player player && player.getItemBySlot(EquipmentSlot.HEAD).is((Item)ModItems.TRANSCENDIUM_HELMET.get())) {
         event.setCanBreathe(true);
         // 一次回满氧气（取代旧的 EntityAirDataAccessor 反射写数据）
         event.setRefillAirAmount(Math.max(event.getRefillAirAmount(), player.getMaxAirSupply()));
      }
   }

   private static boolean isHostileTo(Entity entity, Player player) {
      if (entity instanceof Enemy) {
         return true;
      } else {
         if (entity instanceof Mob mob && mob.getTarget() == player) {
            return true;
         }

         return false;
      }
   }

   public static void toggleGlowingVision(ServerPlayer player) {
      ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
      if (helmet.is((Item)ModItems.TRANSCENDIUM_HELMET.get())) {
         UUID uuid = player.getUUID();
         int nextMode = (ModArmorSetHandler.HELMET_MODE.getOrDefault(uuid, 5) + 1) % 6;
         ModArmorSetHandler.HELMET_MODE.put(uuid, nextMode);
         boolean glowingOn = nextMode == 0 || nextMode == 4;
         GLOWING_VISION_ENABLED.put(uuid, glowingOn);
         if (!glowingOn) {
            clearGlowingTargets(player);
         }
         String[] names = new String[]{"高亮开", "高亮关", "夜视开", "夜视关", "全开", "全关"};
         player.displayClientMessage(ArmorSets.actionMessage("适应", names[nextMode], ModArmorSetHandler.HELMET_MODE_COLORS[nextMode]), true);
         ModArmorSetHandler.saveToggleStates(player);
         ModArmorSetHandler.syncAbilityState(player);
      }
   }

   public static void clearOnLogout(UUID uuid) {
      GLOWING_VISION_ENABLED.remove(uuid);
      GLOWING_TARGETS.remove(uuid);
   }

   public static void save(CompoundTag data, UUID uuid) {
   }

   public static void load(CompoundTag data, UUID uuid) {
   }
}
