package com.dingdongji.mod.ability;

import com.dingdongji.mod.accessor.LivingEntityJumpingAccessor;
import com.dingdongji.mod.event.ModArmorSetHandler;
import com.dingdongji.mod.platform.ClientViews;
import com.dingdongji.mod.event.PhantomDecoyManager;
import com.dingdongji.mod.item.ModItems;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SpectralPhase {
   public static final Map<UUID, Boolean> SPECTRAL_PHASE_ENABLED = new HashMap<>();

   /**
    * 0 = no phase, 1 = horizontal free (phantom), 2 = vertical also
    * free (phantom + boots toggle on). 穿墙能力收进幻影模式：非幻影状态
    * 一律返回 0（无事发生，不能穿墙）。Called from the collide mixin on both
    * sides every movement, so it must stay allocation-free.
    */
   public static int spectralPhaseMode(Player player) {
      if (!ArmorSets.hasFullSpectralSet(player)) {
         return 0;
      }
      if (!PhantomDecoyManager.isPhantomState(player)) {
         return 0;
      }

      boolean vertical = player.level().isClientSide
         ? ClientViews.get().isPhaseVertical(player)
         : SPECTRAL_PHASE_ENABLED.getOrDefault(player.getUUID(), false);
      return vertical ? 2 : 1;
   }

   /**
    * 统一的"墙体相位"门控：玩家全身处于方块内部时，无视方块碰撞、
    * 无视自身碰撞箱放置方块、豁免墙内挖掘/窒息等判定，仅在以下状态生效——
    * 幻灵幻影中且虚化开启（mode 2），或超限相位偏移开启。
    * 仅幻影而未开启虚化（mode 1）的横向穿墙由 isHorizontalPhaseActive 门控，
    * 墙内窒息/挖掘/姿势豁免同样走横向门控。
    */
   public static boolean isWallPhaseActive(Player player) {
      return spectralPhaseMode(player) == 2 || IonocraftBoots.isTranscendiumFlightPhasing(player);
   }

   /**
    * 横向穿墙门控：幻灵幻影开启（mode>=1，含虚化）或超限相位偏移开启时，
    * noPhysics 置 true，水平方向无视方块碰撞；垂直方向由 EntityMixin 的
    * state 1/2 分支分别结算（mode 1 保持原版垂直碰撞，mode 2 全穿透）。
    */
   public static boolean isHorizontalPhaseActive(Player player) {
      return spectralPhaseMode(player) >= 1 || IonocraftBoots.isTranscendiumFlightPhasing(player);
   }

   public static void togglePhaseVertical(ServerPlayer player) {
      ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
      if (boots.is((Item)ModItems.SPECTRAL_BOOTS.get())) {
         UUID uuid = player.getUUID();
         // 非幻影状态（替身未激活）时虚化键完全无响应：不弹标题、不切换状态
         if (!PhantomDecoyManager.isPhantomState(player)) {
            return;
         }
         boolean enabled = !SPECTRAL_PHASE_ENABLED.getOrDefault(uuid, false);
         if (enabled && !ArmorSets.hasFullSpectralSet(player)) {
            player.displayClientMessage(Component.literal("虚化需要穿戴全套幻灵盔甲").withStyle(ChatFormatting.RED), true);
            return;
         }

         SPECTRAL_PHASE_ENABLED.put(uuid, enabled);
         player.displayClientMessage(ArmorSets.actionMessage("虚化", enabled ? "开" : "关", enabled ? ChatFormatting.GREEN : ChatFormatting.RED), true);
         ModArmorSetHandler.saveToggleStates(player);
         ModArmorSetHandler.syncAbilityState(player);
      }
   }

   public static void handleSpectralPhase(Player player) {
      // 开关状态只由玩家按 toggle 键改变，脱掉装备不清除（与蹈火/凌霜等
      // 靴子功能一致），退出/登录经 saveToggleStates/loadToggleStates 持久化。
      // 未穿全套时 spectralPhaseMode 返回 0，功能暂停；穿回后自动恢复。
      if (isVerticalPhaseActive(player)) {
         // mode 2 且（身体在方块内，或触底按 shift 开始下潜，流体中同样适用）：
         // 无重力 + 垂直按键控制（与客户端 SpectralPhaseClientHandler 同逻辑）
         player.setNoGravity(true);
         player.fallDistance = 0.0F;
         boolean jumpDown = ((LivingEntityJumpingAccessor) player).ddj$isJumping();
         applyVerticalPhase(player, jumpDown, player.isShiftKeyDown());
      } else {
         player.setNoGravity(false);
      }
   }

   /**
    * 虚化垂直穿透是否激活（服务端事件与 move Mixin 共用同一判定）：
    *   mode 2（水/岩浆/模组流体中同样适用：触底下穿是主动行为，不与
    *   mode 1 的"水中脚触底不陷入"冲突），且满足以下之一：
    *     - 身体当前处于方块内部（垂直穿移中）；
    *     - 玩家触底（脚下方块有碰撞形状）按住潜行键（开始下潜）。
    * 不满足时 y 分量做原版碰撞（mode 1 / mode 2 未激活：地表正常跑跳、
    * 流体中正常游泳、脚触底停在方块顶）。
    */
   public static boolean isVerticalPhaseActive(Player player) {
      if (spectralPhaseMode(player) != 2) {
         return false;
      }
      if (isBodyClippingBlock(player)) {
         return true;
      }
      return player.isShiftKeyDown() && hasCollisionBelow(player);
   }

   /**
    * 实体身体 AABB（收缩 1e-4 排除面/线相切）是否与任何方块碰撞形状实质
    * 相交。基于 Level#getBlockCollisions，对固体方块、树叶、栅栏等所有有
    * 碰撞形状的方块均有效。客户端/服务端通用。
    */
   public static boolean isBodyClippingBlock(net.minecraft.world.entity.Entity entity) {
      AABB inner = entity.getBoundingBox().deflate(1.0E-4);
      return entity.level().getBlockCollisions(entity, inner).iterator().hasNext();
   }

   /**
    * mode 2 虚化且身体处于方块内部时的垂直移动结算（双端同逻辑）。
    *   跳跃键：向上 0.2/帧，若本帧会越过身体内最高方块顶面，则直接吸附
    *           到该顶面（脚底站在方块顶上）并停止；
    *   潜行键：脚底（下方 0.05）有方块碰撞时向下 0.15/帧；
    *   其余：悬停（dy=0）。
    *
    * 服务端在 PlayerTickEvent.Pre 调用（本帧 move 前生效）；客户端在
    * ClientTickEvent.Post 调用（设置下一帧速度，setPos 吸附修正本帧）。
    */
   public static void applyVerticalPhase(Player player, boolean jumpDown, boolean sneakDown) {
      AABB box = player.getBoundingBox();
      double dy = 0.0;
      if (jumpDown && sneakDown) {
         // 跳跃+潜行同按：悬停（dy 保持 0），方便在方块内停留观察/放置方块
      } else if (jumpDown) {
         double highestTop = highestClippingBlockTop(player, box.deflate(1.0E-4));
         if (highestTop != Double.NEGATIVE_INFINITY) {
            if (player.getY() + 0.2 >= highestTop) {
               player.setPos(player.getX(), highestTop, player.getZ());
               dy = 0.0;
            } else {
               dy = 0.2;
            }
         }
      } else if (sneakDown && hasCollisionBelow(player)) {
         dy = -0.15;
      }

      Vec3 dm = player.getDeltaMovement();
      player.setDeltaMovement(dm.x, dy, dm.z);
   }

   /** 身体 AABB 内所有碰撞形状中的最高顶面 Y。 */
   private static double highestClippingBlockTop(Player player, AABB inner) {
      double top = Double.NEGATIVE_INFINITY;
      for (VoxelShape shape : player.level().getBlockCollisions(player, inner)) {
         if (!shape.isEmpty()) {
            top = Math.max(top, shape.max(Direction.Axis.Y));
         }
      }
      return top;
   }

   /** 脚底下方 0.05 处的方块是否有碰撞形状。 */
   private static boolean hasCollisionBelow(Player player) {
      AABB probe = new AABB(
         player.getX() - 0.1, player.getY() - 0.05, player.getZ() - 0.1,
         player.getX() + 0.1, player.getY(), player.getZ() + 0.1
      );
      return player.level().getBlockCollisions(player, probe).iterator().hasNext();
   }

   public static void clearOnLogout(UUID uuid) {
      SPECTRAL_PHASE_ENABLED.remove(uuid);
   }

   public static void save(CompoundTag data, UUID uuid) {
      data.putBoolean("dingdongji:spectral_phase", SPECTRAL_PHASE_ENABLED.getOrDefault(uuid, false));
   }

   public static void load(CompoundTag data, UUID uuid) {
      SPECTRAL_PHASE_ENABLED.put(uuid, data.getBoolean("dingdongji:spectral_phase"));
   }
}
