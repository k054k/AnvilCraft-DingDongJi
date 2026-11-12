package com.dingdongji.mod.event;

import com.dingdongji.mod.item.ModItems;
import dev.dubhe.anvilcraft.api.power.DynamicPowerComponent;
import dev.dubhe.anvilcraft.api.power.IDynamicPowerComponentHolder;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.entity.CreativeGeneratorBlockEntity;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 蹲起式发电器服务端逻辑（原 KubeJS trinket_squat_generator.js 的 Java 移植）。
 *
 * 每完成一次"蹲下→站起"翻转记一次有效蹲起；最近 5 秒（100 tick）滑动窗口内的
 * 翻转次数按功率曲线线性插值决定发电功率，封顶 120 kW。防宏：相邻翻转 < 3 tick 丢弃。
 *
 * 发电注入：向玩家所在铁砧 PowerGrid 挂载一个 CreativeGeneratorBlockEntity 实例
 * （真实实现 IPowerProducer），setPower 实时功率；脱腿/功率归零/离开电网即卸载。
 *
 * 蹲下状态优先取客户端上报（创造飞行时服务端 isCrouching 不同步），否则读服务端实体。
 */
public final class SquatGeneratorManager {
   private static final int WINDOW = 100;        // 滑动窗口（tick）
   private static final int MIN_INTERVAL = 3;    // 防宏：相邻翻转最小间隔
   private static final int MAX_POWER = 120;     // 功率封顶（kW）
   private static final int PROBE_INTERVAL = 10; // 功率计算 + 电网探测间隔（tick）
   private static final long FX_INTERVAL = 40;   // 末影烛粒子节流（tick）

   /** 功率曲线：窗口内翻转次数 → kW（相邻档位线性插值）。 */
   private static final int[][] CURVE = {
      {0, 0}, {2, 10}, {4, 24}, {6, 40}, {8, 56}, {10, 70},
      {12, 82}, {14, 92}, {16, 102}, {18, 112}, {20, MAX_POWER}
   };

   private static final class Session {
      boolean prevCrouch;
      long lastFlip = -MIN_INTERVAL;
      final ArrayDeque<Long> flips = new ArrayDeque<>();
      CreativeGeneratorBlockEntity producer;
      PowerGrid mountedGrid;
      long lastFx = -FX_INTERVAL;
   }

   private static final Map<UUID, Session> SESSIONS = new HashMap<>();
   /** 客户端上报的蹲下状态（创造飞行时服务端读不到，以客户端为准）。 */
   private static final Map<UUID, Boolean> CLIENT_CROUCH = new HashMap<>();

   private SquatGeneratorManager() {
   }

   public static void onClientCrouch(UUID uuid, boolean crouching) {
      CLIENT_CROUCH.put(uuid, crouching);
   }

   /** 玩家下线：卸载虚拟发电机并清理状态。 */
   public static void onLogout(ServerPlayer player) {
      Session s = SESSIONS.remove(player.getUUID());
      if (s != null) {
         unmount(s);
      }
      CLIENT_CROUCH.remove(player.getUUID());
   }

   public static void serverTick(ServerPlayer player) {
      UUID uuid = player.getUUID();
      boolean wearing = player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.SQUAT_GENERATOR_LEGGINGS.get());
      Session s = SESSIONS.get(uuid);
      if (!wearing) {
         if (s != null) {
            unmount(s);
            SESSIONS.remove(uuid);
         }
         return;
      }
      if (s == null) {
         s = new Session();
         SESSIONS.put(uuid, s);
      }

      // 蹲起翻转检测（每 tick）
      boolean crouching = CLIENT_CROUCH.getOrDefault(uuid, player.isCrouching());
      long now = player.level().getGameTime();
      if (crouching != s.prevCrouch) {
         s.prevCrouch = crouching;
         if (now - s.lastFlip >= MIN_INTERVAL) {
            s.lastFlip = now;
            s.flips.addLast(now);
            spawnFx(player, s, now);
         }
      }

      // 功率 + 电网（每 PROBE_INTERVAL tick）
      if (player.tickCount % PROBE_INTERVAL != 0) {
         return;
      }
      while (!s.flips.isEmpty() && now - s.flips.peekFirst() > WINDOW) {
         s.flips.pollFirst();
      }
      int power = computePower(s.flips.size());

      PowerGrid grid = null;
      IDynamicPowerComponentHolder holder = IDynamicPowerComponentHolder.of(player);
      if (holder != null) {
         DynamicPowerComponent comp = holder.anvilcraft$getPowerComponent();
         if (comp != null) {
            grid = comp.getPowerGrid();
         }
      }

      if (power <= 0 || grid == null) {
         unmount(s);
         return;
      }
      CreativeGeneratorBlockEntity producer = ensureProducer(s, player);
      if (producer == null) {
         return;
      }
      if (s.mountedGrid != grid) {
         unmount(s);
         try {
            grid.add(producer);
            s.mountedGrid = grid;
         } catch (Throwable ignored) {
            return;
         }
      }
      producer.setPower(power);
   }

   /** 末影烛粒子：有效蹲起时胸口向上飘（全视角可见），2 秒节流。 */
   private static void spawnFx(ServerPlayer player, Session s, long now) {
      if (now - s.lastFx < FX_INTERVAL) {
         return;
      }
      s.lastFx = now;
      ((ServerLevel) player.level()).sendParticles(
         ParticleTypes.END_ROD,
         player.getX(), player.getY() + 0.6, player.getZ(),
         12, 0.3, 0.45, 0.3, 0.02
      );
   }

   private static int computePower(int flips) {
      if (flips <= 0) {
         return 0;
      }
      for (int i = 1; i < CURVE.length; i++) {
         if (flips <= CURVE[i][0]) {
            int n0 = CURVE[i - 1][0];
            int p0 = CURVE[i - 1][1];
            int n1 = CURVE[i][0];
            int p1 = CURVE[i][1];
            return p0 + (p1 - p0) * (flips - n0) / (n1 - n0);
         }
      }
      return MAX_POWER;
   }

   private static CreativeGeneratorBlockEntity ensureProducer(Session s, ServerPlayer player) {
      if (s.producer != null) {
         return s.producer;
      }
      try {
         BlockPos pos = player.blockPosition();
         BlockState state = dev.dubhe.anvilcraft.init.block.ModBlocks.CREATIVE_GENERATOR.get().defaultBlockState();
         CreativeGeneratorBlockEntity be = new CreativeGeneratorBlockEntity(pos, state);
         be.setLevel(player.level());
         be.setPower(0);
         s.producer = be;
         return be;
      } catch (Throwable ignored) {
         return null;
      }
   }

   private static void unmount(Session s) {
      if (s.mountedGrid != null && s.producer != null) {
         try {
            s.mountedGrid.remove(s.producer);
         } catch (Throwable ignored) {
         }
      }
      if (s.producer != null) {
         try {
            s.producer.setPower(0);
         } catch (Throwable ignored) {
         }
      }
      s.mountedGrid = null;
   }
}
