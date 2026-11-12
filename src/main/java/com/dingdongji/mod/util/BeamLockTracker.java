package com.dingdongji.mod.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * 铭化信标光柱锁定追踪器（普通类，避免事件类/Mixin 直接引用 Mixin 类导致
 * NoClassDefFoundError "is invalid"）。
 *
 * 职责：
 * 1. 记录每个铭化信标当前锁定的遮光方块（TTL 200 tick 兜底，由信标 tick 续期）；
 * 2. 锁定期间右键互动直接取消（时定配方方块均为温度方块，本身无右键交互）；
 *    方块的一切状态变化（红石传导、活塞、流体等）由 LevelChunkMixin 在区块写
 *    状态入口冻结，唯一例外是被挖成 AIR；
 * 3. 光柱解除（底座被拆/方块不再遮光/信标消失）后正常恢复。
 */
public class BeamLockTracker {
   private static final long LOCK_TTL_TICKS = 200L;

   /** 可被"时定"的方块白名单（时定配方，数据包标签，JEI 分类与之对应）。 */
   public static final TagKey<Block> TIME_ANCHORABLE = TagKey.create(
      Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("dingdongji", "time_anchorable")
   );

   /** 仅时定配方标签内的方块；其他遮光方块不再被锁定。 */
   public static boolean isAnchorable(BlockState state) {
      return state.is(TIME_ANCHORABLE);
   }

   /** lockedPos -> 锁定信息（归属信标 + 过期时间戳）。 */
   private static final Map<ResourceKey<Level>, Map<BlockPos, LockInfo>> BEAM_LOCKS = new HashMap<>();

   private record LockInfo(BlockPos beaconPos, long expireAt) {
   }

   /**
    * 铭化信标每 40 tick 对账：currentLocked 为本次光柱照到的全部时定方块
    * （光柱命中方块及其所在层 5×5 范围内同种方块；无命中/未激活时为空集）。
    * 集合差异处理：仍在集合内的续期、消失的解锁、新增的加锁。
    */
   public static void reconcile(ServerLevel level, BlockPos beaconPos, Collection<BlockPos> currentLocked) {
      long now = level.getGameTime();
      Map<BlockPos, LockInfo> locks = BEAM_LOCKS.computeIfAbsent(level.dimension(), k -> new HashMap<>());
      Set<BlockPos> current = new HashSet<>();
      for (BlockPos pos : currentLocked) {
         current.add(pos.immutable());
      }
      Iterator<Map.Entry<BlockPos, LockInfo>> iterator = locks.entrySet().iterator();
      while (iterator.hasNext()) {
         Map.Entry<BlockPos, LockInfo> entry = iterator.next();
         if (entry.getValue().beaconPos().equals(beaconPos) && !current.contains(entry.getKey())) {
            iterator.remove();
         }
      }
      for (BlockPos pos : current) {
         locks.put(pos, new LockInfo(beaconPos.immutable(), now + LOCK_TTL_TICKS));
      }
   }

   /** 信标被整体移除时由 TTL 兜底：过期锁统一清理。建议每 tick 调用。 */
   public static void sweepExpired(MinecraftServer server) {
      for (Map.Entry<ResourceKey<Level>, Map<BlockPos, LockInfo>> dimEntry : BEAM_LOCKS.entrySet()) {
         ServerLevel level = server.getLevel(dimEntry.getKey());
         if (level == null) {
            continue;
         }
         // 时间戳全部以该维度的 getGameTime 写入，清理也必须用同一时钟比较，
         // 不能用 server.getTickCount（非主世界维度时钟不等值，会提前/延后过期）
         long now = level.getGameTime();
         dimEntry.getValue().values().removeIf(info -> now > info.expireAt());
      }
   }

   /** 服务器停止/退出世界时清空全部静态状态，防止整合端换世界后旧锁定串档。 */
   public static void resetAll() {
      BEAM_LOCKS.clear();
   }

   /**
    * 被时定方块的视觉反馈：末地烛（END_ROD）白色微粒从四个水平暴露面缓缓飘出
    * （不在顶面/底面生成）。每 40 tick（2 秒）一轮，四个水平侧面（不含上/下）
    * 独立判定，每个暴露面 20% 概率出 1 粒，贴有其他方块的接触面永远不出；
    * 四面全暴露时期望 0.8 粒/方块/轮。粒子带贴面向外+轻微上浮的初速度。
    */
   public static void tickParticles(MinecraftServer server) {
      if (server.getTickCount() % 40L != 0L) {
         return;
      }
      for (Map.Entry<ResourceKey<Level>, Map<BlockPos, LockInfo>> dimEntry : BEAM_LOCKS.entrySet()) {
         ServerLevel level = server.getLevel(dimEntry.getKey());
         if (level == null) {
            continue;
         }
         long now = level.getGameTime();
         RandomSource random = level.getRandom();
         for (Map.Entry<BlockPos, LockInfo> entry : dimEntry.getValue().entrySet()) {
            if (now > entry.getValue().expireAt()) {
               continue;
            }
            BlockPos pos = entry.getKey();
            // 四个水平侧面逐一判定：暴露（相邻无碰撞形状）且命中 20% 概率才出粒
            for (Direction face : Direction.Plane.HORIZONTAL) {
               BlockState neighbor = level.getBlockState(pos.relative(face));
               if (!neighbor.getCollisionShape(level, pos.relative(face)).isEmpty() || random.nextFloat() >= 0.2F) {
                  continue;
               }
               double along1 = random.nextDouble();
               double along2 = random.nextDouble();
               double px;
               double py;
               double pz;
               double vx;
               double vz;
               int step = face.getAxisDirection().getStep();
               if (face.getAxis() == Direction.Axis.X) {
                  px = step > 0 ? 1.05 : -0.05;
                  py = along1;
                  pz = along2;
                  vx = step * 0.02;
                  vz = 0.0;
               } else {
                  px = along1;
                  py = along2;
                  pz = step > 0 ? 1.05 : -0.05;
                  vx = 0.0;
                  vz = step * 0.02;
               }
               level.sendParticles(
                  ParticleTypes.END_ROD,
                  pos.getX() + px,
                  pos.getY() + py,
                  pos.getZ() + pz,
                  1,
                  vx, 0.015, vz, 0.0
               );
            }
         }
      }
   }

   public static boolean isLocked(Level level, BlockPos pos) {
      Map<BlockPos, LockInfo> locks = BEAM_LOCKS.get(level.dimension());
      if (locks == null) {
         return false;
      }
      long now = level.getGameTime();
      LockInfo info = locks.get(pos);
      if (info == null) {
         return false;
      }
      if (now > info.expireAt()) {
         locks.remove(pos);
         return false;
      }
      return true;
   }

   /**
    * 方块被挖掉（变为 AIR）时立即解锁——不等 reconcile 的 40 tick 周期。
    * 否则玩家挖掉时定方块后立刻补放新方块会被状态冻结拦截（放置回弹）。
    */
   public static void unlockNow(Level level, BlockPos pos) {
      Map<BlockPos, LockInfo> locks = BEAM_LOCKS.get(level.dimension());
      if (locks != null) {
         locks.remove(pos);
      }
   }
}
