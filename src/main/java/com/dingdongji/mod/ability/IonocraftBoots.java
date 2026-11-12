package com.dingdongji.mod.ability;

import com.dingdongji.mod.event.ModArmorSetHandler;
import com.dingdongji.mod.platform.ClientViews;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.network.IonocraftBootsFlyingPacket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class IonocraftBoots {
   public static final int FLIGHT_OFF = 0;
   public static final int FLIGHT_NORMAL = 1;
   public static final int FLIGHT_SUPER = 2;
   public static final Map<UUID, Integer> IONOCRAFT_FLIGHT_MODE = new HashMap<>();
   private static final Map<UUID, Boolean> IONOCRAFT_GRANTED = new HashMap<>();
   private static final float DEFAULT_FLY_SPEED = 0.05F;
   private static final float SUPER_FLY_SPEED = 0.1F;
   private static final ResourceLocation TRANS_FLUID_EFFICIENCY_ID = ResourceLocation.parse("dingdongji:trans_fluid_efficiency");
   private static final ResourceLocation TRANS_MOVEMENT_EFFICIENCY_ID = ResourceLocation.parse("dingdongji:trans_move_efficiency");
   private static final Map<UUID, Integer> IONOCRAFT_FLYING_SYNC = new HashMap<>();
   private static final Map<UUID, Integer> IONOCRAFT_SYNC_LAST_TICK = new HashMap<>();
   private static final int IONOCRAFT_SYNC_INTERVAL = 20;

   public static void handleTranscendiumBootsFlight(Player player) {
      ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
      boolean wearingBoots = boots.is((Item)ModItems.TRANSCENDIUM_BOOTS.get());
      UUID uuid = player.getUUID();
      boolean isCreative = player.isCreative();
      boolean isSpectator = player.isSpectator();
      if (!wearingBoots) {
         boolean granted = IONOCRAFT_GRANTED.remove(uuid) == Boolean.TRUE;
         boolean needUpdate = false;
         if (Math.abs(player.getAbilities().getFlyingSpeed() - 0.05F) > 1.0E-4F && granted) {
            player.getAbilities().setFlyingSpeed(0.05F);
            needUpdate = true;
         }

         if (granted && !isCreative && !isSpectator) {
            if (player.getAbilities().mayfly) {
               player.getAbilities().mayfly = false;
               needUpdate = true;
            }

            if (player.getAbilities().flying) {
               player.getAbilities().flying = false;
               needUpdate = true;
            }
         }

         if (needUpdate) {
            player.onUpdateAbilities();
         }

         syncIonocraftFlyingState(player, 0);
      } else {
         int mode = IONOCRAFT_FLIGHT_MODE.getOrDefault(uuid, 1);
         if (mode == 0) {
            mode = 1;
            IONOCRAFT_FLIGHT_MODE.put(uuid, 1);
         }

         // 穿超限合金靴即授予常驻飞行：mode 1 普通飞行（0.05），mode 2 超速飞行（0.1）。
         // 创造与旁观的飞行是模式自带能力，不受影响。
         boolean strideOn = mode == 2;
         boolean flightAllowed = true;
         boolean needUpdate = false;
         if (player.getAbilities().mayfly != flightAllowed) {
            player.getAbilities().mayfly = flightAllowed;
            needUpdate = true;
         }
         if (!flightAllowed && player.getAbilities().flying) {
            player.getAbilities().flying = false;
            needUpdate = true;
         }

         float targetSpeed = strideOn ? 0.1F : 0.05F;
         if (Math.abs(player.getAbilities().getFlyingSpeed() - targetSpeed) > 1.0E-4F) {
            player.getAbilities().setFlyingSpeed(targetSpeed);
            needUpdate = true;
         }
         if (needUpdate) {
            player.onUpdateAbilities();
         }

         IONOCRAFT_GRANTED.put(uuid, true);
         int broadcastMode = player.getAbilities().flying && !isSpectator ? mode : 0;
         syncIonocraftFlyingState(player, broadcastMode);
      }
   }

   public static void handleTranscendiumFluidEfficiency(Player player) {
      boolean fullSet = ArmorSets.hasFullTranscendiumSet(player);
      AttributeInstance waterEff = player.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
      AttributeInstance moveEff = player.getAttribute(Attributes.MOVEMENT_EFFICIENCY);
      if (waterEff != null && waterEff.getModifier(TRANS_FLUID_EFFICIENCY_ID) != null) {
         waterEff.removeModifier(TRANS_FLUID_EFFICIENCY_ID);
      }
      if (moveEff != null && moveEff.getModifier(TRANS_MOVEMENT_EFFICIENCY_ID) != null) {
         moveEff.removeModifier(TRANS_MOVEMENT_EFFICIENCY_ID);
      }
   }

   private static void syncIonocraftFlyingState(Player player, int mode) {
      if (player instanceof ServerPlayer serverPlayer) {
         UUID uuid = player.getUUID();
         int tick = serverPlayer.getServer() != null ? serverPlayer.getServer().getTickCount() : 0;
         Integer prev = IONOCRAFT_FLYING_SYNC.put(uuid, mode);
         Integer lastTick = IONOCRAFT_SYNC_LAST_TICK.get(uuid);
         if (prev == null || prev != mode) {
            IONOCRAFT_SYNC_LAST_TICK.put(uuid, tick);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(
               serverPlayer, new IonocraftBootsFlyingPacket(serverPlayer.getId(), mode), new CustomPacketPayload[0]
            );
         } else if (mode != 0 && (lastTick == null || tick - lastTick >= 20)) {
            IONOCRAFT_SYNC_LAST_TICK.put(uuid, tick);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(
               serverPlayer, new IonocraftBootsFlyingPacket(serverPlayer.getId(), mode), new CustomPacketPayload[0]
            );
         }
      }
   }

   public static void toggleIonocraftFlight(ServerPlayer player) {
      ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
      if (boots.is((Item)ModItems.TRANSCENDIUM_BOOTS.get())) {
         UUID uuid = player.getUUID();
         int oldMode = IONOCRAFT_FLIGHT_MODE.getOrDefault(uuid, 1);
         int mode = oldMode == 2 ? 1 : 2;
         IONOCRAFT_FLIGHT_MODE.put(uuid, mode);
         boolean strideOn = mode == 2;
         // 穿靴即常驻飞行，切换只改变速度
         if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = true;
         }
         player.getAbilities().setFlyingSpeed(strideOn ? 0.1F : 0.05F);
         player.onUpdateAbilities();
         Component msg = Component.empty()
            .append(Component.literal("蹈虚：").withStyle(ChatFormatting.WHITE))
            .append(Component.literal(strideOn ? "开" : "关").withStyle(strideOn ? ChatFormatting.GREEN : ChatFormatting.RED));
         player.displayClientMessage(msg, true);
         ModArmorSetHandler.saveToggleStates(player);
      }
   }

   /**
    * 相位偏移（穿墙 + 观察者式视觉）的判定：
    * 穿齐全套超限合金套 + 玩家按键开启相位偏移（默认关，状态持久化）。
    * 开启即激活横向穿墙；飞行中升级为全向穿透，非飞行时垂直碰撞保持原版
    * （幻灵 mode1 同款），脚下有方块即可站立，不会坠入地下。
    */
   public static boolean isTranscendiumFlightPhasing(Player player) {
      if (!ArmorSets.hasFullTranscendiumSet(player)) {
         return false;
      }
      return player.level().isClientSide
         ? ClientViews.get().isTranscendiumPhase(player)
         : TranscendiumSet.TRANSCENDIUM_PHASE_ENABLED.getOrDefault(player.getUUID(), false);
   }

   public static void clearOnLogout(UUID uuid) {
      IONOCRAFT_FLIGHT_MODE.remove(uuid);
      IONOCRAFT_GRANTED.remove(uuid);
      IONOCRAFT_FLYING_SYNC.remove(uuid);
      IONOCRAFT_SYNC_LAST_TICK.remove(uuid);
   }

   public static void save(CompoundTag data, UUID uuid) {
      data.putInt("dingdongji:ionocraft_mode", IONOCRAFT_FLIGHT_MODE.getOrDefault(uuid, 1));
   }

   public static void load(CompoundTag data, UUID uuid) {
      int flightMode;
      if (data.contains("dingdongji:ionocraft_mode")) {
         flightMode = data.getInt("dingdongji:ionocraft_mode");
      } else if (data.contains("dingdongji:ionocraft_flying")) {
         flightMode = data.getBoolean("dingdongji:ionocraft_flying") ? 1 : 0;
      } else {
         flightMode = 0;
      }

      IONOCRAFT_FLIGHT_MODE.put(uuid, flightMode == 2 ? 2 : 1);
   }
}
