package com.dingdongji.mod.event;

import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.item.component.GlowingVisionComponent;
import com.dingdongji.mod.util.AnvilCraftCompat;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ModArmorSetHandler {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int EFFECT_DURATION = 6000; // 5 分钟

    // ===== 蹈虚手动开关状态 =====
    private static final Map<UUID, Integer> STRIDE_VOID_MODE = new HashMap<>(); // 0=正常重力, 1=无重力, 2=低重力, 3=反重力

    // ===== 蹈火开关状态 =====
    private static final Map<UUID, Boolean> LAVA_WALKER_ENABLED = new HashMap<>();

    // ===== Boss弹飞冷却 =====
    private static final Map<UUID, Long> BOSS_PROXIMITY_START = new HashMap<>(); // Boss UUID → 进入1格时间
    private static final Map<UUID, Long> BOSS_REPEL_COOLDOWN = new HashMap<>();  // Boss UUID → 冷却结束时间
    private static final long PROXIMITY_THRESHOLD = 60;  // 3秒（60 tick）
    private static final long REPEL_COOLDOWN = 200;      // 10秒（200 tick）

    // 浴火重生：上次给予生命恢复的 game time
    static final Map<UUID, Long> EMBER_LEG_LAST_HEAL_TIME = new HashMap<>();

    // 超限胸甲紧急恢复冷却
    public static final Map<UUID, Long> CHEST_HEAL_COOLDOWN = new HashMap<>();
    static final Map<UUID, Boolean> CHEST_HEAL_NOTIFIED = new HashMap<>(); // 冷却是否已通知
    public static final long CHEST_HEAL_INTERVAL = 1200; // 1分钟（1200 tick）

    // ===== 玩家退出清理 =====
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUUID();
        STRIDE_VOID_MODE.remove(uuid);
        LAVA_WALKER_ENABLED.remove(uuid);
        BOSS_PROXIMITY_START.clear();
        BOSS_REPEL_COOLDOWN.clear();
        CHEST_HEAL_COOLDOWN.remove(uuid);
        CHEST_HEAL_NOTIFIED.remove(uuid);
        EMBER_LEG_LAST_HEAL_TIME.remove(uuid);
    }

    // ===== 主 Tick =====
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        // ===== 叽套（全套效果）=====
        handleJiSet(player);

        // ===== 舒适（皇家钢靴子专属）=====
        handleComfortable(player);

        // ===== 皇家钢套（逐件效果）=====
        // 注意：必须在浴火重生和应急治愈之前执行，否则会误移除它们的 REGENERATION
        handleRoyalSteelChestplate(player);

        // ===== 余烬金属套（逐件效果）=====
        handleEmberHelmet(player);
        handleEmberChestplate(player);
        handleEmberLeggings(player);
        handleEmberBoots(player);
        handleEmberBootsFirePath(player);

        // ===== 超限合金套（逐件效果）=====
        handleTranscendiumHelmet(player);
        // 壁垒II 已在 handleEmberChestplate 中统一处理
        handleTranscendiumReflect(player);
        handleTranscendiumStrideVoid(player);

        // 蹈火双击检测已移除，改为键位切换（见 toggleLavaWalker）

        // ===== 超限胸甲：应急恢复检测 + 冷却通知 =====
        UUID uuid = player.getUUID();
        long now = player.level().getGameTime();
        long lastHeal = CHEST_HEAL_COOLDOWN.getOrDefault(uuid, 0L);

        // 应急恢复：生命值低于10时给予10秒生命恢复V（无图标无粒子），冷却5分钟
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.is(ModItems.TRANSCENDIUM_CHESTPLATE.get()) && player.getHealth() < 10.0f
                && now >= lastHeal + CHEST_HEAL_INTERVAL) {
            // 清除所有负面效果（适配原版和其他模组）
            for (net.minecraft.world.effect.MobEffectInstance activeEffect : new java.util.ArrayList<>(player.getActiveEffects())) {
                if (!activeEffect.getEffect().value().isBeneficial()) {
                    player.removeEffect(activeEffect.getEffect());
                }
            }

            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 4, false, false, false));
            LOGGER.info("[DingDongJi] 应急治愈触发，当前生命值: {}", player.getHealth());
            CHEST_HEAL_COOLDOWN.put(uuid, now);
            CHEST_HEAL_NOTIFIED.put(uuid, false);
            player.displayClientMessage(
                    Component.literal(">>>生命值低！已启用应急治愈，清除所有负面效果！").withStyle(ChatFormatting.RED),
                    true
            );

            // 触发时产生白色放气粒子效果（类似劫掠兽怒吼后的白色烟雾）
            if (player.level() instanceof ServerLevel serverLevel) {
                double px = player.getX();
                double py = player.getY() + 0.5;
                double pz = player.getZ();
                serverLevel.sendParticles(ParticleTypes.POOF,
                        px, py, pz, 40,    // 40 个粒子
                        1.2, 1.0, 1.2,      // 扩散范围
                        0.05                // 速度
                );
            }

            // 弹飞自身2格内的所有生物（力度3）
            AABB knockbackArea = player.getBoundingBox().inflate(2.0);
            List<LivingEntity> nearbyEntities = player.level().getEntitiesOfClass(LivingEntity.class, knockbackArea);
            for (LivingEntity entity : nearbyEntities) {
                if (entity == player) continue;
                double dx = entity.getX() - player.getX();
                double dz = entity.getZ() - player.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist < 0.1) {
                    dx = player.getLookAngle().x;
                    dz = player.getLookAngle().z;
                    dist = 1.0;
                }
                double forceX = (dx / dist) * 3.0;
                double forceZ = (dz / dist) * 3.0;
                entity.setDeltaMovement(entity.getDeltaMovement().add(forceX, 0.4, forceZ));
                entity.hurtMarked = true;
            }

            // 在脚边（弹飞范围边缘）生成一圈 END_ROD 粒子
            spawnEndRodRing(player, 2.0);
        }

        // 冷却结束通知
        // 注意：必须从 MAP 重新读取冷却结束时间，不能使用局部变量 lastHeal
        // （局部变量在触发块执行前就已捕获，触发块更新了 Map 但局部变量不会同步更新）
        boolean notified = CHEST_HEAL_NOTIFIED.getOrDefault(uuid, true);
        long actualLastHeal = CHEST_HEAL_COOLDOWN.getOrDefault(uuid, 0L);
        if (!notified && now >= actualLastHeal + CHEST_HEAL_INTERVAL) {
            CHEST_HEAL_NOTIFIED.put(uuid, true);
            player.displayClientMessage(
                    Component.literal(">>>应急治愈已冷却完毕！").withStyle(ChatFormatting.AQUA),
                    true
            );
        }
    }

    // ========================================================================
    //  药水效果辅助
    // ========================================================================

    private static void addHiddenEffect(Player player, Holder<MobEffect> effect, int amplifier) {
        MobEffectInstance existing = player.getEffect(effect);
        if (existing == null || existing.getDuration() < 400) {
            player.addEffect(new MobEffectInstance(effect, EFFECT_DURATION, amplifier, false, false, false));
        }
    }

    private static void removeEffect(Player player, Holder<MobEffect> effect) {
        if (player.getEffect(effect) != null) {
            player.removeEffect(effect);
        }
    }

    // ========================================================================
    //  叽套：全套效果（挖掘速度 + 方块交互距离）
    // ========================================================================
    private static final ResourceLocation JI_SET_BREAK_SPEED_ID =
            ResourceLocation.parse("dingdongji:ji_set_break_speed");
    private static final ResourceLocation JI_SET_REACH_ID =
            ResourceLocation.parse("dingdongji:ji_set_reach");

    private static void handleJiSet(Player player) {
        boolean fullSet = player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.JI_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.JI_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.JI_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.JI_BOOTS.get());

        AttributeInstance breakSpeed = player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
        AttributeInstance reach = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);

        if (fullSet) {
            if (breakSpeed != null) {
                breakSpeed.removeModifier(JI_SET_BREAK_SPEED_ID);
                breakSpeed.addTransientModifier(
                        new AttributeModifier(JI_SET_BREAK_SPEED_ID, 2.0, AttributeModifier.Operation.ADD_VALUE)
                );
            }
            if (reach != null) {
                reach.removeModifier(JI_SET_REACH_ID);
                reach.addTransientModifier(
                        new AttributeModifier(JI_SET_REACH_ID, 4.0, AttributeModifier.Operation.ADD_VALUE)
                );
            }
        } else {
            if (breakSpeed != null) breakSpeed.removeModifier(JI_SET_BREAK_SPEED_ID);
            if (reach != null) reach.removeModifier(JI_SET_REACH_ID);
        }
    }

    // ========================================================================
    //  舒适：皇家钢靴子专属（移动速度+0.02，步高+0.5）
    // ========================================================================
    private static final ResourceLocation COMFORTABLE_SPEED_ID =
            ResourceLocation.parse("dingdongji:comfortable_speed");
    private static final ResourceLocation COMFORTABLE_STEP_ID =
            ResourceLocation.parse("dingdongji:comfortable_step");

    private static void handleComfortable(Player player) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        boolean hasComfort = boots.is(ModItems.ROYAL_STEEL_BOOTS.get());

        AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance stepHeight = player.getAttribute(Attributes.STEP_HEIGHT);

        if (hasComfort) {
            if (moveSpeed != null) {
                moveSpeed.removeModifier(COMFORTABLE_SPEED_ID);
                moveSpeed.addTransientModifier(
                        new AttributeModifier(COMFORTABLE_SPEED_ID, 0.02, AttributeModifier.Operation.ADD_VALUE)
                );
            }
            if (stepHeight != null) {
                stepHeight.removeModifier(COMFORTABLE_STEP_ID);
                stepHeight.addTransientModifier(
                        new AttributeModifier(COMFORTABLE_STEP_ID, 0.5, AttributeModifier.Operation.ADD_VALUE)
                );
            }
        } else {
            if (moveSpeed != null) moveSpeed.removeModifier(COMFORTABLE_SPEED_ID);
            if (stepHeight != null) stepHeight.removeModifier(COMFORTABLE_STEP_ID);
        }
    }

    // ========================================================================
    //  蹈火键位切换（由网络包调用）
    // ========================================================================
    public static void toggleLavaWalker(ServerPlayer player) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!boots.is(ModItems.EMBER_METAL_BOOTS.get())) {
            player.displayClientMessage(
                    Component.literal("[叮咚叽] 未穿戴余烬金属靴子，无法切换蹈火").withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }

        UUID uuid = player.getUUID();
        boolean enabled = LAVA_WALKER_ENABLED.getOrDefault(uuid, false);
        enabled = !enabled;
        LAVA_WALKER_ENABLED.put(uuid, enabled);

        Component msg;
        if (enabled) {
            msg = Component.literal("蹈火：开").withStyle(ChatFormatting.GREEN);
        } else {
            msg = Component.literal("蹈火：关").withStyle(ChatFormatting.RED);
        }
        player.displayClientMessage(msg, true);
    }



    // ========================================================================
    //  余烬头盔：隔热（抗火）
    // ========================================================================
    private static void handleEmberHelmet(Player player) {
        if (player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.EMBER_METAL_HELMET.get())) {
            addHiddenEffect(player, MobEffects.FIRE_RESISTANCE, 0);
        } else if (!player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.TRANSCENDIUM_HELMET.get())) {
            // 超限合金头盔也需要抗火，不在此处移除
            removeEffect(player, MobEffects.FIRE_RESISTANCE);
        }
    }

    // ========================================================================
    //  胸甲抗性统一处理（壁垒 I / 壁垒 II）
    // ========================================================================
    private static void handleEmberChestplate(Player player) {
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.TRANSCENDIUM_CHESTPLATE.get())) {
            addHiddenEffect(player, MobEffects.DAMAGE_RESISTANCE, 3); // 壁垒 II
        } else if (player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.EMBER_METAL_CHESTPLATE.get())) {
            addHiddenEffect(player, MobEffects.DAMAGE_RESISTANCE, 1); // 壁垒 I
        } else {
            removeEffect(player, MobEffects.DAMAGE_RESISTANCE);
        }
    }

    // ========================================================================
    //  余烬护腿：浴火重生（火焰/熔岩中每10秒给予一次生命恢复）
    // ========================================================================
    private static final int EMBER_LEG_HEAL_INTERVAL = 200; // 10秒（200 tick）
    private static final int EMBER_LEG_HEAL_DURATION = 200; // 10秒生命恢复效果

    private static void handleEmberLeggings(Player player) {
        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        UUID uuid = player.getUUID();

        Level level = player.level();
        BlockPos playerPos = player.blockPosition();

        // 先检测是否处于灵魂火/灵魂篝火中（灵魂火优先，效果翻倍）
        boolean inSoulFire = level.getBlockState(playerPos).is(Blocks.SOUL_FIRE)
                || level.getBlockState(playerPos).is(Blocks.SOUL_CAMPFIRE)
                || level.getBlockState(playerPos.below()).is(Blocks.SOUL_CAMPFIRE)
                || level.getBlockState(playerPos.below(2)).is(Blocks.SOUL_CAMPFIRE);
        if (!inSoulFire) {
            for (int dx = -1; dx <= 1 && !inSoulFire; dx++) {
                for (int dz = -1; dz <= 1 && !inSoulFire; dz++) {
                    for (int dy = -2; dy <= 0 && !inSoulFire; dy++) {
                        BlockPos check = playerPos.offset(dx, dy, dz);
                        inSoulFire = level.getBlockState(check).is(Blocks.SOUL_FIRE)
                                || level.getBlockState(check).is(Blocks.SOUL_CAMPFIRE);
                    }
                }
            }
        }

        // 再检测普通火焰（如果已经检测到灵魂火，跳过普通火检测）
        boolean inFire = inSoulFire || player.isInLava()
                || level.getBlockState(playerPos).is(Blocks.FIRE)
                || level.getBlockState(playerPos).is(Blocks.CAMPFIRE)
                || level.getBlockState(playerPos.below()).is(Blocks.CAMPFIRE);
        if (!inFire) {
            for (int dx = -1; dx <= 1 && !inFire; dx++) {
                for (int dz = -1; dz <= 1 && !inFire; dz++) {
                    BlockPos check = playerPos.offset(dx, 0, dz);
                    inFire = level.getBlockState(check).is(Blocks.FIRE)
                            || level.getBlockState(check).is(Blocks.CAMPFIRE);
                }
            }
        }
        if (!inFire) {
            EMBER_LEG_LAST_HEAL_TIME.remove(uuid);
            return;
        }

        if (!leggings.is(ModItems.EMBER_METAL_LEGGINGS.get())) return;

        long now = player.level().getGameTime();
        long lastHealTime = EMBER_LEG_LAST_HEAL_TIME.getOrDefault(uuid, -999L);

        if (now - lastHealTime >= EMBER_LEG_HEAL_INTERVAL) {
            int amplifier = inSoulFire ? 1 : 0;
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, EMBER_LEG_HEAL_DURATION, amplifier, false, false, false));
            EMBER_LEG_LAST_HEAL_TIME.put(uuid, now);
            if (now % 20 < 2) LOGGER.info("[DingDongJi] 浴火重生：给予生命恢复{} (10秒间隔)", amplifier == 1 ? "II" : "I");
        }
    }

    // ========================================================================
    //  余烬靴子：蹈火（炽足兽式岩浆表面行走）
    // ========================================================================
    private static void handleEmberBoots(Player player) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!boots.is(ModItems.EMBER_METAL_BOOTS.get())) {
            return;
        }

        Level level = player.level();
        BlockPos playerPos = player.blockPosition();

        // 多途径检测玩家是否接触岩浆
        boolean onLava = player.isInLava();
        if (!onLava) {
            BlockPos below = playerPos.below();
            onLava = level.getFluidState(below).is(FluidTags.LAVA);
        }
        if (!onLava) {
            onLava = level.getFluidState(playerPos).is(FluidTags.LAVA);
        }
        if (!onLava) {
            BlockPos below2 = playerPos.below(2);
            onLava = level.getFluidState(below2).is(FluidTags.LAVA);
        }

        if (!onLava) return;

        Vec3 currentMotion = player.getDeltaMovement();
        double hSpeed = Math.sqrt(currentMotion.x * currentMotion.x + currentMotion.z * currentMotion.z);
        if (hSpeed > 0.01 && hSpeed < 0.1) {
            double scale = 0.1 / hSpeed;
            player.setDeltaMovement(currentMotion.x * scale, currentMotion.y, currentMotion.z * scale);
        } else {
            player.setDeltaMovement(currentMotion.x * 2.0, currentMotion.y, currentMotion.z * 2.0);
        }
    }

    // ========================================================================
    //  余烬靴子：火焰路径（开启后行走路径产生火焰）
    // ========================================================================
    private static void handleEmberBootsFirePath(Player player) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!boots.is(ModItems.EMBER_METAL_BOOTS.get())) return;
        if (player.level().isClientSide) return;

        // 检查蹈火开关
        if (!LAVA_WALKER_ENABLED.getOrDefault(player.getUUID(), false)) return;

        Level level = player.level();
        BlockPos playerPos = player.blockPosition();

        // 每 2 tick 在脚部位置生成火焰
        // 灵魂沙等非完整方块上行走时，playerPos 返回的是脚下的方块而非脚部位置
        // 因此同时检测 playerPos 和 playerPos.above()
        if (player.tickCount % 2 == 0) {
            if (level.getBlockState(playerPos).isAir()) {
                level.setBlockAndUpdate(playerPos, Blocks.FIRE.defaultBlockState());
            } else if (level.getBlockState(playerPos.above()).isAir()) {
                level.setBlockAndUpdate(playerPos.above(), Blocks.FIRE.defaultBlockState());
            }
        }
    }

    // ========================================================================
    //  皇家钢胸甲：皇家亲和（生命恢复 I）
    // ========================================================================
    private static void handleRoyalSteelChestplate(Player player) {
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.ROYAL_STEEL_CHESTPLATE.get())) {
            addHiddenEffect(player, MobEffects.REGENERATION, 0);
        }
        // 不再在 else 分支移除 REGENERATION，避免误移除应急治愈（V级）和浴火重生的生命恢复效果
        // 皇家亲和的生命恢复会由 addHiddenEffect 的持续时间自然管理
    }

    // ========================================================================
    //  超越合金头盔：适应（夜视 + 水下呼吸 + 敌对发光）
    // ========================================================================
    private static void handleTranscendiumHelmet(Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.is(ModItems.TRANSCENDIUM_HELMET.get())) {
            removeEffect(player, MobEffects.NIGHT_VISION);
            removeEffect(player, MobEffects.WATER_BREATHING);
            return;
        }

        addHiddenEffect(player, MobEffects.NIGHT_VISION, 0);
        addHiddenEffect(player, MobEffects.WATER_BREATHING, 0);
        addHiddenEffect(player, MobEffects.FIRE_RESISTANCE, 0);

        GlowingVisionComponent vision = helmet.get(ModComponents.GLOWING_VISION.get());
        if (vision == null) vision = GlowingVisionComponent.DEFAULT;
        int range = vision.range();

        AABB area = player.getBoundingBox().inflate(range);
        List<LivingEntity> hostiles = player.level().getEntitiesOfClass(
                LivingEntity.class, area,
                e -> e != player && e.isAlive() && isHostileTo(e, player)
        );

        for (LivingEntity living : hostiles) {
            MobEffectInstance existing = living.getEffect(MobEffects.GLOWING);
            if (existing == null || existing.getDuration() <= 100) {
                living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false, true));
            }
        }
    }

    private static boolean isHostileTo(Entity entity, Player player) {
        if (entity instanceof Enemy) return true;
        if (entity instanceof Mob mob && mob.getTarget() == player) return true;
        return false;
    }

    // ========================================================================
    //  超限合金护腿：屏蔽（2格内清除敌对弹射物 + Boss弹飞）
    // ========================================================================
    private static void handleTranscendiumReflect(Player player) {
        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        if (!leggings.is(ModItems.TRANSCENDIUM_LEGGINGS.get())) return;

        Level level = player.level();
        AABB range = player.getBoundingBox().inflate(2.0);

        // 清除弹射物：来自敌对生物 或 将要伤害自身的弹射物
        List<Projectile> projectiles = level.getEntitiesOfClass(
                Projectile.class, range,
                p -> p.isAlive() && isHarmfulProjectile(p, player)
        );
        for (Projectile projectile : projectiles) {
            spawnPurpleParticles(level, projectile.position());
            projectile.discard();
        }

        // 高威胁生物弹飞：贴近2格时弹飞，若1格内停留3秒则关闭弹飞10秒
        AABB closeRange = player.getBoundingBox().inflate(1.0);
        long gameTime = level.getGameTime();

        List<LivingEntity> threats = level.getEntitiesOfClass(
                LivingEntity.class, range,
                e -> e != player && e.isAlive() && isBoss(e)
        );
        for (LivingEntity threat : threats) {
            UUID tid = threat.getUUID();

            // 检查冷却
            Long cooldownEnd = BOSS_REPEL_COOLDOWN.get(tid);
            if (cooldownEnd != null && gameTime < cooldownEnd) continue;

            // 检查是否在1格内
            if (closeRange.contains(threat.position())) {
                Long start = BOSS_PROXIMITY_START.get(tid);
                if (start == null) {
                    BOSS_PROXIMITY_START.put(tid, gameTime);
                } else if (gameTime - start >= PROXIMITY_THRESHOLD) {
                    // 在1格内停留超过3秒 → 冷却10秒
                    BOSS_REPEL_COOLDOWN.put(tid, gameTime + REPEL_COOLDOWN);
                    BOSS_PROXIMITY_START.remove(tid);
                    continue;
                }
            } else {
                BOSS_PROXIMITY_START.remove(tid);
            }

            // 弹飞
            Vec3 knockDir = threat.position().subtract(player.position()).normalize();
            threat.push(knockDir.x * 1.5, 0.1, knockDir.z * 1.5);
            threat.hurtMarked = true;

            // 在玩家周围生成一圈 END_ROD 粒子（中子屏障效果）
            spawnEndRodRing(player, 2.0);
        }

        // 清理不在范围内的Boss冷却状态
        BOSS_PROXIMITY_START.keySet().removeIf(id -> {
            Entity e = ((ServerLevel)level).getEntity(id);
            return e == null || !e.isAlive() || !range.contains(e.position());
        });
        BOSS_REPEL_COOLDOWN.keySet().removeIf(id -> {
            Entity e = ((ServerLevel)level).getEntity(id);
            return e == null || !e.isAlive();
        });
    }

    /** 判断是否为对玩家有害的弹射物 */
    private static boolean isHarmfulProjectile(Projectile projectile, Player player) {
        Entity owner = projectile.getOwner();
        // 没有所有者或所有者为玩家自身 → 无害
        if (owner == player) return false;
        if (owner == null) {
            // 无所有者的弹射物（如自然生成的火焰弹）→ 检查是否朝向玩家
            Vec3 vel = projectile.getDeltaMovement();
            if (vel.lengthSqr() < 0.01) return false;
            Vec3 toPlayer = player.position().subtract(projectile.position()).normalize();
            Vec3 velNorm = vel.normalize();
            return velNorm.dot(toPlayer) > 0.5; // 朝玩家方向
        }
        // 所有者是敌对生物或玩家 → 伤害自身
        if (owner instanceof Enemy) return true;
        if (owner instanceof Mob mob && mob.getTarget() == player) return true;
        if (owner instanceof Player && owner != player) return true;
        return false;
    }

    /** 判断是否为Boss生物（最大生命值 ≥ 150） */
    private static boolean isBoss(LivingEntity entity) {
        return entity.getMaxHealth() >= 150.0;
    }

    private static void spawnPurpleParticles(Level level, Vec3 pos) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.WITCH,
                    pos.x, pos.y + 0.5, pos.z,
                    3, 0.1, 0.1, 0.1, 0
            );
        }
    }

    /** 在玩家周围生成一圈 END_ROD 粒子（中子星蓝色屏障效果） */
    private static void spawnEndRodRing(Player player, double radius) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        double px = player.getX();
        double py = player.getY() + 0.2;
        double pz = player.getZ();
        int count = 24; // 圆环上粒子数量
        for (int i = 0; i < count; i++) {
            double angle = 2.0 * Math.PI * i / count;
            double dx = radius * Math.cos(angle);
            double dz = radius * Math.sin(angle);
            serverLevel.sendParticles(
                    ParticleTypes.END_ROD,
                    px + dx, py, pz + dz,
                    1, 0.0, 0.0, 0.0, 0.01
            );
        }
    }

    // ========================================================================
    //  超限合金靴子：蹈虚（手动开关失重 + 常驻摔落伤害归零）
    // ========================================================================
    private static final ResourceLocation STRIDE_VOID_GRAVITY_ID =
            ResourceLocation.parse("dingdongji:stride_void_gravity");
    private static final ResourceLocation STRIDE_VOID_FALL_ID =
            ResourceLocation.parse("dingdongji:stride_void_fall");

    private static void handleTranscendiumStrideVoid(Player player) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        boolean wearingBoots = boots.is(ModItems.TRANSCENDIUM_BOOTS.get());
        UUID uuid = player.getUUID();

        AttributeInstance gravityAttr = player.getAttribute(Attributes.GRAVITY);
        AttributeInstance fallAttr = player.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER);

        if (!wearingBoots) {
            if (gravityAttr != null) gravityAttr.removeModifier(STRIDE_VOID_GRAVITY_ID);
            if (fallAttr != null) fallAttr.removeModifier(STRIDE_VOID_FALL_ID);
            STRIDE_VOID_MODE.remove(uuid);
            return;
        }

        // ===== 常驻效果：摔落伤害归零 =====
        if (fallAttr != null) {
            fallAttr.removeModifier(STRIDE_VOID_FALL_ID);
            fallAttr.addTransientModifier(
                    new AttributeModifier(STRIDE_VOID_FALL_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            );
        }

        // 双击潜行检测已移除，改为键位切换（见 toggleStrideVoid）

        // ===== 无重力模式时持续保持不掉落 =====
        if (STRIDE_VOID_MODE.getOrDefault(uuid, 0) == 1) {
            Vec3 motion = player.getDeltaMovement();
            if (motion.y < 0) {
                player.setDeltaMovement(motion.x, 0, motion.z);
            }
            player.fallDistance = 0;
        }
    }

    /**
     * 蹈虚键位切换（由网络包调用）。
     * 循环切换重力模式：正常 → 无重力 → 低重力 → 反重力 → 正常
     */
    public static void toggleStrideVoid(ServerPlayer player) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!boots.is(ModItems.TRANSCENDIUM_BOOTS.get())) {
            player.displayClientMessage(
                    Component.literal("[叮咚叽] 未穿戴超限合金靴子，无法切换蹈虚").withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }

        UUID uuid = player.getUUID();
        int mode = STRIDE_VOID_MODE.getOrDefault(uuid, 0);
        mode = (mode + 1) % 4;
        STRIDE_VOID_MODE.put(uuid, mode);

        AttributeInstance gravityAttr = player.getAttribute(Attributes.GRAVITY);

        // 先清除旧的重力修饰符
        if (gravityAttr != null) gravityAttr.removeModifier(STRIDE_VOID_GRAVITY_ID);

        Component msg;
        if (mode == 0) {
            // 正常重力
            player.removeEffect(MobEffects.LEVITATION);
            msg = Component.literal("蹈虚：正常重力").withStyle(ChatFormatting.RED);
        } else if (mode == 1) {
            // 无重力
            if (gravityAttr != null) {
                gravityAttr.addTransientModifier(
                        new AttributeModifier(STRIDE_VOID_GRAVITY_ID, -0.08, AttributeModifier.Operation.ADD_VALUE)
                );
            }
            msg = Component.literal("蹈虚：无重力").withStyle(ChatFormatting.GREEN);
            Vec3 motion = player.getDeltaMovement();
            if (motion.y < 0) {
                player.setDeltaMovement(motion.x, 0, motion.z);
            }
            player.fallDistance = 0;
        } else if (mode == 2) {
            // 低重力
            if (gravityAttr != null) {
                gravityAttr.addTransientModifier(
                        new AttributeModifier(STRIDE_VOID_GRAVITY_ID, -0.07, AttributeModifier.Operation.ADD_VALUE)
                );
            }
            msg = Component.literal("蹈虚：低重力").withStyle(ChatFormatting.YELLOW);
        } else {
            // 反重力：漂浮 II
            player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 6000, 1, false, false, false));
            msg = Component.literal("蹈虚：反重力").withStyle(ChatFormatting.AQUA);
        }
        player.displayClientMessage(msg, true);
    }




    // ========================================================================
    //  铁砧工艺组件赋予
    // ========================================================================

    /** 玩家登录时：扫描背包中所有物品，补齐组件 */
    @SubscribeEvent
    public static void onPlayerLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (!AnvilCraftCompat.isLoaded()) return;

        LOGGER.info("玩家 {} 登录，扫描背包补齐 AnvilCraft 组件", player.getName().getString());

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            applyAnvilCraftComponent(stack);
        }
    }

    /** 装备变更时：为换上的装备补齐组件 */
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (!AnvilCraftCompat.isLoaded()) return;

        ItemStack to = event.getTo();
        if (to.isEmpty()) return;

        applyAnvilCraftComponent(to);
    }

    /** 为物品应用对应的铁砧工艺组件 */
    // ========================================================================
    //  超限合金胸甲：壁垒II（无视魔法/虚空/接触伤害）
    // ========================================================================
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.is(ModItems.TRANSCENDIUM_CHESTPLATE.get())) return;

        DamageSource source = event.getContainer().getSource();
        // 无视：魔法伤害、虚空伤害、接触伤害
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)
                || source.is(DamageTypes.FELL_OUT_OF_WORLD)
                || source.is(DamageTypes.CACTUS) || source.is(DamageTypes.SWEET_BERRY_BUSH)
                || source.is(DamageTypes.THORNS) || source.is(DamageTypes.SONIC_BOOM)
                || source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION)
                || source.is(DamageTypes.BAD_RESPAWN_POINT)) {
            event.getContainer().setNewDamage(0.0f);
        }
    }

    private static void applyAnvilCraftComponent(ItemStack stack) {
        if (stack.is(ModItems.JI_SWORD.get()) || stack.is(ModItems.JI_PICKAXE.get())
                || stack.is(ModItems.JI_HELMET.get()) || stack.is(ModItems.JI_CHESTPLATE.get())
                || stack.is(ModItems.JI_LEGGINGS.get()) || stack.is(ModItems.JI_BOOTS.get())) {
            AnvilCraftCompat.setFortune(stack);
        }

        if (stack.is(ModItems.EMBER_METAL_HELMET.get()) || stack.is(ModItems.EMBER_METAL_CHESTPLATE.get())
                || stack.is(ModItems.EMBER_METAL_LEGGINGS.get()) || stack.is(ModItems.EMBER_METAL_BOOTS.get())) {
            AnvilCraftCompat.setReforging(stack);
        }

        if (stack.is(ModItems.TRANSCENDIUM_HELMET.get()) || stack.is(ModItems.TRANSCENDIUM_CHESTPLATE.get())
                || stack.is(ModItems.TRANSCENDIUM_LEGGINGS.get()) || stack.is(ModItems.TRANSCENDIUM_BOOTS.get())) {
            AnvilCraftCompat.setEternal(stack);
        }
    }
}
