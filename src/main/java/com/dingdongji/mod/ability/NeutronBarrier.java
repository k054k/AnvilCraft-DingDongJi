package com.dingdongji.mod.ability;

import com.dingdongji.mod.event.ModArmorSetHandler;
import com.dingdongji.mod.init.ModParticles;
import com.dingdongji.mod.item.ModItems;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class NeutronBarrier {
   public static final Map<UUID, Integer> NEUTRON_BARRIER_ENABLED = new HashMap<>();
   private static final Map<UUID, Long> BOSS_PROXIMITY_START = new HashMap<>();
   private static final Map<UUID, Long> BOSS_REPEL_COOLDOWN = new HashMap<>();
   private static final long PROXIMITY_THRESHOLD = 60L;
   private static final long REPEL_COOLDOWN = 100L;
   private static final Map<UUID, Long> REPEL_PARTICLE_COOLDOWN = new HashMap<>();

   public static void toggleNeutronBarrier(ServerPlayer player) {
      ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
      if (chestplate.is((Item)ModItems.TRANSCENDIUM_CHESTPLATE.get())) {
         UUID uuid = player.getUUID();
         int state = NEUTRON_BARRIER_ENABLED.getOrDefault(uuid, 0);
         state = (state + 1) % 4;
         NEUTRON_BARRIER_ENABLED.put(uuid, state);
         switch (state) {
            case 0:
               player.displayClientMessage(ArmorSets.actionMessage("中子力场", "关闭屏蔽", ChatFormatting.BLUE), true);
               break;
            case 1:
               player.displayClientMessage(ArmorSets.actionMessage("中子力场", "屏蔽敌对生物", ChatFormatting.GREEN), true);
               break;
            case 2:
               player.displayClientMessage(ArmorSets.actionMessage("中子力场", "屏蔽弹射物", ChatFormatting.AQUA), true);
               break;
            case 3:
               player.displayClientMessage(ArmorSets.actionMessage("中子力场", "全部屏蔽", ChatFormatting.LIGHT_PURPLE), true);
         }

         ModArmorSetHandler.saveToggleStates(player);
      }
   }

   public static void handleTranscendiumReflect(Player player) {
      // 旁观者附身其他实体时，服务端会把旁观者实体传送到摄像机目标坐标，
      // 若照常跑屏障检测，会在被观察者身边吞弹幕、击退怪物并把粒子播在
      // 摄像机（玩家自己）位置。旁观者不参与战斗，直接跳过整套屏障逻辑。
      if (player.isSpectator()) {
         return;
      }
      ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
      if (chestplate.is((Item)ModItems.TRANSCENDIUM_CHESTPLATE.get())) {
         Level level = player.level();
         Vec3 center = player.position();
         double shieldRadius = 3.0;
         long gameTime = level.getGameTime();
         int __bs = NEUTRON_BARRIER_ENABLED.getOrDefault(player.getUUID(), 0);
         if (__bs == 2 || __bs == 3) {
            for (Projectile projectile : level.getEntitiesOfClass(
               Projectile.class,
               player.getBoundingBox().inflate(shieldRadius),
               p -> p.isAlive() && p.distanceToSqr(center) <= shieldRadius * shieldRadius && isHarmfulProjectile(p, player)
            )) {
               spawnAbsorptionRipple(level, projectile.position(), player);
               projectile.discard();
            }
         }

         int __state = NEUTRON_BARRIER_ENABLED.getOrDefault(player.getUUID(), 0);
         if (__state == 1 || __state == 3) {
            AABB range = player.getBoundingBox().inflate(2.0, 2.0, 2.0).expandTowards(0.0, -3.0, 0.0);

            for (Entity threat : level.getEntities(
               player, range, e -> e != player && e.isAlive() && (e instanceof Enemy || e instanceof Mob mob && mob.getTarget() == player)
            )) {
               UUID tid = threat.getUUID();
               Long cooldownEnd = BOSS_REPEL_COOLDOWN.get(tid);
               if (cooldownEnd == null || gameTime >= cooldownEnd) {
                  Vec3 knockDir = threat.position().subtract(player.position()).normalize();
                  threat.push(knockDir.x * 1.5, 0.1, knockDir.z * 1.5);
                  threat.hurtMarked = true;
                  spawnRepelBurst(player, threat, knockDir, gameTime, false);
               }
            }

            BOSS_PROXIMITY_START.keySet().removeIf(id -> {
               Entity e = ((ServerLevel)level).getEntity(id);
               return e == null || !e.isAlive() || !range.contains(e.position());
            });
            BOSS_REPEL_COOLDOWN.keySet().removeIf(id -> {
               Entity e = ((ServerLevel)level).getEntity(id);
               return e == null || !e.isAlive();
            });
         }
      }
   }

   private static boolean isHarmfulProjectile(Projectile projectile, Player player) {
      Entity owner = projectile.getOwner();
      if (owner == player) {
         return false;
      } else if (owner == null) {
         Vec3 vel = projectile.getDeltaMovement();
         if (vel.lengthSqr() < 0.01) {
            return false;
         } else {
            Vec3 toPlayer = player.position().subtract(projectile.position()).normalize();
            Vec3 velNorm = vel.normalize();
            return velNorm.dot(toPlayer) > 0.5;
         }
      } else if (owner instanceof Enemy) {
         return true;
      } else {
         if (owner instanceof Mob mob && mob.getTarget() == player) {
            return true;
         }

         return owner instanceof Player && owner != player ? projectile.getClass().getName().startsWith("net.minecraft.") : false;
      }
   }

   private static boolean isBoss(LivingEntity entity) {
      return (double)entity.getMaxHealth() >= 150.0;
   }

   private static void spawnAbsorptionRipple(Level level, Vec3 pos, Player player) {
      if (level instanceof ServerLevel serverLevel) {
         serverLevel.sendParticles(ModParticles.NEUTRON_BARRIER_ABSORB.get(), pos.x, pos.y + 0.5, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
      }
   }

   private static void spawnRepelBurst(Player player, Entity threat, Vec3 knockDir, long gameTime, boolean big) {
      if (player.level() instanceof ServerLevel serverLevel) {
         UUID var11 = player.getUUID();
         Long last = REPEL_PARTICLE_COOLDOWN.get(var11);
         if (last == null || gameTime - last >= 20L) {
            REPEL_PARTICLE_COOLDOWN.put(var11, gameTime);
            SimpleParticleType type = big ? ModParticles.NEUTRON_BARRIER_REPEL_BIG.get() : ModParticles.NEUTRON_BARRIER_REPEL.get();
            Vec3 chest = player.position().add(0.0, 1.5, 0.0);
            serverLevel.sendParticles(type, chest.x, chest.y, chest.z, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }
   }

   public static void clearOnLogout(UUID uuid) {
      NEUTRON_BARRIER_ENABLED.remove(uuid);
      REPEL_PARTICLE_COOLDOWN.remove(uuid);
      BOSS_PROXIMITY_START.remove(uuid);
      BOSS_REPEL_COOLDOWN.remove(uuid);
   }

   public static void save(CompoundTag data, UUID uuid) {
      data.putInt("dingdongji:neutron_barrier", NEUTRON_BARRIER_ENABLED.getOrDefault(uuid, 0));
   }

   public static void load(CompoundTag data, UUID uuid) {
      NEUTRON_BARRIER_ENABLED.put(uuid, data.getInt("dingdongji:neutron_barrier"));
   }
}
