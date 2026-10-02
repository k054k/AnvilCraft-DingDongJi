package com.dingdongji.mod.client;

import com.dingdongji.mod.init.ModParticles;
import com.dingdongji.mod.item.ModItems;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

@EventBusSubscriber({Dist.CLIENT})
public class IonocraftBootsClientHandler {
   private static final Map<Integer, Integer> SYNCED_FLIGHT_MODE = new ConcurrentHashMap<>();
   private static ClientLevel trackedLevel;
   private static final int DOUBLE_TAP_WINDOW = 7;
   private static boolean forwardKeyWasDown = false;
   private static int lastForwardPressTick = -1073741824;

   public static void onFlyingSync(int playerId, int mode) {
      if (mode != 0) {
         SYNCED_FLIGHT_MODE.put(playerId, mode);
      } else {
         SYNCED_FLIGHT_MODE.remove(playerId);
      }
   }

   public static int getSyncedMode(int playerId) {
      return SYNCED_FLIGHT_MODE.getOrDefault(playerId, 0);
   }

   @SubscribeEvent
   public static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && !mc.isPaused()) {
         ClientLevel level = mc.level;
         LocalPlayer local = mc.player;
         if (level != trackedLevel) {
            SYNCED_FLIGHT_MODE.clear();
            forwardKeyWasDown = false;
            lastForwardPressTick = -1073741824;
            trackedLevel = level;
         }

         if (local == null) {
            forwardKeyWasDown = false;
         } else {
            handleAirSprint(mc, local);
            if ((level.getGameTime() & 1L) != 1L) {
               boolean firstPerson = mc.options.getCameraType() == CameraType.FIRST_PERSON;

               for (Player player : level.players()) {
                  if (!player.isCreative() && !player.isSpectator() && (player != local || !firstPerson)) {
                     int mode = player == local
                        ? (isWearingTranscendiumBoots(player) && player.getAbilities().flying ? Math.max(1, getSyncedMode(player.getId())) : 0)
                        : getSyncedMode(player.getId());
                     if (mode != 0) {
                        spawnBootsParticles(level, player, level.random);
                     }
                  }
               }
            }
         }
      }
   }

   private static void handleAirSprint(Minecraft mc, LocalPlayer local) {
      boolean forwardDown = mc.options.keyUp.isDown();
      int now = local.tickCount;
      boolean doubleTap = false;
      if (forwardDown && !forwardKeyWasDown) {
         if (now - lastForwardPressTick <= 7) {
            doubleTap = true;
         }

         lastForwardPressTick = now;
      }

      forwardKeyWasDown = forwardDown;
      // Deliberately do NOT gate on isCreative: creative players need the
      // dash-sprint too since their normal sprint is also suppressed in flight.
      if (isWearingTranscendiumBoots(local) && local.getAbilities().flying && !local.isSpectator()) {
         boolean wantSprint = mc.options.keySprint.isDown() || doubleTap;
         if (wantSprint
            && !local.isSprinting()
            && local.input.hasForwardImpulse()
            && !local.isUsingItem()
            && !local.hasEffect(MobEffects.BLINDNESS)
            && !local.isFallFlying()) {
            local.setSprinting(true);
         }
      }
   }

   private static boolean isWearingTranscendiumBoots(Player player) {
      ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
      return boots.is((Item)ModItems.TRANSCENDIUM_BOOTS.get());
   }

   private static void spawnBootsParticles(ClientLevel level, Player player, RandomSource random) {
      float yawRad = (float)Math.toRadians((double)player.yBodyRot);
      double cosYaw = Math.cos((double)yawRad);
      double sinYaw = Math.sin((double)yawRad);
      float limbPos = 0.0F;
      float limbSpeed = 0.0F;

      try {
         limbPos = player.walkAnimation.position();
         limbSpeed = player.walkAnimation.speed();
      } catch (Exception var31) {
      }

      double legSwing = (double)(Mth.cos(limbPos * 0.6662F) * 1.4F * limbSpeed);
      double footLift = (double)(Math.abs(Mth.sin(limbPos * 0.6662F)) * 1.4F * limbSpeed) * 0.15;
      double side = 0.12;
      double fwdK = 0.45;
      double footY = player.getY() + 0.05;
      double[][] feet = new double[][]{{-side, -legSwing * fwdK, footLift}, {side, legSwing * fwdK, footLift}};

      for (double[] f : feet) {
         double worldX = player.getX() + f[0] * cosYaw - f[1] * sinYaw;
         double worldZ = player.getZ() + f[0] * sinYaw + f[1] * cosYaw;
         double worldY = footY + f[2];
         level.addParticle(
            (ParticleOptions)ModParticles.IONOCRAFT_BOOTS_EXHAUST.get(),
            true,
            worldX + random.nextGaussian() * 0.02,
            worldY + random.nextGaussian() * 0.02,
            worldZ + random.nextGaussian() * 0.02,
            0.0,
            -0.22 - (double)random.nextFloat() * 0.1,
            0.0
         );
      }
   }
}
