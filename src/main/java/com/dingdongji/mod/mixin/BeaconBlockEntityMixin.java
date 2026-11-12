package com.dingdongji.mod.mixin;

import com.dingdongji.mod.block.ModBlocks;
import com.dingdongji.mod.util.BeaconBaseHelper;
import com.dingdongji.mod.util.BeamLockTracker;
import dev.dubhe.anvilcraft.util.TriggerUtil;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 重质金块金字塔（1 个重质金块 = 9 个普通金块）对信标的转化：
 * - 1~3 层（3×3 / 5×5 / 7×7）：不自动转化，走铁砧砸落概率机制
 *   （与本体同表 18% / 45% / 100%，见 HeavyGoldBeaconConversionBehavior）；
 * - 连续铺满 4 层（9×9）同种重质金块：放上即刻转化，无需铁砧与金锭。
 *   诅咒侧触发本体进度与雷暴；附魔侧转晴。
 * 本体附魔金块底座的转化走铁砧概率机制（见 EnchantedBeaconConversionBehavior）。
 *
 * 铭化信标激活（3×3 附魔重质金块/附魔金块底座）时：
 * - 光柱内活体每 40 tick 获得生命恢复 I（持续 100 tick），玩家负面效果被净化；
 * - 光柱照到的第一个白名单方块为中心的 5×5×5 同种方块被时定锁定
 *   （每 tick 扫描/对账，进入范围即刻冻结，玩家无法改变其方块状态，
 *   最后一次右键互动在光柱解除后回放，见 BeamLockTracker）。
 */
@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin {
   private static final long EFFECT_INTERVAL_TICKS = 40L;

   @Inject(method = "tick", at = @At("HEAD"))
   private static void ddj$heavyGoldBaseConversion(Level level, BlockPos pos, BlockState state, BeaconBlockEntity beacon, CallbackInfo ci) {
      if (!(level instanceof ServerLevel serverLevel) || level.getGameTime() % EFFECT_INTERVAL_TICKS != 0L) {
         return;
      }
      // 仅普通信标可被即刻转化：否则已转化的铭化/腐化信标会每 40 tick 重放
      // 转化（符文粒子墙 + 强制改天），日志实证曾 2 秒一次连刷 67 轮
      if (!state.is(Blocks.BEACON)) {
         return;
      }
      Block cursedHeavy = (Block)ModBlocks.CURSED_HEAVY_GOLD_BLOCK.get();
      Block enchantedHeavy = (Block)ModBlocks.ENCHANTED_HEAVY_GOLD_BLOCK.get();
      Block firstLayerBlock = null;
      if (BeaconBaseHelper.isLayerFull(level, pos.below(), 1, cursedHeavy)) {
         firstLayerBlock = cursedHeavy;
      } else if (BeaconBaseHelper.isLayerFull(level, pos.below(), 1, enchantedHeavy)) {
         firstLayerBlock = enchantedHeavy;
      }
      if (firstLayerBlock == null) {
         return;
      }
      // 仅 4 层连续全满才即刻转化；1~3 层交给铁砧概率行为
      if (BeaconBaseHelper.countLayers(level, pos, firstLayerBlock) < 4) {
         return;
      }
      if (firstLayerBlock == cursedHeavy) {
         TriggerUtil.convertBeacon(level, pos);
         Block corrupted = (Block)BuiltInRegistries.BLOCK.get(ResourceLocation.parse("anvilcraft:corrupted_beacon"));
         serverLevel.setBlockAndUpdate(pos, corrupted.defaultBlockState());
         if (serverLevel.getServer().getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).get()) {
            serverLevel.setWeatherParameters(0, ServerLevel.THUNDER_DURATION.sample(serverLevel.getRandom()), true, true);
         }
      } else {
         BeaconBaseHelper.convertToAnchored(serverLevel, pos);
      }
   }

   @Inject(method = "tick", at = @At("TAIL"))
   private static void ddj$anchoredBeaconEffects(Level level, BlockPos pos, BlockState state, BeaconBlockEntity beacon, CallbackInfo ci) {
      if (!(level instanceof ServerLevel serverLevel)) {
         return;
      }
      java.util.List<BlockPos> currentLocked = java.util.List.of();
      boolean active = state.is((Block)ModBlocks.ANCHORED_BEACON.get()) && BeaconBaseHelper.hasEnchantedBase(level, pos);
      if (active) {
         int beamTop = BeaconBaseHelper.scanBeamTop(level, pos);
         // 生命恢复 + 净化 + 生物反向转化：保持每 40 tick 一次
         if (level.getGameTime() % EFFECT_INTERVAL_TICKS == 0L) {
            // 覆盖到第一个遮光方块为止的光柱柱体
            AABB beam = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, beamTop, pos.getZ() + 1);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, beam)) {
               entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, true, true));
               // 铭化信标光柱内的玩家：清除所有负面效果
               if (entity instanceof Player player) {
                  for (MobEffectInstance inst : new java.util.ArrayList<>(player.getActiveEffects())) {
                     if (inst.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
                        player.removeEffect(inst.getEffect());
                     }
                  }
               }
               ddj$reverseTransformEntity(serverLevel, entity);
            }
         }
         // 时定：每 tick 扫描，光柱实际照到的第一个白名单方块，
         // 再以该方块为中心把 5×5×5 立方体内的时定配方方块一并锁定（当 tick 即冻结）
         if (beamTop < level.getMaxBuildHeight()) {
            BlockPos hitPos = new BlockPos(pos.getX(), beamTop, pos.getZ());
            BlockState hitState = level.getBlockState(hitPos);
            if (BeamLockTracker.isAnchorable(hitState)) {
               java.util.List<BlockPos> locked = new java.util.ArrayList<>();
               for (int dx = -2; dx <= 2; dx++) {
                  for (int dy = -2; dy <= 2; dy++) {
                     for (int dz = -2; dz <= 2; dz++) {
                        BlockPos candidate = hitPos.offset(dx, dy, dz);
                        BlockState candidateState = level.getBlockState(candidate);
                        // 不要求与被射中方块同种，只要在时定配方白名单内即可
                        if (BeamLockTracker.isAnchorable(candidateState)) {
                           locked.add(candidate.immutable());
                        }
                     }
                  }
               }
               currentLocked = locked;
            }
         }
      }
      // 每 tick 对账：新锁定/续期/解除（解除时回放最后一次右键互动）
      BeamLockTracker.reconcile(serverLevel, pos, currentLocked);
      // 注册/注销注魔管理器中的激活信标
      com.dingdongji.mod.event.BeaconInfusionManager.setActive(serverLevel, pos, active);
   }

   /** 输入实体类型 → 转化配方（自有 anchored_mob_transform 配方，不复用本体配方）。 */
   private static Map<EntityType<?>, java.util.List<com.dingdongji.mod.recipe.AnchoredMobTransformRecipe>> ddj$reverseTransformMap;
   private static RecipeManager ddj$reverseTransformOwner;

   /** 铭化信标光柱内：命中腐化信标转化配方的"结果生物"时反向变回"转化前生物"（与腐化信标相反）。 */
   private static void ddj$reverseTransformEntity(ServerLevel level, LivingEntity entity) {
      if (!entity.isAlive() || entity instanceof Player) {
         return;
      }
      java.util.List<com.dingdongji.mod.recipe.AnchoredMobTransformRecipe> recipes = ddj$reverseTransformMap(level).get(entity.getType());
      if (recipes == null) {
         return;
      }
      com.dingdongji.mod.recipe.AnchoredMobTransformRecipe recipe = ddj$selectRecipe(recipes, entity);
      if (recipe == null) {
         return;
      }
      // 变体按手持区分结果手持（如巨人的巨型铁砧/巨型独石核心/巨型独石线）
      com.dingdongji.mod.recipe.AnchoredMobTransformRecipe.Variant variant = recipe.selectVariant(entity);
      EntityType<?> target = recipe.result();
      CompoundTag tag = new CompoundTag();
      tag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(target).toString());
      Entity created = EntityType.loadEntityRecursive(tag, level, e -> {
         e.moveTo(entity.getX(), entity.getY(), entity.getZ(), e.getYRot(), e.getXRot());
         return e;
      });
      if (created == null) {
         return;
      }
      // 与本体 MobTransformRecipe.apply 一致：Mob 需 finalizeSpawn
      if (created instanceof net.minecraft.world.entity.Mob mob) {
         mob.finalizeSpawn(level, level.getCurrentDifficultyAt(created.blockPosition()),
            net.minecraft.world.entity.MobSpawnType.NATURAL, null);
      }
      // 配方指定的生成物手持物品（如巨人→僵尸时手持砧）
      net.minecraft.world.item.ItemStack held = variant != null ? variant.resultItem() : recipe.resultItem();
      if (created instanceof LivingEntity living && !held.isEmpty()) {
         living.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, held.copy());
         if (living instanceof net.minecraft.world.entity.Mob persistMob) {
            persistMob.setPersistenceRequired();
         }
      }
      Entity vehicle = entity.isPassenger() ? entity.getVehicle() : null;
      entity.discard();
      if (level.tryAddFreshEntityWithPassengers(created) && vehicle != null) {
         created.startRiding(vehicle);
      }
   }

   private static Map<EntityType<?>, java.util.List<com.dingdongji.mod.recipe.AnchoredMobTransformRecipe>> ddj$reverseTransformMap(ServerLevel level) {
      RecipeManager manager = level.getServer().getRecipeManager();
      if (ddj$reverseTransformMap == null || ddj$reverseTransformOwner != manager) {
         Map<EntityType<?>, java.util.List<com.dingdongji.mod.recipe.AnchoredMobTransformRecipe>> map = new HashMap<>();
         for (RecipeHolder<com.dingdongji.mod.recipe.AnchoredMobTransformRecipe> holder
            : manager.getAllRecipesFor(
               (RecipeType<com.dingdongji.mod.recipe.AnchoredMobTransformRecipe>)(Object)
                  com.dingdongji.mod.init.ModRecipes.ANCHORED_MOB_TRANSFORM_TYPE.get())) {
            // 一个配方可含多个输入实体（如掠夺者/卫道士/唤魔者→村民），逐一建索引；
            // 同一实体类型可有多条带手持条件的配方（如巨人的三条逆向）
            for (EntityType<?> input : holder.value().inputs()) {
               map.computeIfAbsent(input, k -> new java.util.ArrayList<>()).add(holder.value());
            }
         }
         ddj$reverseTransformMap = map;
         ddj$reverseTransformOwner = manager;
      }
      return ddj$reverseTransformMap;
   }

   /**
    * 按手持物品选择配方：优先命中手持条件匹配的配方，
    * 否则回退到无条件的配方（保持旧行为）。
    */
   private static com.dingdongji.mod.recipe.AnchoredMobTransformRecipe ddj$selectRecipe(
      java.util.List<com.dingdongji.mod.recipe.AnchoredMobTransformRecipe> recipes, LivingEntity entity) {
      com.dingdongji.mod.recipe.AnchoredMobTransformRecipe fallback = null;
      for (com.dingdongji.mod.recipe.AnchoredMobTransformRecipe recipe : recipes) {
         if (recipe.inputHeld().isEmpty() && recipe.variants().isEmpty()) {
            if (fallback == null) {
               fallback = recipe;
            }
         } else if (recipe.matchesRecipe(entity)) {
            return recipe;
         }
      }
      return fallback;
   }
}
