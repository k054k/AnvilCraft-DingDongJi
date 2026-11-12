package com.dingdongji.mod.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;

/**
 * 信标转化为铭化信标瞬间的附魔符文吸入粒子：
 * 以信标为中心、半径 3 的立方体外表面随机取点作为出生位置，符文直线飞向
 * 信标中心被吸入。仅服务端播放（sendParticles 同步给周围玩家）。
 */
public class BeaconConversionFx {
   private static final int DURATION_TICKS = 15;
   private static final int PARTICLES_PER_TICK = 12;

   /** 维度 -> 信标位置 -> 剩余 tick。 */
   private static final Map<ResourceKey<Level>, Map<BlockPos, Integer>> ACTIVE = new HashMap<>();

   /** 转化发生时调用：注册一次持续约 0.75 秒的吸入粒子效果。 */
   public static void spawn(ServerLevel level, BlockPos pos) {
      ACTIVE.computeIfAbsent(level.dimension(), k -> new HashMap<>()).put(pos.immutable(), DURATION_TICKS);
   }

   /** 服务器每 tick 调用。 */
   public static void tick(MinecraftServer server) {
      for (Map.Entry<ResourceKey<Level>, Map<BlockPos, Integer>> dimEntry : ACTIVE.entrySet()) {
         ServerLevel level = server.getLevel(dimEntry.getKey());
         if (level == null) {
            continue;
         }
         RandomSource random = level.getRandom();
         Iterator<Map.Entry<BlockPos, Integer>> it = dimEntry.getValue().entrySet().iterator();
         while (it.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = it.next();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0 || !level.getBlockState(entry.getKey()).is(com.dingdongji.mod.block.ModBlocks.ANCHORED_BEACON.get())) {
               it.remove();
               continue;
            }
            entry.setValue(remaining);
            emitRing(level, entry.getKey(), random);
         }
      }
   }

   /**
    * 在以信标为中心、半径 3 的立方体外表面随机取点，符文从该点出生后飞向信标中心。
    *
    *  <p>ENCHANT 粒子（FlyTowardsPositionParticle）为插值运动：sendParticles 的
    *  位置参数是「目标点」，count=0 时速度参数是「相对目标点的出生偏移」，粒子
    *  从目标点+偏移处出生、按 f(1→0) 直线飞向目标点，生命末期额外下坠 ≤1.2 格
    *  （坠入信标方块内淡出，观感即被吸入）。
    */
   private static void emitRing(ServerLevel level, BlockPos pos, RandomSource random) {
      double cx = pos.getX() + 0.5;
      double cy = pos.getY() + 0.5;
      double cz = pos.getZ() + 0.5;
      for (int i = 0; i < PARTICLES_PER_TICK; i++) {
         int face = random.nextInt(6);
         double x;
         double y;
         double z;
         // 表面坐标：信标占 [pos, pos+1]，外表面在 ±3 处；y 方向取 -2.5~+3.5
         double u = pos.getX() - 2.5 + random.nextDouble() * 6.0;
         double v = pos.getY() - 2.5 + random.nextDouble() * 6.0;
         double w = pos.getZ() - 2.5 + random.nextDouble() * 6.0;
         switch (face) {
            case 0 -> { x = pos.getX() - 3.0; y = v; z = w; }
            case 1 -> { x = pos.getX() + 4.0; y = v; z = w; }
            case 2 -> { x = u; y = pos.getY() - 3.0; z = w; }
            case 3 -> { x = u; y = pos.getY() + 4.0; z = w; }
            case 4 -> { x = u; y = v; z = pos.getZ() - 3.0; }
            default -> { x = u; y = v; z = pos.getZ() + 4.0; }
         }
         // 目标点=信标中心，出生偏移=外壳表面点相对中心（半径向量）
         level.sendParticles(
            ParticleTypes.ENCHANT,
            cx, cy, cz, 0,
            x - cx,
            y - cy,
            z - cz,
            1.0
         );
      }
   }

   public static void resetAll() {
      ACTIVE.clear();
   }
}
