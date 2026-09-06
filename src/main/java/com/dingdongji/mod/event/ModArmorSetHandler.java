package com.dingdongji.mod.event;

import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.item.component.GlowingVisionComponent;
import com.dingdongji.mod.item.component.MeaninglessData;
import com.dingdongji.mod.network.IonocraftBootsFlyingPacket;
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
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.tags.EnchantmentTags;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
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

    /** 获取胸甲应急治愈上次触发时间（供 tooltip 使用）*/
    public static long getChestHealCooldown(UUID uuid) {
        return CHEST_HEAL_COOLDOWN.getOrDefault(uuid, 0L);
    }




    // ===== 蹈火开关状态 =====
    private static final Map<UUID, Boolean> LAVA_WALKER_ENABLED = new HashMap<>();

    // ===== 高亮敌对生物开关 =====
    private static final Map<UUID, Boolean> GLOWING_VISION_ENABLED = new HashMap<>();

    // ===== 舒适开关 =====
    private static final Map<UUID, Boolean> COMFORTABLE_ENABLED = new HashMap<>();

    // ===== 中子屏罩开关（默认关闭）=====
    private static final Map<UUID, Integer> NEUTRON_BARRIER_ENABLED = new HashMap<>();

    // ===== Boss弹飞冷却 =====
    private static final Map<UUID, Long> BOSS_PROXIMITY_START = new HashMap<>(); // Boss UUID → 进入1格时间
    private static final Map<UUID, Long> BOSS_REPEL_COOLDOWN = new HashMap<>();  // Boss UUID → 冷却结束时间
    private static final long PROXIMITY_THRESHOLD = 60;  // 3秒（60 tick）
    private static final long REPEL_COOLDOWN = 100;      // 5秒（100 tick）

    // 浴火重生：上次给予生命恢复的 game time + 上次是否为灵魂火
    static final Map<UUID, Long> EMBER_LEG_LAST_HEAL_TIME = new HashMap<>();
    private static final Map<UUID, Boolean> EMBER_LEG_WAS_SOUL_FIRE = new HashMap<>();
    private static final int EMBER_LEG_HEAL_INTERVAL = 200; // 10秒

    // 超限胸甲紧急恢复冷却
    public static final Map<UUID, Long> CHEST_HEAL_COOLDOWN = new HashMap<>();
    static final Map<UUID, Boolean> CHEST_HEAL_NOTIFIED = new HashMap<>(); // 冷却是否已通知
    public static final long CHEST_HEAL_INTERVAL = 1200; // 1分钟（1200 tick）

    // ===== 模组添加的效果追踪（只移除自己添加的，不影响其他模组）=====
    private static final Map<UUID, Map<Holder<MobEffect>, Boolean>> MOD_ADDED_EFFECTS = new HashMap<>();

    private static void markEffectAdded(Player player, Holder<MobEffect> effect) {
        MOD_ADDED_EFFECTS.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(effect, true);
    }

    // ===== 玩家退出清理 =====
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUUID();
        // 退出前先保存按键状态
        saveToggleStates(player);
        LAVA_WALKER_ENABLED.remove(uuid);
        GLOWING_VISION_ENABLED.remove(uuid);
        COMFORTABLE_ENABLED.remove(uuid);
        NEUTRON_BARRIER_ENABLED.remove(uuid);
        HELMET_MODE.remove(uuid);
        IONOCRAFT_FLYING.remove(uuid);
        IONOCRAFT_GRANTED.remove(uuid);
        IONOCRAFT_SPEED_BOOSTED.remove(uuid);
        IONOCRAFT_FLYING_SYNC.remove(uuid);
        IONOCRAFT_SYNC_LAST_TICK.remove(uuid);
        REPEL_PARTICLE_COOLDOWN.remove(uuid);
        // Boss 冷却按 Boss UUID 索引，不能在单个玩家退出时全局 clear，否则多人会互相打断
        CHEST_HEAL_COOLDOWN.remove(uuid);
        CHEST_HEAL_NOTIFIED.remove(uuid);
        EMBER_LEG_LAST_HEAL_TIME.remove(uuid);
        EMBER_LEG_WAS_SOUL_FIRE.remove(uuid);
        MOD_ADDED_EFFECTS.remove(uuid);
    }

    /** 通过 DataComponent 标记头盔夜视状态（自动同步到客户端，无需网络包）*/
    // setHelmetNightVision 已弃用，保留 DataComponent 注册但不使用，避免同步闪烁

    // ===== 主 Tick =====
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
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
        handleEmberArmorRepair(player);

        // ===== 超限合金套（逐件效果）=====
        handleTranscendiumHelmet(player);
        // 壁垒II 已在 handleEmberChestplate 中统一处理
        handleTranscendiumReflect(player);
        handleMeaninglessConversion(player);


        // 蹈火双击检测已移除，改为键位切换（见 toggleLavaWalker）

        // ===== 超限胸甲：应急恢复检测 + 冷却通知 =====
        UUID uuid = player.getUUID();
        long now = player.level().getGameTime();
        long lastHeal = CHEST_HEAL_COOLDOWN.getOrDefault(uuid, -CHEST_HEAL_INTERVAL); // 初始值为负，首次不延迟

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
            markEffectAdded(player, MobEffects.REGENERATION);
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

            // 弹飞自身3格内的所有实体（范围=护腿2+1=3；力度3=护腿1.5的2倍）
            AABB knockbackArea = player.getBoundingBox().inflate(3.0);
            List<Entity> nearbyEntities = player.level().getEntities(player, knockbackArea, e -> e != player);
            long gt = player.level().getGameTime();
            for (Entity entity : nearbyEntities) {
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

                // 采用护腿排斥敌对生物的大号粒子（比护腿更大一点）
                Vec3 knockDir = new Vec3(dx / dist, 0, dz / dist).normalize();
                spawnRepelBurst(player, entity, knockDir, gt, true);
            }

            // 在脚边生成爆发粒子
            if (player.level() instanceof ServerLevel serverLevel) {
                double px = player.getX();
                double py = player.getY() + 0.2;
                double pz = player.getZ();
                serverLevel.sendParticles(
                        ParticleTypes.END_ROD,
                        px, py, pz,
                        16,
                        2.0, 0.2, 2.0,
                        0.05
                );
            }
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

        // ===== 超限合金靴子：飘升机增强创造飞行 =====
        handleTranscendiumBootsFlight(player);
    }

    // ========================================================================
    //  药水效果辅助
    // ========================================================================

    private static void addHiddenEffect(Player player, Holder<MobEffect> effect, int amplifier) {
        MobEffectInstance existing = player.getEffect(effect);
        if (existing == null || existing.getDuration() < 400) {
            player.addEffect(new MobEffectInstance(effect, EFFECT_DURATION, amplifier, false, false, false));
            markEffectAdded(player, effect);
        }
    }

    /** 只移除模组自己添加的效果，不影响其他来源的同类效果 */
    private static void removeOwnEffect(Player player, Holder<MobEffect> effect) {
        Map<Holder<MobEffect>, Boolean> playerEffects = MOD_ADDED_EFFECTS.get(player.getUUID());
        if (playerEffects == null || !playerEffects.containsKey(effect)) return;

        MobEffectInstance existing = player.getEffect(effect);
        if (existing != null) {
            player.removeEffect(effect);
        }
        playerEffects.remove(effect);
    }

    /** 只移除模组自己添加的、且 amplifier 匹配的效果（避免误删不同等级的同效果） */
    private static void removeOwnEffectAt(Player player, Holder<MobEffect> effect, int amplifier) {
        Map<Holder<MobEffect>, Boolean> playerEffects = MOD_ADDED_EFFECTS.get(player.getUUID());
        if (playerEffects == null || !playerEffects.containsKey(effect)) return;

        MobEffectInstance existing = player.getEffect(effect);
        if (existing != null && existing.getAmplifier() == amplifier) {
            player.removeEffect(effect);
        }
        playerEffects.remove(effect);
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

    public static void toggleComfortable(ServerPlayer player) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!boots.is(ModItems.ROYAL_STEEL_BOOTS.get())) {
            return;
        }

        UUID uuid = player.getUUID();
        boolean enabled = COMFORTABLE_ENABLED.getOrDefault(uuid, false);
        enabled = !enabled;
        COMFORTABLE_ENABLED.put(uuid, enabled);

        player.displayClientMessage(
                Component.literal(String.format("舒适：%s", enabled ? "开" : "关"))
                        .withStyle(ChatFormatting.GREEN),
                true
        );
        saveToggleStates(player);
    }

    private static void handleComfortable(Player player) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        boolean hasComfort = boots.is(ModItems.ROYAL_STEEL_BOOTS.get())
                && COMFORTABLE_ENABLED.getOrDefault(player.getUUID(), false);

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
        saveToggleStates(player);
    }



    // ========================================================================
    //  余烬头盔：隔热（抗火）
    // ========================================================================
    private static void handleEmberHelmet(Player player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.is(ModItems.EMBER_METAL_HELMET.get()) || head.is(ModItems.TRANSCENDIUM_HELMET.get())) {
            addHiddenEffect(player, MobEffects.FIRE_RESISTANCE, 0);
        } else {
            removeOwnEffect(player, MobEffects.FIRE_RESISTANCE);
        }
    }

    // ========================================================================
    //  胸甲抗性统一处理（壁垒 I / 壁垒 II）
    //  即脱即消：脱下胸甲时立即移除本模组添加的抗性
    //  不影响其他来源：只移除自己添加的，不触碰其他模组/食物给的
    //  高等级覆盖：当盔甲自身效果等级更高时覆盖低等级外部效果
    // ========================================================================
    private static void handleEmberChestplate(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);

        if (chest.is(ModItems.TRANSCENDIUM_CHESTPLATE.get())) {
            // 超限胸甲：壁垒II（Resistance IV, amplifier=3）
            applyBarrierEffect(player, 3);
        } else if (chest.is(ModItems.EMBER_METAL_CHESTPLATE.get())) {
            // 余烬胸甲：壁垒I（Resistance II, amplifier=1）
            applyBarrierEffect(player, 1);
        } else {
            // 不穿对应胸甲时，只移除本模组添加的抗性，不影响其他来源
            removeOwnEffect(player, MobEffects.DAMAGE_RESISTANCE);
        }
    }

    /**
     * 胸甲抗性效果应用逻辑：
     * - 无现有抗性 → 添加我们的
     * - 现有抗性等级更低 → 替换为我们的高等级
     * - 现有抗性同等级且是我们添加的 → 续期
     * - 现有抗性等级更高 → 不干预
     */
    private static void applyBarrierEffect(Player player, int amplifier) {
        MobEffectInstance existing = player.getEffect(MobEffects.DAMAGE_RESISTANCE);

        if (existing == null) {
            // 无现有抗性，直接添加
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, EFFECT_DURATION, amplifier, false, false, false));
            markEffectAdded(player, MobEffects.DAMAGE_RESISTANCE);
        } else if (existing.getAmplifier() < amplifier) {
            // 现有抗性等级更低，替换为我们的高等级
            player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, EFFECT_DURATION, amplifier, false, false, false));
            markEffectAdded(player, MobEffects.DAMAGE_RESISTANCE);
        } else if (existing.getAmplifier() == amplifier) {
            // 同等级，检查是否是我们添加的，是则续期
            Map<Holder<MobEffect>, Boolean> playerEffects = MOD_ADDED_EFFECTS.get(player.getUUID());
            boolean isOurs = playerEffects != null && playerEffects.containsKey(MobEffects.DAMAGE_RESISTANCE);
            if (isOurs && existing.getDuration() < 400) {
                player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, EFFECT_DURATION, amplifier, false, false, false));
                markEffectAdded(player, MobEffects.DAMAGE_RESISTANCE);
            }
        }
        // 现有抗性等级更高时，不做任何操作
    }

    // ========================================================================
    //  余烬护腿：浴火重生（火焰/熔岩中每 tick 检测，即时给予/移除效果）
    // ========================================================================
    private static void handleEmberLeggings(Player player) {
        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        UUID uuid = player.getUUID();

        if (!leggings.is(ModItems.EMBER_METAL_LEGGINGS.get())) return;

        Level level = player.level();
        BlockPos playerPos = player.blockPosition();

        // 先检测是否处于灵魂火/灵魂篝火中（灵魂火优先，效果翻倍）
        // 灵魂沙等非完整方块上行走时，playerPos 返回的是脚下的方块而非脚部位置
        // 因此同时检测 playerPos 和 playerPos.above()
        boolean inSoulFire = level.getBlockState(playerPos).is(Blocks.SOUL_FIRE)
                || level.getBlockState(playerPos).is(Blocks.SOUL_CAMPFIRE)
                || level.getBlockState(playerPos.above()).is(Blocks.SOUL_FIRE)
                || level.getBlockState(playerPos.above()).is(Blocks.SOUL_CAMPFIRE)
                || level.getBlockState(playerPos.below()).is(Blocks.SOUL_CAMPFIRE)
                || level.getBlockState(playerPos.below(2)).is(Blocks.SOUL_CAMPFIRE);

        // 再检测普通火焰（如果已经检测到灵魂火，跳过普通火检测）
        // 优化：先快速判断 isOnFire，着火则无需查询普通篝火方块
        boolean inFire = inSoulFire || player.isOnFire();
        if (!inFire) {
            // 未着火时才检查普通篝火（覆盖灵魂沙等边缘情况）
            inFire = level.getBlockState(playerPos).is(Blocks.CAMPFIRE)
                    || level.getBlockState(playerPos.above()).is(Blocks.CAMPFIRE)
                    || level.getBlockState(playerPos.below()).is(Blocks.CAMPFIRE);
        }

        if (inFire) {
            long now = player.level().getGameTime();
            long lastHealTime = EMBER_LEG_LAST_HEAL_TIME.getOrDefault(uuid, -999L);
            boolean wasSoulFire = EMBER_LEG_WAS_SOUL_FIRE.getOrDefault(uuid, false);

            // 火焰类型切换时（灵魂火↔普通火），立即替换效果等级
            if (inSoulFire != wasSoulFire) {
                player.removeEffect(MobEffects.REGENERATION);
                int amplifier = inSoulFire ? 2 : 1;
                // 效果持续时间比刷新间隔长 60 ticks（3秒），避免结束前闪烁
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, EMBER_LEG_HEAL_INTERVAL + 60, amplifier, false, false, false));
                EMBER_LEG_LAST_HEAL_TIME.put(uuid, now);
                EMBER_LEG_WAS_SOUL_FIRE.put(uuid, inSoulFire);
            } else if (now - lastHealTime >= EMBER_LEG_HEAL_INTERVAL) {
                // 10秒间隔刷新
                int amplifier = inSoulFire ? 2 : 1;
                // 效果持续时间比刷新间隔长 60 ticks（3秒），避免结束前闪烁
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, EMBER_LEG_HEAL_INTERVAL + 60, amplifier, false, false, false));
                EMBER_LEG_LAST_HEAL_TIME.put(uuid, now);
                EMBER_LEG_WAS_SOUL_FIRE.put(uuid, inSoulFire);
            }
        } else {
            // 离开火焰：不强制移除，让效果自然结束
            EMBER_LEG_LAST_HEAL_TIME.remove(uuid);
            EMBER_LEG_WAS_SOUL_FIRE.remove(uuid);
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
    //  余烬金属套：火焰中修复耐久（类似重铸效果）
    // ========================================================================
    private static void handleEmberArmorRepair(Player player) {
        // 快速路径：玩家着火或在熔岩中（最常见场景）→ 直接修复，零方块查询
        if (player.isOnFire() || player.isInLava()) {
            repairEmberArmor(player);
            return;
        }

        // 慢速路径：玩家未着火时检查脚下火焰源（灵魂火/灵魂篝火等不触发 isOnFire 的火焰源）
        Level level = player.level();
        BlockPos playerPos = player.blockPosition();
        if (isFireBlock(level, playerPos) || isFireBlock(level, playerPos.above())
                || isFireBlock(level, playerPos.below())) {
            repairEmberArmor(player);
        }
    }

    /** 判断方块是否为火焰类方块（火/灵魂火/篝火/灵魂篝火） */
    private static boolean isFireBlock(Level level, BlockPos pos) {
        BlockState bs = level.getBlockState(pos);
        return bs.is(Blocks.FIRE) || bs.is(Blocks.SOUL_FIRE)
                || bs.is(Blocks.CAMPFIRE) || bs.is(Blocks.SOUL_CAMPFIRE);
    }

    /** 对余烬金属套进行耐久修复（每 4 tick 修复 1 点） */
    private static void repairEmberArmor(Player player) {
        if (player.tickCount % 4 != 0) return;
        ItemStack[] emberPieces = {
            player.getItemBySlot(EquipmentSlot.HEAD),
            player.getItemBySlot(EquipmentSlot.CHEST),
            player.getItemBySlot(EquipmentSlot.LEGS),
            player.getItemBySlot(EquipmentSlot.FEET)
        };
        for (ItemStack piece : emberPieces) {
            if (piece.is(ModItems.EMBER_METAL_HELMET.get()) ||
                piece.is(ModItems.EMBER_METAL_CHESTPLATE.get()) ||
                piece.is(ModItems.EMBER_METAL_LEGGINGS.get()) ||
                piece.is(ModItems.EMBER_METAL_BOOTS.get())) {
                if (piece.isDamaged()) {
                    piece.setDamageValue(Math.max(0, piece.getDamageValue() - 1));
                }
            }
        }
    }

    // ========================================================================
    //  皇家钢胸甲：皇家亲和（生命恢复 I）
    // ========================================================================
    private static void handleRoyalSteelChestplate(Player player) {
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.ROYAL_STEEL_CHESTPLATE.get())) {
            addHiddenEffect(player, MobEffects.REGENERATION, 0);
        } else {
            // 即脱即消：只移除本模组添加的 Regen amplifier=0（皇家钢亲和），
            // 不影响应急治愈(amp=4)和浴火重生(amp=1/2)的 Regen
            removeOwnEffectAt(player, MobEffects.REGENERATION, 0);
        }
    }



    // ========================================================================
    //  超越合金头盔：适应（夜视 + 水下呼吸 + 敌对发光）
    // ========================================================================
    private static void handleTranscendiumHelmet(Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.is(ModItems.TRANSCENDIUM_HELMET.get())) {
            // 没戴头盔时清除模组添加的夜视效果，不影响其他来源
            removeOwnEffect(player, MobEffects.NIGHT_VISION);
            removeOwnEffect(player, MobEffects.WATER_BREATHING);
            return;
        }

        // 持续移除黑暗效果（监守者/潜声等来源的黑暗会被不断清除）
        if (player.hasEffect(MobEffects.DARKNESS)) {
            player.removeEffect(MobEffects.DARKNESS);
        }
        // 头盔着火免疫：允许熄灭已有火焰（mixin 只拦截 >0 的 setRemainingFireTicks）
        if (player.getRemainingFireTicks() > 0) {
            player.clearFire();
        }

        // 从 HELMET_MODE Map 读取夜视状态（与旧版一致，避免 DataComponent 同步导致闪烁）
        int mode = HELMET_MODE.getOrDefault(player.getUUID(), 5);
        if (mode == 2 || mode == 4) {
            addHiddenEffect(player, MobEffects.NIGHT_VISION, 0);
        }

        // 水下呼吸按需应用
        addHiddenEffect(player, MobEffects.WATER_BREATHING, 0);
        // 隐藏抗火效果（火焰伤害免疫 + 岩浆明视由 mixin 补充）
        addHiddenEffect(player, MobEffects.FIRE_RESISTANCE, 0);

        // 检查高亮开关（降频到每20tick扫描一次；GLOWING 持续200tick=10秒，不会断档）
        if (GLOWING_VISION_ENABLED.getOrDefault(player.getUUID(), false)
                && player.tickCount % 20 == 0) {
            GlowingVisionComponent vision = helmet.get(ModComponents.GLOWING_VISION.get());
            if (vision == null) vision = GlowingVisionComponent.DEFAULT;
            int range = vision.range();

            AABB area = player.getBoundingBox().inflate(range);
            List<LivingEntity> hostiles = player.level().getEntitiesOfClass(
                    LivingEntity.class, area,
                    e -> e != player && e.isAlive() && (e instanceof net.minecraft.world.entity.monster.Enemy || (e instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == player)) && isHostileTo(e, player)
            );

            for (LivingEntity living : hostiles) {
                MobEffectInstance existing = living.getEffect(MobEffects.GLOWING);
                if (existing == null || existing.getDuration() <= 100) {
                    living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false, true));
                }
            }
        }
    }

    private static boolean isHostileTo(Entity entity, Player player) {
        if (entity instanceof Enemy) return true;
        if (entity instanceof Mob mob && mob.getTarget() == player) return true;
        return false;
    }

    // ========================================================================
    //  超限合金头盔：高亮敌对生物切换
    // ========================================================================
    // 适应模式切换（高亮/夜视循环）：0=高亮开 1=高亮关 2=夜视开 3=夜视关 4=全开 5=全关
    private static final Map<UUID, Integer> HELMET_MODE = new HashMap<>();

    // 记录 WATER_BREATHING/FIRE_RESISTANCE 是否已应用，同样避免每 tick 重新 addEffect

    private static final ChatFormatting[] HELMET_MODE_COLORS = {
            ChatFormatting.GREEN, ChatFormatting.RED, ChatFormatting.AQUA,
            ChatFormatting.YELLOW, ChatFormatting.LIGHT_PURPLE, ChatFormatting.BLUE
    };

    public static void toggleGlowingVision(ServerPlayer player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.is(ModItems.TRANSCENDIUM_HELMET.get())) return;

        UUID uuid = player.getUUID();
        int nextMode = (HELMET_MODE.getOrDefault(uuid, 5) + 1) % 6;
        HELMET_MODE.put(uuid, nextMode);
        boolean glowingOn = (nextMode == 0 || nextMode == 4);
        boolean nightVisionOn = (nextMode == 2 || nextMode == 4);
        GLOWING_VISION_ENABLED.put(uuid, glowingOn);

        // 通过 HELMET_MODE Map 即时应用/移除药水效果（与旧版一致）
        if (nightVisionOn) {
            addHiddenEffect(player, MobEffects.NIGHT_VISION, 0);
        } else {
            removeOwnEffect(player, MobEffects.NIGHT_VISION);
        }

                String[] names = {
                "\u9002\u5E94\uFF1A\u9AD8\u4EAE\u5F00",
                "\u9002\u5E94\uFF1A\u9AD8\u4EAE\u5173",
                "\u9002\u5E94\uFF1A\u591C\u89C6\u5F00",
                "\u9002\u5E94\uFF1A\u591C\u89C6\u5173",
                "\u9002\u5E94\uFF1A\u5168\u5F00",
                "\u9002\u5E94\uFF1A\u5168\u5173"
        };
        player.displayClientMessage(
                Component.literal(names[nextMode]).withStyle(HELMET_MODE_COLORS[nextMode]), true
        );
        saveToggleStates(player);
    }

    public static void toggleNeutronBarrier(ServerPlayer player) {
        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        if (!leggings.is(ModItems.TRANSCENDIUM_LEGGINGS.get())) {
            return;
        }

        UUID uuid = player.getUUID();
        int state = NEUTRON_BARRIER_ENABLED.getOrDefault(uuid, 0);
        state = (state + 1) % 4;
        NEUTRON_BARRIER_ENABLED.put(uuid, state);

        switch (state) {
            case 0 -> player.displayClientMessage(
                    Component.literal("中子屏罩：关闭屏蔽").withStyle(ChatFormatting.BLUE), true);
            case 1 -> player.displayClientMessage(
                    Component.literal("中子屏罩：屏蔽敌对生物").withStyle(ChatFormatting.GREEN), true);
            case 2 -> player.displayClientMessage(
                    Component.literal("中子屏罩：屏蔽弹射物").withStyle(ChatFormatting.AQUA), true);
            case 3 -> player.displayClientMessage(
                    Component.literal("中子屏罩：全部屏蔽").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        saveToggleStates(player);
    }

    // ========================================================================
    //  浮霜金属套：无义——将非诅咒附魔转换为护甲值与盔甲韧性
    // ========================================================================
    private static final ResourceLocation MEANINGLESS_ARMOR_ID =
            ResourceLocation.fromNamespaceAndPath("dingdongji", "meaningless_armor");
    private static final ResourceLocation MEANINGLESS_TOUGHNESS_ID =
            ResourceLocation.fromNamespaceAndPath("dingdongji", "meaningless_toughness");

    private static void handleMeaninglessConversion(Player player) {
        // 扫描玩家所有背包槽位（主手+背包+盔甲+副手），参考铁砧工艺无情
        for (ItemStack stack : player.getInventory().items) {
            tryConvertFrostMetalPiece(player, stack);
        }
        for (ItemStack stack : player.getInventory().armor) {
            tryConvertFrostMetalPiece(player, stack);
        }
        tryConvertFrostMetalPiece(player, player.getOffhandItem());
    }

    private static void tryConvertFrostMetalPiece(Player player, ItemStack stack) {
        ArmorItem.Type type = null;
        if (stack.is(ModItems.FROST_METAL_HELMET.get())) type = ArmorItem.Type.HELMET;
        else if (stack.is(ModItems.FROST_METAL_CHESTPLATE.get())) type = ArmorItem.Type.CHESTPLATE;
        else if (stack.is(ModItems.FROST_METAL_LEGGINGS.get())) type = ArmorItem.Type.LEGGINGS;
        else if (stack.is(ModItems.FROST_METAL_BOOTS.get())) type = ArmorItem.Type.BOOTS;
        else return;

        convertPiece(player, stack, type);
    }

    private static void convertPiece(Player player, ItemStack stack, ArmorItem.Type armorType) {
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) return;

        // 获取已转换的附魔记录
        ItemEnchantments convertedEnchs = stack.getOrDefault(ModComponents.MEANINGLESS_DATA.get(),
                MeaninglessData.EMPTY).convertedEnchantments();

        int newLevels = 0;
        ItemEnchantments.Mutable convertedEnchsMut = new ItemEnchantments.Mutable(convertedEnchs);

        // 转换所有附魔（包括诅咒）
        for (Holder<Enchantment> enchantment : enchantments.keySet()) {
            int level = enchantments.getLevel(enchantment);
            if (convertedEnchs.getLevel(enchantment) >= level) {
                continue;
            }
            int addLevels = level - convertedEnchs.getLevel(enchantment);
            if (addLevels > 0) {
                newLevels += addLevels;
            }
            convertedEnchsMut.set(enchantment, level);
        }

        if (newLevels == 0) return;

        // 计算总等级
        int totalLevels = 0;
        for (Holder<Enchantment> e : convertedEnchsMut.keySet()) {
            totalLevels += convertedEnchsMut.getLevel(e);
        }

        // 记录附魔到 MEANINGLESS_DATA 供属性加成和 tooltip 使用
        // 注意：不清除 minecraft:enchantments，确保锻造台配方能继承附魔
        stack.set(ModComponents.MEANINGLESS_DATA.get(), new MeaninglessData(totalLevels, convertedEnchsMut.toImmutable()));

        // 计算护甲加成
        float bonusArmor = (float) Math.round(Math.sqrt(totalLevels) * 1.5 + totalLevels / 5.0);
        float bonusToughness = totalLevels / 3.0f;

        // 构建属性修饰符
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        if (bonusArmor > 0) {
            builder.add(
                    Attributes.ARMOR,
                    new AttributeModifier(MEANINGLESS_ARMOR_ID, bonusArmor, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.bySlot(armorType.getSlot())
            );
        }
        if (bonusToughness > 0) {
            builder.add(
                    Attributes.ARMOR_TOUGHNESS,
                    new AttributeModifier(MEANINGLESS_TOUGHNESS_ID, bonusToughness, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.bySlot(armorType.getSlot())
            );
        }
        // 保留基础属性修饰符
        for (ItemAttributeModifiers.Entry entry : stack.getAttributeModifiers().modifiers()) {
            if (!entry.modifier().is(MEANINGLESS_ARMOR_ID) && !entry.modifier().is(MEANINGLESS_TOUGHNESS_ID)) {
                builder.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
    }


    // ========================================================================
    //  超限合金护腿：中子屏罩（常驻清除弹射物 + 按Z键切换弹飞）
    // ========================================================================
    private static void handleTranscendiumReflect(Player player) {
        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        if (!leggings.is(ModItems.TRANSCENDIUM_LEGGINGS.get())) return;

        Level level = player.level();
        Vec3 center = player.position();
        double shieldRadius = 3.0;
        long gameTime = level.getGameTime();

        // ===== 屏蔽弹射物（受中子屏罩状态控制）=====
        int __bs = NEUTRON_BARRIER_ENABLED.getOrDefault(player.getUUID(), 0);
        if (__bs == 2 || __bs == 3) {
            List<Projectile> projectiles = level.getEntitiesOfClass(
                    Projectile.class, player.getBoundingBox().inflate(shieldRadius),
                    p -> p.isAlive() && p.distanceToSqr(center) <= shieldRadius * shieldRadius
                            && isHarmfulProjectile(p, player)
            );
            for (Projectile projectile : projectiles) {
                spawnAbsorptionRipple(level, projectile.position(), player);
                projectile.discard();
            }
        }

        // ===== 开关：弹飞所有实体（按Z键切换）=====
        int __state = NEUTRON_BARRIER_ENABLED.getOrDefault(player.getUUID(), 0);
        if (__state != 1 && __state != 3) return;

        AABB range = player.getBoundingBox().inflate(2.0, 2.0, 2.0).expandTowards(0, -3, 0);
        List<Entity> threats = level.getEntities(
                player, range,
                e -> e != player && e.isAlive() && (e instanceof net.minecraft.world.entity.monster.Enemy || (e instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == player))
        );
        for (Entity threat : threats) {
            UUID tid = threat.getUUID();

            // 检查弹飞冷却
            Long cooldownEnd = BOSS_REPEL_COOLDOWN.get(tid);
            if (cooldownEnd != null && gameTime < cooldownEnd) continue;

            // 弹飞
            Vec3 knockDir = threat.position().subtract(player.position()).normalize();
            threat.push(knockDir.x * 1.5, 0.1, knockDir.z * 1.5);
            threat.hurtMarked = true;

            // 冲击波爆发 + 拖尾（护腿用普通大小粒子）
            spawnRepelBurst(player, threat, knockDir, gameTime, false);
        }

        // 清理不在范围内的Boss冷却状态和粒子冷却
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
        if (owner instanceof Player && owner != player) {
            // 仅清除原版弹射物，模组弹射物放过（如钩爪等工具类弹射物）
            if (projectile.getClass().getName().startsWith("net.minecraft.")) return true;
            return false;
        }
        return false;
    }

    /** 判断是否为Boss生物（最大生命值 ≥ 150） */
    private static boolean isBoss(LivingEntity entity) {
        return entity.getMaxHealth() >= 150.0;
    }

    /** 清除弹射物时：弹射物位置的女巫紫色爆裂粒子 */
    private static void spawnAbsorptionRipple(Level level, Vec3 pos, Player player) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        // 弹射物位置：新粒子，从小变大，2s 消失
        serverLevel.sendParticles(
                com.dingdongji.mod.init.ModParticles.NEUTRON_BARRIER_ABSORB.get(),
                pos.x, pos.y + 0.5, pos.z,
                1, 0, 0, 0, 0
        );
    }

    /**
     * 弹飞生物时：从玩家胸腔处生成一个平躺的屏障粒子，
     * 粒子会同时放大、淡出、下坠到地面（具体动画在 NeutronBarrierParticle.tick）。
     */
    /** 屏蔽敌对生物粒子的生成冷却（tick），1s 一次 */
    private static final java.util.Map<UUID, Long> REPEL_PARTICLE_COOLDOWN = new java.util.HashMap<>();

    private static void spawnRepelBurst(Player player, Entity threat, Vec3 knockDir, long gameTime, boolean big) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        // 每 1s 产生一次，避免一直屏蔽一直触发粒子
        UUID uid = player.getUUID();
        Long last = REPEL_PARTICLE_COOLDOWN.get(uid);
        if (last != null && gameTime - last < 20) return; // 1s (20 tick) 冷却
        REPEL_PARTICLE_COOLDOWN.put(uid, gameTime);

        // 从玩家胸腔处生成粒子；big=true 时用更大的排斥粒子（胸甲应急治愈用）
        net.minecraft.core.particles.SimpleParticleType type =
                big ? com.dingdongji.mod.init.ModParticles.NEUTRON_BARRIER_REPEL_BIG.get()
                    : com.dingdongji.mod.init.ModParticles.NEUTRON_BARRIER_REPEL.get();
        Vec3 chest = player.position().add(0, 1.5, 0);
        serverLevel.sendParticles(type, chest.x, chest.y, chest.z, 1, 0, 0, 0, 0);
    }



        // ========================================================================
    //  超限合金全套：偏执（根据已有魔咒的等级提升护甲值和盔甲韧性）
    // ========================================================================
    // 偏执已移至 ModEvents.onItemAttributeModifier（ItemAttributeModifierEvent 方式，
    // 每次属性计算时动态添加，附魔变化自动触发重算）

    // ========================================================================
    //  超限合金靴子：蹈虚（穿戴后即可创造飞行，按键可开关；与飘升机同时穿戴加速）
    // ========================================================================
    private static final Map<UUID, Boolean> IONOCRAFT_FLYING = new HashMap<>();
    /** 本 tick 周期内是否由本 mod 授予了 mayfly，脱靴时只清这一次 */
    private static final Map<UUID, Boolean> IONOCRAFT_GRANTED = new HashMap<>();

    private static final float DEFAULT_FLY_SPEED = 0.05f;
    private static final float BOOSTED_FLY_SPEED = 0.1f;   // 飘升机+增强靴子 = 2倍飞行速度
    private static final Map<UUID, Boolean> IONOCRAFT_SPEED_BOOSTED = new HashMap<>();

    private static void handleTranscendiumBootsFlight(Player player) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        boolean wearingBoots = boots.is(ModItems.TRANSCENDIUM_BOOTS.get());
        UUID uuid = player.getUUID();

        if (!wearingBoots) {
            // 脱下超限靴子：仅清理本 mod 自己的蹈虚/加速内部状态。
            // 【重要】绝不在未穿超限靴子时主动关闭 mayfly/flying：
            // 飞行能力可能来自飘升机(AnvilCraft)或其他附属模组，本 mod 不应干预，
            // 否则会误关其他模组的飘升机飞行。
            // 开关偏好保留在 IONOCRAFT_FLYING 中，下次穿上仍按上次选择恢复。
            // 只用 GRANTED 判断「本 mod 是否正在授予飞行」，避免每 tick 误关其他模组。
            boolean granted = IONOCRAFT_GRANTED.remove(uuid) == Boolean.TRUE;
            IONOCRAFT_SPEED_BOOSTED.remove(uuid);
            boolean needUpdate = false;
            // 仅当飞行速度确实等于本 mod 的加速值(0.1)时还原为默认(0.05)，
            // 不触碰其他来源设置的飞行速度。
            if (Math.abs(player.getAbilities().getFlyingSpeed() - BOOSTED_FLY_SPEED) < 1.0E-4f) {
                player.getAbilities().setFlyingSpeed(DEFAULT_FLY_SPEED);
                needUpdate = true;
            }
            // 仅关闭本 mod 自己开启的蹈虚飞行；飘升机仍有电时保留其飞行能力
            boolean hasIonocraft = AnvilCraftCompat.hasActiveIonocraftBackpack(player);
            if (granted && !hasIonocraft && !player.isCreative() && !player.isSpectator()) {
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
            syncIonocraftFlyingState(player, false);
            return;
        }

        // 默认开启：穿上即可飞行（与飘升机背包一致），无需每次进游戏按快捷键。
        // computeIfAbsent 把偏好写入 map，脱靴时才能正确关掉本 mod 开的飞行。
        boolean shouldFly = IONOCRAFT_FLYING.computeIfAbsent(uuid, k -> true);
        boolean isCreative = player.isCreative();
        boolean isSpectator = player.isSpectator();
        // 飘升机是否有电（有电时由飘升机自身供能/出粒子，蹈虚作为速度增强）
        boolean hasIonocraft = AnvilCraftCompat.hasActiveIonocraftBackpack(player);

        // ====== 飘升机没电 + 蹈虚未开启 + 玩家仍处飞行状态（飘升机刚耗尽电量）→ 蹈虚自动接管 ======
        // 不关闭创造飞行状态，将创造飞行灵活转移至蹈虚上；粒子随之切到蹈虚。
        if (!hasIonocraft && !shouldFly && !isCreative && !isSpectator
                && (player.getAbilities().flying || player.getAbilities().mayfly)) {
            IONOCRAFT_FLYING.put(uuid, true);
            shouldFly = true;
        }

        // 决定是否允许创造飞行：蹈虚开启 或 飘升机有电（两者任一供能则保持 mayfly）
        boolean wantFly = shouldFly || hasIonocraft;
        if (wantFly && !player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        } else if (!wantFly && player.getAbilities().mayfly && !isCreative) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.getAbilities().setFlyingSpeed(DEFAULT_FLY_SPEED);
            player.onUpdateAbilities();
        }
        if (shouldFly) {
            IONOCRAFT_GRANTED.put(uuid, true);
        } else {
            IONOCRAFT_GRANTED.remove(uuid);
        }

        // ====== 飞行状态同步（复刻飘升机：广播蹈虚飞行状态到所有客户端）======
        // 蹈虚粒子仅在「蹈虚开启 且 飘升机没电」时喷：
        // 飘升机有电时用飘升机自身的粒子；关闭蹈虚时同样改用飘升机粒子。
        boolean nowFlying = shouldFly && !hasIonocraft
                && player.getAbilities().flying && !isCreative && !isSpectator;
        syncIonocraftFlyingState(player, nowFlying);

        // ====== 飞行速度提升：蹈虚开启 + 飘升机有电 = 2倍 ======
        if (shouldFly && player.getAbilities().flying) {
            boolean wasBoosted = IONOCRAFT_SPEED_BOOSTED.getOrDefault(uuid, false);

            if (hasIonocraft && !wasBoosted) {
                player.getAbilities().setFlyingSpeed(BOOSTED_FLY_SPEED);
                player.onUpdateAbilities();
                IONOCRAFT_SPEED_BOOSTED.put(uuid, true);
            } else if (!hasIonocraft && wasBoosted) {
                player.getAbilities().setFlyingSpeed(DEFAULT_FLY_SPEED);
                player.onUpdateAbilities();
                IONOCRAFT_SPEED_BOOSTED.put(uuid, false);
            }
        } else {
            if (IONOCRAFT_SPEED_BOOSTED.getOrDefault(uuid, false)) {
                player.getAbilities().setFlyingSpeed(DEFAULT_FLY_SPEED);
                player.onUpdateAbilities();
                IONOCRAFT_SPEED_BOOSTED.put(uuid, false);
            }
        }
    }

    private static final Map<UUID, Boolean> IONOCRAFT_FLYING_SYNC = new HashMap<>();
    // 记录上次广播时刻，用于周期性重发（解决跨维度/新进入视野时收不到状态包的问题）
    private static final Map<UUID, Integer> IONOCRAFT_SYNC_LAST_TICK = new HashMap<>();
    private static final int IONOCRAFT_SYNC_INTERVAL = 20; // 每20tick(1秒)重发一次

    /**
     * 复刻飘升机：服务端检测蹈虚飞行状态变化，广播 IonocraftBootsFlyingPacket 给追踪玩家。
     * 状态变化时立即广播；持续飞行时周期性重发，确保跨维度/新进入视野的玩家也能收到。
     */
    private static void syncIonocraftFlyingState(Player player, boolean nowFlying) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        UUID uuid = player.getUUID();
        int tick = serverPlayer.getServer() != null ? serverPlayer.getServer().getTickCount() : 0;
        Boolean prev = IONOCRAFT_FLYING_SYNC.put(uuid, nowFlying);
        Integer lastTick = IONOCRAFT_SYNC_LAST_TICK.get(uuid);

        if (prev == null || prev != nowFlying) {
            // 状态变化：立即广播（含自己，第三人称粒子才稳定）
            IONOCRAFT_SYNC_LAST_TICK.put(uuid, tick);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                    serverPlayer,
                    new IonocraftBootsFlyingPacket(serverPlayer.getId(), nowFlying)
            );
        } else if (nowFlying && (lastTick == null || tick - lastTick >= IONOCRAFT_SYNC_INTERVAL)) {
            // 持续飞行：周期性重发，保证新客户端能收到
            IONOCRAFT_SYNC_LAST_TICK.put(uuid, tick);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                    serverPlayer,
                    new IonocraftBootsFlyingPacket(serverPlayer.getId(), true)
            );
        }
    }

    public static void toggleIonocraftFlight(ServerPlayer player) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!boots.is(ModItems.TRANSCENDIUM_BOOTS.get())) {
            return;
        }

        UUID uuid = player.getUUID();
        boolean nowFlying = !IONOCRAFT_FLYING.getOrDefault(uuid, true);
        IONOCRAFT_FLYING.put(uuid, nowFlying);

        if (nowFlying) {
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
        } else {
            // 若背后/饰品栏有飘升机，保留飘升机自身的飞行能力，仅关闭本 mod 的加速
            boolean hasIonocraft = AnvilCraftCompat.hasActiveIonocraftBackpack(player);
            if (!hasIonocraft) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
            }
            player.getAbilities().setFlyingSpeed(DEFAULT_FLY_SPEED);
            IONOCRAFT_SPEED_BOOSTED.put(uuid, false);
        }
        player.onUpdateAbilities();

        player.displayClientMessage(
                Component.literal("\u8E48\u865A\uFF1A" + (nowFlying ? "\u00a7a\u5F00\u542F\u98DE\u884C" : "\u00a7c\u5173\u95ED\u98DE\u884C")),
                true
        );
        saveToggleStates(player);
    }

// ========================================================================
    //  铁砧工艺组件赋予
    // ========================================================================

    /** 玩家登录时：恢复按键状态 */
    @SubscribeEvent
    public static void onPlayerLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        // 恢复按键状态（夜视/高亮/蹈火/舒适/中子屏罩/蹈虚模式）
        loadToggleStates(player);
        // tick 续期会在下一个 tick 自动根据 HELMET_MODE 恢复夜视效果
    }

    /** 装备变更时：只补齐铁砧工艺组件（旧版逻辑，完全不碰效果）*/
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

        DamageSource source = event.getContainer().getSource();

        // 超限合金靴子：永久免疫摔落伤害（穿靴子时任何方式摔落都不受伤，
        // 但保留正常下落判定，不影响跳跃/踩耕地/落地方块声音等事件）
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (boots.is(ModItems.TRANSCENDIUM_BOOTS.get()) && source.is(DamageTypes.FALL)) {
            event.getContainer().setNewDamage(0.0f);
            return;
        }

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.is(ModItems.TRANSCENDIUM_CHESTPLATE.get())) return;

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

    // ========================================================================
    //  按键状态持久化（跨游戏会话保存）
    // ========================================================================

    private static void saveToggleStates(Player player) {
        var data = player.getPersistentData();
        UUID uuid = player.getUUID();
        data.putInt("dingdongji:helmet_mode", HELMET_MODE.getOrDefault(uuid, 5));
        data.putBoolean("dingdongji:lava_walker", LAVA_WALKER_ENABLED.getOrDefault(uuid, false));
        data.putBoolean("dingdongji:comfortable", COMFORTABLE_ENABLED.getOrDefault(uuid, false));
        data.putInt("dingdongji:neutron_barrier", NEUTRON_BARRIER_ENABLED.getOrDefault(uuid, 0));
        data.putBoolean("dingdongji:ionocraft_flying", IONOCRAFT_FLYING.getOrDefault(uuid, true));
    }

    private static void loadToggleStates(Player player) {
        var data = player.getPersistentData();
        UUID uuid = player.getUUID();

        int helmetMode = data.contains("dingdongji:helmet_mode") ? data.getInt("dingdongji:helmet_mode") : 5;
        HELMET_MODE.put(uuid, helmetMode);
        // 高亮开关以头盔模式为准，避免 mode=0(高亮开) 与 glowing=false 不同步
        GLOWING_VISION_ENABLED.put(uuid, helmetMode == 0 || helmetMode == 4);

        LAVA_WALKER_ENABLED.put(uuid, data.getBoolean("dingdongji:lava_walker"));
        COMFORTABLE_ENABLED.put(uuid, data.getBoolean("dingdongji:comfortable"));
        NEUTRON_BARRIER_ENABLED.put(uuid, data.getInt("dingdongji:neutron_barrier"));

        if (data.contains("dingdongji:ionocraft_flying")) {
            IONOCRAFT_FLYING.put(uuid, data.getBoolean("dingdongji:ionocraft_flying"));
        } else {
            IONOCRAFT_FLYING.put(uuid, true);
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
