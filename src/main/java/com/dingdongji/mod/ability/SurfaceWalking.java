package com.dingdongji.mod.ability;

import com.dingdongji.mod.event.ModArmorSetHandler;
import com.dingdongji.mod.platform.ClientViews;
import com.dingdongji.mod.item.ModItems;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public class SurfaceWalking {
   public static final Map<UUID, Boolean> LAVA_WALKER_ENABLED = new HashMap<>();
   public static final Map<UUID, Boolean> FROST_SLIDE_ENABLED = new HashMap<>();
   // 玩家行走/潜行判定在客户端与服务端线程各自执行（单人游戏双线程并发），
   // 必须使用并发集合，避免普通 HashSet 并发读写导致结构损坏
   public static final Set<UUID> FLUID_SWIM = java.util.concurrent.ConcurrentHashMap.newKeySet();

   /**
    * 蹈火按键：仅控制脚下生火特效（LAVA_WALKER_ENABLED 复用为火焰开关，
    * 默认开启）。熔岩液面行走为穿靴自带能力，不响应该开关。
    */
   public static void toggleLavaWalker(ServerPlayer player) {
      if (player.getItemBySlot(EquipmentSlot.FEET).is((Item)ModItems.EMBER_METAL_BOOTS.get())) {
         UUID uuid = player.getUUID();
         boolean enabled = !LAVA_WALKER_ENABLED.getOrDefault(uuid, false);
         LAVA_WALKER_ENABLED.put(uuid, enabled);
         Component msg = ArmorSets.actionMessage("蹈火", enabled ? "开" : "关", enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
         player.displayClientMessage(msg, true);
         ModArmorSetHandler.saveToggleStates(player);
         if (player instanceof ServerPlayer) {
            ModArmorSetHandler.syncAbilityState(player);
         }
      }
   }

   public static boolean isLavaWalkerEnabled(Player player) {
      return player.level().isClientSide ? ClientViews.get().isLavaWalker(player) : LAVA_WALKER_ENABLED.getOrDefault(player.getUUID(), false);
   }

   public static void toggleFrostSlide(ServerPlayer player) {
      ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
      if (boots.is((Item)ModItems.FROST_METAL_BOOTS.get())) {
         UUID uuid = player.getUUID();
         boolean enabled = !FROST_SLIDE_ENABLED.getOrDefault(uuid, false);
         FROST_SLIDE_ENABLED.put(uuid, enabled);
         Component msg = ArmorSets.actionMessage("凌霜", enabled ? "开" : "关", enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
         player.displayClientMessage(msg, true);
         ModArmorSetHandler.saveToggleStates(player);
         ModArmorSetHandler.syncAbilityState(player);
      }
   }

   public static boolean isFrostSlideEnabled(Player player) {
      return player.level().isClientSide ? ClientViews.get().isFrostSlide(player) : FROST_SLIDE_ENABLED.getOrDefault(player.getUUID(), false);
   }

   public static boolean isFluidSwimming(Entity entity) {
      if (entity instanceof Player player && FLUID_SWIM.contains(player.getUUID())) {
         return true;
      }

      return false;
   }

   /**
    * 流体行走：
    * 超限靴走"液面钳位"方案（handleFluidSurface + move TAIL 的
    * landOnWalkableFluid），任意流体可行走、潜行下沉；
    * 余烬靴（蹈火开启）同样接入钳位方案但仅限熔岩；
    * 全身浸没时双方都交还原版流体游泳物理。
    */
   public static void handleSurfaceWalking(Player player) {
      ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
      boolean transBoots = boots.is((Item)ModItems.TRANSCENDIUM_BOOTS.get());
      boolean emberBoots = boots.is((Item)ModItems.EMBER_METAL_BOOTS.get());
      UUID uuid = player.getUUID();
      if (!transBoots && !emberBoots) {
         FLUID_SWIM.remove(uuid);
         return;
      }
      Level level = player.level();
      BlockPos feetPos = player.blockPosition();
      if (transBoots) {
         // 超限靴：任意流体，旧版钳位逻辑
         boolean fluidAtFeet = isWalkableFluid(level.getFluidState(feetPos), true);
         boolean fluidBelow = isWalkableFluid(level.getFluidState(feetPos.below()), true);
         if (!fluidAtFeet && !fluidBelow) {
            FLUID_SWIM.remove(uuid);
            return;
         }
         if (FLUID_SWIM.contains(uuid)) {
            double surfaceY = findFluidSurfaceY(level, feetPos, true);
            if (Double.isNaN(surfaceY) || !(player.getY() >= surfaceY - 0.05) || !(player.getDeltaMovement().y > 0.0)) {
               player.fallDistance = 0.0F;
               return;
            }

            FLUID_SWIM.remove(uuid);
         }

         if (player.isShiftKeyDown() && fluidAtFeet) {
            FLUID_SWIM.add(uuid);
            player.setOnGround(false);
            player.fallDistance = 0.0F;
         } else {
            handleFluidSurface(player, true);
         }
         return;
      }
      // 余烬靴：仅熔岩，与超限靴同款液面钳位；熔岩行走为穿靴自带能力，无需开关
      if (player.isShiftKeyDown()) {
         FluidState feetFluid = level.getFluidState(feetPos);
         if (feetFluid.is(FluidTags.LAVA)) {
            FLUID_SWIM.add(uuid);
            player.setOnGround(false);
            player.fallDistance = 0.0F;
            return;
         }
      }
      boolean lavaAtFeet = isWalkableFluid(level.getFluidState(feetPos), false);
      boolean lavaBelow = isWalkableFluid(level.getFluidState(feetPos.below()), false);
      if (!lavaAtFeet && !lavaBelow) {
         FLUID_SWIM.remove(uuid);
         return;
      }
      if (FLUID_SWIM.contains(uuid)) {
         // 潜行下沉后停止潜行：回到液面之前仍交给原版熔岩物理，避免瞬移
         double recoveryY = findFluidSurfaceY(level, feetPos, false);
         if (Double.isNaN(recoveryY) || !(player.getY() >= recoveryY - 0.05) || !(player.getDeltaMovement().y > 0.0)) {
            player.fallDistance = 0.0F;
            return;
         }
         FLUID_SWIM.remove(uuid);
      }
      handleFluidSurface(player, false);
      return;
   }

   private static boolean isWalkableFluid(FluidState state, boolean walkAny) {
      if (state == null || state.isEmpty()) {
         return false;
      } else {
         return walkAny || state.is(FluidTags.LAVA);
      }
   }

   private static double findFluidSurfaceY(Level level, BlockPos feetPos, boolean walkAny) {
      FluidState here = level.getFluidState(feetPos);
      FluidState below = level.getFluidState(feetPos.below());
      double surfaceY = Double.NaN;
      if (isWalkableFluid(here, walkAny)) {
         BlockPos top = feetPos;
         FluidState topState = here;

         for (int i = 0; i < 8; i++) {
            BlockPos up = top.above();
            FluidState upState = level.getFluidState(up);
            if (!isWalkableFluid(upState, walkAny)) {
               break;
            }

            top = up;
            topState = upState;
         }

         surfaceY = (double)((float)top.getY() + topState.getHeight(level, top));
      }

      if (isWalkableFluid(below, walkAny)) {
         double belowSurface = (double)((float)feetPos.below().getY() + below.getHeight(level, feetPos.below()));
         if (Double.isNaN(surfaceY) || belowSurface > surfaceY) {
            surfaceY = belowSurface;
         }
      }

      return surfaceY;
   }

   /**
    * 由 Entity#move TAIL 调用：下落穿过可行走流体液面的瞬间把脚精确
    * 钳在液面上，和固体方块碰撞一致，消除一帧下沉的视觉抖动。
    * 仅超限靴（任意流体）生效。
    */
   public static boolean landOnWalkableFluid(Player player) {
      ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
      boolean transBoots = boots.is((Item)ModItems.TRANSCENDIUM_BOOTS.get());
      // 余烬靴同样接液面钳位（穿靴自带），但仅限熔岩
      boolean emberWalker = boots.is((Item)ModItems.EMBER_METAL_BOOTS.get());
      if (!transBoots && !emberWalker) {
         return false;
      } else if (isFluidSwimming(player) || player.isShiftKeyDown() || player.isFallFlying()) {
         // 潜行下沉或滑翔：放行穿过液面
         return false;
      } else {
         Vec3 mot = player.getDeltaMovement();
         if (mot.y > 0.0) {
            return false;
         } else {
            Level level = player.level();
            BlockPos feetPos = player.blockPosition();
            boolean walkAny = transBoots;
            boolean fluidAtFeet = isWalkableFluid(level.getFluidState(feetPos), walkAny);
            boolean fluidBelow = isWalkableFluid(level.getFluidState(feetPos.below()), walkAny);
            if (!fluidAtFeet && !fluidBelow) {
               return false;
            } else if (isWalkableFluid(level.getFluidState(BlockPos.containing(player.getEyePosition())), walkAny)) {
               // 全身浸没：保留原版游泳上浮路径，不瞬移到液面
               return false;
            } else {
               double surfaceY = findFluidSurfaceY(level, feetPos, walkAny);
               if (Double.isNaN(surfaceY)) {
                  return false;
               } else {
                  double feetY = player.getY();
                  if (feetY <= surfaceY + 0.1 && feetY >= surfaceY - 0.6) {
                     player.setPos(player.getX(), surfaceY, player.getZ());
                     player.setDeltaMovement(mot.x, 0.0, mot.z);
                     player.setOnGround(true);
                     player.fallDistance = 0.0F;
                     return true;
                  } else {
                     return false;
                  }
               }
            }
         }
      }
   }

   private static void handleFluidSurface(Player player, boolean walkAny) {
      Level level = player.level();
      BlockPos feetPos = player.blockPosition();
      double surfaceY = findFluidSurfaceY(level, feetPos, walkAny);
      if (!Double.isNaN(surfaceY)) {
         boolean eyeInFluid = isWalkableFluid(level.getFluidState(BlockPos.containing(player.getEyePosition())), walkAny);
         if (eyeInFluid) {
            // 全身浸没时交还原版游泳物理；仅脚/眼之间没过一半时强制上浮
            boolean fullySubmerged = isWalkableFluid(level.getFluidState(feetPos), walkAny);
            Vec3 mot = player.getDeltaMovement();
            if (!fullySubmerged) {
               player.setDeltaMovement(mot.x * 0.5, Math.max(mot.y, 0.12), mot.z * 0.5);
            }
         } else {
            double feetY = player.getY();
            double dy = feetY - surfaceY;
            Vec3 mot = player.getDeltaMovement();
            if (dy <= 0.1) {
               if (dy < 0.0 || mot.y < 0.0) {
                  player.setPos(player.getX(), surfaceY, player.getZ());
                  player.setDeltaMovement(mot.x, 0.0, mot.z);
               }

               player.setOnGround(true);
               player.fallDistance = 0.0F;
            }

            player.fallDistance = 0.0F;
         }
      }
   }

   public static void clearOnLogout(UUID uuid) {
      LAVA_WALKER_ENABLED.remove(uuid);
      FROST_SLIDE_ENABLED.remove(uuid);
      FLUID_SWIM.remove(uuid);
   }

   public static void save(CompoundTag data, UUID uuid) {
      data.putBoolean("dingdongji:lava_walker", LAVA_WALKER_ENABLED.getOrDefault(uuid, false));
      data.putBoolean("dingdongji:frost_slide", FROST_SLIDE_ENABLED.getOrDefault(uuid, false));
   }

   public static void load(CompoundTag data, UUID uuid) {
      LAVA_WALKER_ENABLED.put(uuid, data.getBoolean("dingdongji:lava_walker"));
      FROST_SLIDE_ENABLED.put(uuid, data.getBoolean("dingdongji:frost_slide"));
   }
}
