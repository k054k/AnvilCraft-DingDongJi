package com.dingdongji.mod.block;

import com.dingdongji.mod.util.BeamLockTracker;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * 过冷的浮霜金属块：被极度冷却、持续散发寒气的浮霜金属块。
 *
 * <p>对齐本体温度方块架构（本体 {@code HeatableBlock} 一族均非方块实体方块，
 * 因此原版活塞天然可推）：倒计时存放于 BlockState 整数属性（每档 1 秒），由
 * 原版 {@code scheduleTick} 驱动。本方块不带 BlockEntity，可被活塞推动/粘性
 * 活塞拉动，剩余时间随 BlockState 天然跟随移动与存档。
 *
 * <p>直接冰冻伤害：生物站上后由 {@link #stepOn} 每 tick 尝试造成 freeze 伤害，
 * 由原版受伤无敌帧限流为约每 0.5 秒 1 点（学本体超温余烬金属块烫脚节奏）。
 * 不走细雪 ticksFrozen 累积，因此没有屏幕结霜糊脸效果，离开即停止受伤。
 * {@code canFreeze()} 为 false（冰冻免疫生物）时不伤；创造模式由原版 hurt 免伤。
 * 方块周边持续漂浮白色寒雾粒子。
 */
public class SupercooledFrostMetalBlock extends Block {
   public static final MapCodec<SupercooledFrostMetalBlock> CODEC = simpleCodec(SupercooledFrostMetalBlock::new);

   /** 剩余倒计时（秒）：放置默认 60（1 分钟），每秒 -1，归 0 劣化。 */
   public static final IntegerProperty FROST_SECONDS = IntegerProperty.create("frost_seconds", 0, 60);

   /** 单次冰冻伤害（每 tick 尝试，无敌帧限流后约每 0.5 秒结算一次）。 */
   private static final float FROST_DAMAGE = 1.0F;

   private static Block frostMetalBlock;
   private static Block royalSteelBlock;

   public SupercooledFrostMetalBlock(Properties properties) {
      super(properties);
      registerDefaultState(stateDefinition.any().setValue(FROST_SECONDS, 60));
   }

   @Override
   protected MapCodec<? extends Block> codec() {
      return CODEC;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(FROST_SECONDS);
   }

   @Override
   protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
      super.onPlace(state, level, pos, oldState, isMoving);
      level.scheduleTick(pos, this, 20);
   }

   /**
    * 每 20 tick 递减 1 档（1 秒）。被铭化信标"时定"期间（BeamLockTracker 锁定）
    * 不递减、只重新调度=时间暂停。归 0 后 5% 概率劣化为皇家钢块（对标超温余烬
    * 金属块劣化为下界合金块），否则变回浮霜金属块。
    */
   @Override
   protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      if (BeamLockTracker.isLocked(level, pos)) {
         level.scheduleTick(pos, this, 20);
         return;
      }
      int seconds = state.getValue(FROST_SECONDS);
      if (seconds <= 0) {
         Block result = random.nextFloat() < 0.05F ? royalSteelBlock() : frostMetalBlock();
         level.setBlockAndUpdate(pos, result.defaultBlockState());
         return;
      }
      level.setBlock(pos, state.setValue(FROST_SECONDS, seconds - 1), 3);
      level.scheduleTick(pos, this, 20);
   }

   /**
    * 活塞推动/粘性拉动的自愈：活塞 finalize 的 setBlock 旗标含 16（不触发
    * onPlace），但含 4（邻居更新）——被移动的方块至少收到一次 neighborChanged，
    * 此时若该位置没有已排程的 tick 则补一个，倒计时恢复运行。正常放置路径
    * 已有调度，hasScheduledTick 判空保证幂等。
    */
   @Override
   protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean isMoving) {
      super.neighborChanged(state, level, pos, neighborBlock, fromPos, isMoving);
      if (!level.isClientSide && !level.getBlockTicks().hasScheduledTick(pos, this)) {
         level.scheduleTick(pos, this, 20);
      }
   }

   /**
    * 持续冰冻伤害：与本体 {@code RedhotBlock.stepOn}（超温余烬金属块烫脚）同构。
    * 站上即每 tick 尝试 1 点 freeze 伤害，{@link Entity#hurt} 的 10 tick 无敌帧
    * 把实际频率限流为约每 0.5 秒 1 点；潜行（isSteppingCarefully）时不伤，
    * 冰冻免疫生物（canFreeze=false）不伤，创造模式原版免伤。
    */
   @Override
   public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
      if (!entity.isSteppingCarefully() && entity instanceof LivingEntity living
            && living.canFreeze()) {
         entity.hurt(level.damageSources().freeze(), FROST_DAMAGE);
      }
      super.stepOn(level, pos, state, entity);
   }

   /** 周边持续漂浮的白色寒雾：白色灰烬粒子带轻微上升气流，在顶面与四周飘散。 */
   @Override
   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
      double x0 = pos.getX();
      double y0 = pos.getY();
      double z0 = pos.getZ();
      int count = random.nextInt(3) + 1;
      for (int i = 0; i < count; i++) {
         double x;
         double z;
         double y;
         // 三分之二从顶面升起，三分之一贴着方块侧面外沿漂浮
         if (random.nextInt(3) != 0) {
            x = x0 + random.nextDouble();
            z = z0 + random.nextDouble();
            y = y0 + 0.9 + random.nextDouble() * 0.35;
         } else {
            boolean alongX = random.nextBoolean();
            x = alongX ? x0 + random.nextDouble() : x0 + (random.nextBoolean() ? -0.08 : 1.08);
            z = alongX ? z0 + (random.nextBoolean() ? -0.08 : 1.08) : z0 + random.nextDouble();
            y = y0 + random.nextDouble();
         }
         double vx = (random.nextDouble() - 0.5) * 0.02;
         double vy = 0.008 + random.nextDouble() * 0.02;
         double vz = (random.nextDouble() - 0.5) * 0.02;
         level.addParticle(ParticleTypes.WHITE_ASH, x, y, z, vx, vy, vz);
      }
   }

   private static Block frostMetalBlock() {
      if (frostMetalBlock == null) {
         frostMetalBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("anvilcraft", "frost_metal_block"));
      }
      return frostMetalBlock;
   }

   private static Block royalSteelBlock() {
      if (royalSteelBlock == null) {
         royalSteelBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("anvilcraft", "royal_steel_block"));
      }
      return royalSteelBlock;
   }
}
