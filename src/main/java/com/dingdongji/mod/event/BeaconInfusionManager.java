package com.dingdongji.mod.event;

import com.dingdongji.mod.util.BeaconBaseHelper;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * 铭化信标的「注魔」：
 *
 * <p>附魔物品（带任意附魔的装备/书）被丢入激活铭化信标的光柱后，按普通掉落物
 * 物理自由运动（自然下落、静置在信标顶面，玩家随时可捡起，寿命/拾取均为原版
 * 行为，不做任何锁定）。只要物品实体停留在光柱柱体内，就按在柱内的停留 tick
 * 累计注魔时间；物品离开柱体（被捡起、飘出、消失）时计时清零。
 *
 * <p>信标按底座层数每经过一个固定周期（附魔金块 120 / 100 / 80 / 60 秒，
 * 重质附魔金块 80 / 55 / 35 / 20 秒；两种金块混用则完全不注魔），就对物品进行
 * 一次注魔与一次祛咒：注魔随机把一个非诅咒附魔提升 1 级（可突破原版附魔等级
 * 上限），祛咒随机把一个诅咒降低 1 级（1 级诅咒直接移除）；没有可升级附魔或
 * 没有诅咒时对应操作跳过。纯按时间结算，不接入随机刻，与 randomTickSpeed 无关。
 *
 * <p>同一道光柱同一时刻只对一个附魔物品注魔：柱内存在多个物品时，优先维持
 * 当前正在注魔的物品，其离开后才轮到下一个，避免多个物品同时被注魔。
 *
 * <p>判定范围比视觉光柱向四周各扩大一格（3×3 截面），物品不必精确落在信标
 * 正上方那一列，只要在光柱及其周围一圈的柱体内即可注魔；垂直方向从信标顶面
 * 一直到光柱顶端。
 *
 * <p>粒子：ENCHANT 粒子会被吸向自己的「生成位置」，速度参数决定出生偏移。
 * 因此在物品中心生成、速度给随机外向偏移——粒子出生在物品四周更大范围后
 * 回旋飞入物品，与书架符文飞向附魔台中央之书的表现一致。
 */
public class BeaconInfusionManager {
   /** 维度 -> 激活的铭化信标位置。 */
   private static final Map<ResourceKey<Level>, Set<BlockPos>> ACTIVE_BEACONS = new HashMap<>();
   /** 维度 -> 物品实体 id -> 注魔状态（归属信标 + 在柱内累计 tick 数）。 */
   private static final Map<ResourceKey<Level>, Map<Integer, InfusingItem>> INFUSING = new HashMap<>();
   /** 维度 -> 信标位置 -> 当前注魔周期秒数缓存（每秒刷新，0=混用底座不注魔）。 */
   private static final Map<ResourceKey<Level>, Map<BlockPos, Integer>> SECONDS_CACHE = new HashMap<>();

   /** 单个在柱物品的服务端状态。 */
   private static final class InfusingItem {
      final BlockPos beacon;
      int elapsedTicks;

      InfusingItem(BlockPos beacon) {
         this.beacon = beacon;
      }
   }

   /** 由信标 BlockEntity tick 每 tick 调用：注册/注销激活的铭化信标。 */
   public static void setActive(ServerLevel level, BlockPos beaconPos, boolean active) {
      ResourceKey<Level> dim = level.dimension();
      if (active) {
         ACTIVE_BEACONS.computeIfAbsent(dim, k -> new HashSet<>()).add(beaconPos.immutable());
      } else {
         Set<BlockPos> beacons = ACTIVE_BEACONS.get(dim);
         if (beacons != null) {
            beacons.remove(beaconPos);
         }
         releaseBeacon(level, beaconPos);
      }
   }

   /** 服务器每 tick 调用。 */
   public static void tick(MinecraftServer server) {
      for (ResourceKey<Level> dim : new ArrayList<>(ACTIVE_BEACONS.keySet())) {
         ServerLevel level = server.getLevel(dim);
         if (level == null) {
            continue;
         }
         Set<BlockPos> beacons = ACTIVE_BEACONS.get(dim);
         if (beacons == null || beacons.isEmpty()) {
            continue;
         }
         long gameTime = level.getGameTime();
         if (gameTime % 20L == 0L) {
            Map<BlockPos, Integer> cache = SECONDS_CACHE.computeIfAbsent(dim, k -> new HashMap<>());
            for (BlockPos beaconPos : beacons) {
               cache.put(beaconPos, BeaconBaseHelper.infusionSeconds(level, beaconPos));
            }
         }
         Map<Integer, InfusingItem> infusing = INFUSING.computeIfAbsent(dim, k -> new HashMap<>());
         for (BlockPos beaconPos : new ArrayList<>(beacons)) {
            // 兜底：信标被整体移除时不会再有 BlockEntity tick 来注销，这里自行校验
            if (!level.getBlockState(beaconPos).is(com.dingdongji.mod.block.ModBlocks.ANCHORED_BEACON.get())
               || !BeaconBaseHelper.hasEnchantedBase(level, beaconPos)) {
               beacons.remove(beaconPos);
               releaseBeacon(level, beaconPos);
               continue;
            }
            tickBeacon(level, beaconPos, infusing, gameTime);
         }
         // 物品若已不属于任何仍然存在的信光柱体，其条目在 tickBeacon 对账中删除
      }
   }

   /** 统计光柱柱体内停留的附魔物品，累计注魔时间并按周期结算。 */
   private static void tickBeacon(ServerLevel level, BlockPos pos,
         Map<Integer, InfusingItem> infusing, long gameTime) {
      int beamTop = BeaconBaseHelper.scanBeamTop(level, pos);
      // 柱体截面比视觉光柱（1×1）向四周各扩一格：x/z 从 -1 到 +2（3×3）；
      // 下界放到 y+0.75：静置信标顶面的掉落物 AABB 下沿恰好压在 y+1，用相交判定
      AABB beam = new AABB(
         pos.getX() - 1.0, pos.getY() + 0.75, pos.getZ() - 1.0,
         pos.getX() + 2.0, beamTop, pos.getZ() + 2.0
      );
      // 柱体内可注魔、且未被相邻光柱先占的物品
      List<ItemEntity> candidates = new ArrayList<>();
      for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, beam)) {
         if (item.isRemoved() || !isInfusible(item.getItem())) {
            continue;
         }
         InfusingItem occupied = infusing.get(item.getId());
         if (occupied != null && !occupied.beacon.equals(pos)) {
            continue;
         }
         candidates.add(item);
      }
      // 同一道光柱同一时刻只注魔一个物品：优先维持当前正在注魔的物品，
      // 保证选定结果稳定、不每 tick 抖动；它离开后下一个 tick 才重选
      ItemEntity active = null;
      for (ItemEntity item : candidates) {
         if (infusing.containsKey(item.getId())) {
            active = item;
            break;
         }
      }
      if (active == null && !candidates.isEmpty()) {
         active = candidates.get(0);
         infusing.put(active.getId(), new InfusingItem(pos.immutable()));
      }
      Set<Integer> inBeam = new HashSet<>();
      for (ItemEntity item : candidates) {
         inBeam.add(item.getId());
      }
      if (active != null) {
         InfusingItem state = infusing.get(active.getId());
         int seconds = SECONDS_CACHE
            .getOrDefault(level.dimension(), Map.of())
            .getOrDefault(pos, BeaconBaseHelper.infusionSeconds(level, pos));
         // 混用底座（seconds=0）计时暂停
         if (seconds > 0) {
            state.elapsedTicks++;
            if (state.elapsedTicks >= seconds * 20) {
               applyInfusion(level, active, level.getRandom());
               state.elapsedTicks = 0;
            }
         }
         emitInfusionParticles(level, active, pos, level.getRandom());
      }
      // 对账：归属本信标、但本 tick 已不在柱体内的物品——删除计时（被捡起/飘走/消失）
      infusing.entrySet().removeIf(e ->
         e.getValue().beacon.equals(pos) && !inBeam.contains(e.getKey())
      );
   }

   /** 注魔粒子：ENCHANT 粒子在物品周围半径 1.0 的立方体表面随机
    *  出生，从四面八方飞向物品中心被吸入；只取立方体的上半部分
    *  （顶面 + 四个侧面的上半段），符文自上而下汇聚；粒子量为旧版
    *  的一半（每 tick 1 颗）。
    *
    *  <p>目标 y 取 {@code getY()+1.825}：在原版 1.325 的基础上抬高半格，
    *  使符文消失点（生命末期下坠 ≤1.2 格后）落在物品上方更高处，
    *  不穿入下方方块。 */
   private static final int INFUSION_PARTICLES_PER_TICK = 1;
   private static final double INFUSION_RADIUS = 1.0;

   private static void emitInfusionParticles(
      ServerLevel level, ItemEntity item, BlockPos beaconPos, RandomSource random) {
      double cx = item.getX();
      double cy = item.getY() + 1.825;
      double cz = item.getZ();
      double r = INFUSION_RADIUS;
      for (int i = 0; i < INFUSION_PARTICLES_PER_TICK; i++) {
         // 上半部分：顶面（y=+r）或四个侧面（x/z=±r，y∈[0,+r]）
         int face = random.nextInt(5);
         double x;
         double y;
         double z;
         if (face == 0) {
            x = cx - r + random.nextDouble() * 2.0 * r;
            y = cy + r;
            z = cz - r + random.nextDouble() * 2.0 * r;
         } else {
            x = cx - r + random.nextDouble() * 2.0 * r;
            y = cy + random.nextDouble() * r;
            z = cz - r + random.nextDouble() * 2.0 * r;
            switch (face) {
               case 1 -> x = cx - r;
               case 2 -> x = cx + r;
               case 3 -> z = cz - r;
               default -> z = cz + r;
            }
         }
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

   /**
    * 一个周期的注魔与祛咒（两者各尝试一次，互不影响）：
    * 注魔——随机一个非诅咒附魔 +1 级，可突破原版附魔等级上限（set 封顶 255）；
    * 祛咒——随机一个诅咒降低 1 级，1 级诅咒 set(0) 时自动移除。
    * 没有非诅咒附魔或没有诅咒时对应操作跳过；物品可能是附魔书（STORED_ENCHANTMENTS）。
    */
   private static void applyInfusion(ServerLevel level, ItemEntity itemEntity, RandomSource random) {
      ItemStack stack = itemEntity.getItem().copy();
      boolean stored = stack.has(DataComponents.STORED_ENCHANTMENTS);
      DataComponentType<ItemEnchantments> component = stored
         ? DataComponents.STORED_ENCHANTMENTS
         : DataComponents.ENCHANTMENTS;
      ItemEnchantments enchantments = stack.getOrDefault(component, ItemEnchantments.EMPTY);
      if (enchantments.isEmpty()) {
         return;
      }
      List<Holder<Enchantment>> curses = new ArrayList<>();
      List<Holder<Enchantment>> upgradable = new ArrayList<>();
      for (Entry<Holder<Enchantment>> e : enchantments.entrySet()) {
         Holder<Enchantment> holder = e.getKey();
         if (holder.is(EnchantmentTags.CURSE)) {
            curses.add(holder);
         } else {
            upgradable.add(holder);
         }
      }

      ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(enchantments);
      boolean changed = false;
      if (!upgradable.isEmpty()) {
         // 注魔：随机一个非诅咒附魔 +1，不检查 getMaxLevel，允许超上限
         Holder<Enchantment> target = upgradable.get(random.nextInt(upgradable.size()));
         mutable.set(target, mutable.getLevel(target) + 1);
         changed = true;
      }
      if (!curses.isEmpty()) {
         // 祛咒：随机一个诅咒 -1，1 级时 set 自动移除
         Holder<Enchantment> curse = curses.get(random.nextInt(curses.size()));
         mutable.set(curse, mutable.getLevel(curse) - 1);
         changed = true;
      }
      if (changed) {
         stack.set(component, mutable.toImmutable());
         itemEntity.setItem(stack);
      }
   }

   /** 是否为可注魔的附魔物品（装备附魔或附魔书的存储附魔非空）。 */
   private static boolean isInfusible(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      }
      return !stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty()
         || !stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty();
   }

   /** 信标失效时清掉其名下全部计时与周期缓存（物品保持原版掉落物行为，无需释放）。 */
   private static void releaseBeacon(ServerLevel level, BlockPos beaconPos) {
      Map<Integer, InfusingItem> infusing = INFUSING.get(level.dimension());
      if (infusing != null) {
         infusing.values().removeIf(state -> state.beacon.equals(beaconPos));
      }
      Map<BlockPos, Integer> cache = SECONDS_CACHE.get(level.dimension());
      if (cache != null) {
         cache.remove(beaconPos);
      }
   }

   public static void resetAll() {
      ACTIVE_BEACONS.clear();
      INFUSING.clear();
      SECONDS_CACHE.clear();
   }
}
