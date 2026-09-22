package com.dingdongji.mod.event;

import com.dingdongji.mod.client.ClientAbilityState;
import com.dingdongji.mod.init.ModParticles;
import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.item.component.GlowingVisionComponent;
import com.dingdongji.mod.item.component.MeaninglessData;
import com.dingdongji.mod.mixin.EntityAirDataAccessor;
import com.dingdongji.mod.network.AbilityStateSyncPacket;
import com.dingdongji.mod.network.IonocraftBootsFlyingPacket;
import com.dingdongji.mod.util.AnvilCraftCompat;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemAttributeModifiers.Builder;
import net.minecraft.world.item.component.ItemAttributeModifiers.Entry;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments.Mutable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.dingdongji.mod.mixin.LivingEntityJumpingAccessor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

public class ModArmorSetHandler {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int EFFECT_DURATION = 6000;
   private static final Map<UUID, Boolean> LAVA_WALKER_ENABLED = new HashMap<>();
   private static final Map<UUID, Boolean> FROST_SLIDE_ENABLED = new HashMap<>();
   private static final Map<UUID, Boolean> SPECTRAL_PHASE_ENABLED = new HashMap<>();
   private static final Map<UUID, Boolean> GLOWING_VISION_ENABLED = new HashMap<>();
   private static final Map<UUID, Boolean> COMFORTABLE_ENABLED = new HashMap<>();
   private static final Map<UUID, Integer> NEUTRON_BARRIER_ENABLED = new HashMap<>();
   private static final Map<UUID, Long> BOSS_PROXIMITY_START = new HashMap<>();
   private static final Map<UUID, Long> BOSS_REPEL_COOLDOWN = new HashMap<>();
   private static final long PROXIMITY_THRESHOLD = 60L;
   private static final long REPEL_COOLDOWN = 100L;
   static final Map<UUID, Long> EMBER_LEG_LAST_HEAL_TIME = new HashMap<>();
   private static final Map<UUID, Boolean> EMBER_LEG_WAS_SOUL_FIRE = new HashMap<>();
   private static final int EMBER_LEG_HEAL_INTERVAL = 200;
   public static final Map<UUID, Long> CHEST_HEAL_COOLDOWN = new HashMap<>();
   static final Map<UUID, Boolean> CHEST_HEAL_NOTIFIED = new HashMap<>();
   public static final long CHEST_HEAL_INTERVAL = 1200L;
   private static final Map<UUID, Map<Holder<MobEffect>, Boolean>> MOD_ADDED_EFFECTS = new HashMap<>();
   private static final ResourceLocation JI_SET_BREAK_SPEED_ID = ResourceLocation.parse("dingdongji:ji_set_break_speed");
   private static final ResourceLocation JI_SET_REACH_ID = ResourceLocation.parse("dingdongji:ji_set_reach");
   private static final ResourceLocation COMFORTABLE_SPEED_ID = ResourceLocation.parse("dingdongji:comfortable_speed");
   private static final ResourceLocation COMFORTABLE_STEP_ID = ResourceLocation.parse("dingdongji:comfortable_step");
   private static final double VOID_WALK_Y = -64.0;
   private static final double SURFACE_TOLERANCE = 0.1;
   private static final Set<UUID> FLUID_SWIM = new HashSet<>();
   private static final Map<UUID, Integer> HELMET_MODE = new HashMap<>();
   private static final ChatFormatting[] HELMET_MODE_COLORS = new ChatFormatting[]{
      ChatFormatting.GREEN, ChatFormatting.RED, ChatFormatting.AQUA, ChatFormatting.YELLOW, ChatFormatting.LIGHT_PURPLE, ChatFormatting.BLUE
   };
   private static final ResourceLocation MEANINGLESS_ARMOR_ID = ResourceLocation.fromNamespaceAndPath("dingdongji", "meaningless_armor");
   private static final ResourceLocation MEANINGLESS_TOUGHNESS_ID = ResourceLocation.fromNamespaceAndPath("dingdongji", "meaningless_toughness");
   private static final Map<UUID, Long> REPEL_PARTICLE_COOLDOWN = new HashMap<>();
   public static final int FLIGHT_OFF = 0;
   public static final int FLIGHT_NORMAL = 1;
   public static final int FLIGHT_SUPER = 2;
   private static final Map<UUID, Integer> IONOCRAFT_FLIGHT_MODE = new HashMap<>();
   private static final Map<UUID, Boolean> IONOCRAFT_GRANTED = new HashMap<>();
   private static final float DEFAULT_FLY_SPEED = 0.05F;
   private static final float SUPER_FLY_SPEED = 0.1F;
   private static final ResourceLocation TRANS_FLUID_EFFICIENCY_ID = ResourceLocation.parse("dingdongji:trans_fluid_efficiency");
   private static final ResourceLocation TRANS_MOVEMENT_EFFICIENCY_ID = ResourceLocation.parse("dingdongji:trans_movement_efficiency");
   private static final Map<UUID, Integer> IONOCRAFT_FLYING_SYNC = new HashMap<>();
   private static final Map<UUID, Integer> IONOCRAFT_SYNC_LAST_TICK = new HashMap<>();
   private static final int IONOCRAFT_SYNC_INTERVAL = 20;

   public static long getChestHealCooldown(UUID uuid) {
      return CHEST_HEAL_COOLDOWN.getOrDefault(uuid, 0L);
   }

   private static Component actionMessage(String title, String suffix, ChatFormatting suffixColor) {
      return Component.empty().append(Component.literal(title + "：").withStyle(ChatFormatting.WHITE)).append(Component.literal(suffix).withStyle(suffixColor));
   }

   private static void markEffectAdded(Player player, Holder<MobEffect> effect) {
      MOD_ADDED_EFFECTS.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(effect, true);
   }

   @SubscribeEvent
   public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
      Player player = event.getEntity();
      UUID uuid = player.getUUID();
      saveToggleStates(player);
      LAVA_WALKER_ENABLED.remove(uuid);
      FROST_SLIDE_ENABLED.remove(uuid);
      SPECTRAL_PHASE_ENABLED.remove(uuid);
      FLUID_SWIM.remove(uuid);
      GLOWING_VISION_ENABLED.remove(uuid);
      COMFORTABLE_ENABLED.remove(uuid);
      NEUTRON_BARRIER_ENABLED.remove(uuid);
      HELMET_MODE.remove(uuid);
      IONOCRAFT_FLIGHT_MODE.remove(uuid);
      IONOCRAFT_GRANTED.remove(uuid);
      IONOCRAFT_FLYING_SYNC.remove(uuid);
      IONOCRAFT_SYNC_LAST_TICK.remove(uuid);
      REPEL_PARTICLE_COOLDOWN.remove(uuid);
      CHEST_HEAL_COOLDOWN.remove(uuid);
      CHEST_HEAL_NOTIFIED.remove(uuid);
      EMBER_LEG_LAST_HEAL_TIME.remove(uuid);
      EMBER_LEG_WAS_SOUL_FIRE.remove(uuid);
      MOD_ADDED_EFFECTS.remove(uuid);
   }

   @SubscribeEvent
   public static void onPlayerTick(Pre event) {
      Player player = event.getEntity();
      handleSurfaceWalking(player);
      if (!player.level().isClientSide) {
         handleSpectralPhase(player);
         handleJiSet(player);
         handleComfortable(player);
         handleRoyalSteelChestplate(player);
         handleEmberChestplate(player);
         handleEmberLeggings(player);
         handleEmberArmorRepair(player);
         handleEmberBootsFirePath(player);
         handleTranscendiumHelmet(player);
         handleTranscendiumReflect(player);
         handleMeaninglessConversion(player);
         UUID uuid = player.getUUID();
         long now = player.level().getGameTime();
         long lastHeal = CHEST_HEAL_COOLDOWN.getOrDefault(uuid, -1200L);
         ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
         if (chest.is((Item)ModItems.TRANSCENDIUM_CHESTPLATE.get()) && player.getHealth() < 10.0F && now >= lastHeal + 1200L) {
            for (MobEffectInstance activeEffect : new ArrayList<>(player.getActiveEffects())) {
               if (!((MobEffect)activeEffect.getEffect().value()).isBeneficial()) {
                  player.removeEffect(activeEffect.getEffect());
               }
            }

            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 4, false, false, false));
            markEffectAdded(player, MobEffects.REGENERATION);
            CHEST_HEAL_COOLDOWN.put(uuid, now);
            CHEST_HEAL_NOTIFIED.put(uuid, false);
            player.displayClientMessage(Component.literal(">>>生命值低！已启用应急治愈，清除所有负面效果！").withStyle(ChatFormatting.RED), true);
            if (player.level() instanceof ServerLevel serverLevel) {
               double px = player.getX();
               double py = player.getY() + 0.5;
               double pz = player.getZ();
               serverLevel.sendParticles(ParticleTypes.POOF, px, py, pz, 40, 1.2, 1.0, 1.2, 0.05);
            }

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

               double forceX = dx / dist * 3.0;
               double forceZ = dz / dist * 3.0;
               entity.setDeltaMovement(entity.getDeltaMovement().add(forceX, 0.4, forceZ));
               entity.hurtMarked = true;
               Vec3 knockDir = new Vec3(dx / dist, 0.0, dz / dist).normalize();
               spawnRepelBurst(player, entity, knockDir, gt, true);
            }

            if (player.level() instanceof ServerLevel serverLevel) {
               double px = player.getX();
               double py = player.getY() + 0.2;
               double pz = player.getZ();
               serverLevel.sendParticles(ParticleTypes.END_ROD, px, py, pz, 16, 2.0, 0.2, 2.0, 0.05);
            }
         }

         boolean notified = CHEST_HEAL_NOTIFIED.getOrDefault(uuid, true);
         long actualLastHeal = CHEST_HEAL_COOLDOWN.getOrDefault(uuid, 0L);
         if (!notified && now >= actualLastHeal + 1200L) {
            CHEST_HEAL_NOTIFIED.put(uuid, true);
            player.displayClientMessage(Component.literal(">>>应急治愈已冷却完毕！").withStyle(ChatFormatting.AQUA), true);
         }

         handleTranscendiumBootsFlight(player);
         handleTranscendiumFluidEfficiency(player);
      }
   }

   private static void addHiddenEffect(Player player, Holder<MobEffect> effect, int amplifier) {
      MobEffectInstance existing = player.getEffect(effect);
      if (existing == null || existing.getDuration() < 400) {
         player.addEffect(new MobEffectInstance(effect, 6000, amplifier, false, false, false));
         markEffectAdded(player, effect);
      }
   }

   private static void removeOwnEffect(Player player, Holder<MobEffect> effect) {
      Map<Holder<MobEffect>, Boolean> playerEffects = MOD_ADDED_EFFECTS.get(player.getUUID());
      if (playerEffects != null && playerEffects.containsKey(effect)) {
         MobEffectInstance existing = player.getEffect(effect);
         if (existing != null) {
            player.removeEffect(effect);
         }

         playerEffects.remove(effect);
      }
   }

   private static void removeOwnEffectAt(Player player, Holder<MobEffect> effect, int amplifier) {
      Map<Holder<MobEffect>, Boolean> playerEffects = MOD_ADDED_EFFECTS.get(player.getUUID());
      if (playerEffects != null && playerEffects.containsKey(effect)) {
         MobEffectInstance existing = player.getEffect(effect);
         if (existing != null && existing.getAmplifier() == amplifier) {
            player.removeEffect(effect);
         }

         playerEffects.remove(effect);
      }
   }

   private static void handleJiSet(Player player) {
      boolean fullSet = player.getItemBySlot(EquipmentSlot.HEAD).is((Item)ModItems.JI_HELMET.get())
         && player.getItemBySlot(EquipmentSlot.CHEST).is((Item)ModItems.JI_CHESTPLATE.get())
         && player.getItemBySlot(EquipmentSlot.LEGS).is((Item)ModItems.JI_LEGGINGS.get())
         && player.getItemBySlot(EquipmentSlot.FEET).is((Item)ModItems.JI_BOOTS.get());
      AttributeInstance breakSpeed = player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
      AttributeInstance reach = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
      if (fullSet) {
         if (breakSpeed != null) {
            breakSpeed.removeModifier(JI_SET_BREAK_SPEED_ID);
            breakSpeed.addTransientModifier(new AttributeModifier(JI_SET_BREAK_SPEED_ID, 2.0, Operation.ADD_VALUE));
         }

         if (reach != null) {
            reach.removeModifier(JI_SET_REACH_ID);
            reach.addTransientModifier(new AttributeModifier(JI_SET_REACH_ID, 4.0, Operation.ADD_VALUE));
         }
      } else {
         if (breakSpeed != null) {
            breakSpeed.removeModifier(JI_SET_BREAK_SPEED_ID);
         }

         if (reach != null) {
            reach.removeModifier(JI_SET_REACH_ID);
         }
      }
   }

   public static void toggleComfortable(ServerPlayer player) {
      ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
      if (boots.is((Item)ModItems.ROYAL_STEEL_BOOTS.get())) {
         UUID uuid = player.getUUID();
         boolean enabled = COMFORTABLE_ENABLED.getOrDefault(uuid, false);
         enabled = !enabled;
         COMFORTABLE_ENABLED.put(uuid, enabled);
         player.displayClientMessage(actionMessage("舒适", enabled ? "开" : "关", ChatFormatting.GREEN), true);
         saveToggleStates(player);
      }
   }

   private static void handleComfortable(Player player) {
      ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
      boolean hasComfort = boots.is((Item)ModItems.ROYAL_STEEL_BOOTS.get()) && COMFORTABLE_ENABLED.getOrDefault(player.getUUID(), false);
      AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
      AttributeInstance stepHeight = player.getAttribute(Attributes.STEP_HEIGHT);
      if (hasComfort) {
         if (moveSpeed != null) {
            moveSpeed.removeModifier(COMFORTABLE_SPEED_ID);
            moveSpeed.addTransientModifier(new AttributeModifier(COMFORTABLE_SPEED_ID, 0.02, Operation.ADD_VALUE));
         }

         if (stepHeight != null) {
            stepHeight.removeModifier(COMFORTABLE_STEP_ID);
            stepHeight.addTransientModifier(new AttributeModifier(COMFORTABLE_STEP_ID, 0.5, Operation.ADD_VALUE));
         }
      } else {
         if (moveSpeed != null) {
            moveSpeed.removeModifier(COMFORTABLE_SPEED_ID);
         }

         if (stepHeight != null) {
            stepHeight.removeModifier(COMFORTABLE_STEP_ID);
         }
      }
   }

   public static void toggleLavaWalker(ServerPlayer player) {
      ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
      if (boots.is((Item)ModItems.EMBER_METAL_BOOTS.get())) {
         UUID uuid = player.getUUID();
         boolean enabled = !LAVA_WALKER_ENABLED.getOrDefault(uuid, false);
         LAVA_WALKER_ENABLED.put(uuid, enabled);
         Component msg = actionMessage("蹈火", enabled ? "开" : "关", enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
         player.displayClientMessage(msg, true);
         saveToggleStates(player);
         if (player instanceof ServerPlayer) {
            syncAbilityState(player);
         }
      }
   }

   public static boolean isLavaWalkerEnabled(Player player) {
      return player.level().isClientSide ? ClientAbilityState.isLavaWalker(player) : LAVA_WALKER_ENABLED.getOrDefault(player.getUUID(), false);
   }

   public static void toggleFrostSlide(ServerPlayer player) {
      ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
      if (boots.is((Item)ModItems.FROST_METAL_BOOTS.get())) {
         UUID uuid = player.getUUID();
         boolean enabled = !FROST_SLIDE_ENABLED.getOrDefault(uuid, false);
         FROST_SLIDE_ENABLED.put(uuid, enabled);
         Component msg = actionMessage("凌霜", enabled ? "开" : "关", enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
         player.displayClientMessage(msg, true);
         saveToggleStates(player);
         syncAbilityState(player);
      }
   }

   public static boolean isFrostSlideEnabled(Player player) {
      return player.level().isClientSide ? ClientAbilityState.isFrostSlide(player) : FROST_SLIDE_ENABLED.getOrDefault(player.getUUID(), false);
   }

   public static boolean hasFullSpectralSet(Player player) {
      boolean head = player.getItemBySlot(EquipmentSlot.HEAD).is((Item)ModItems.SPECTRAL_HELMET.get());
      boolean chest = player.getItemBySlot(EquipmentSlot.CHEST).is((Item)ModItems.SPECTRAL_CHESTPLATE.get());
      boolean legs = player.getItemBySlot(EquipmentSlot.LEGS).is((Item)ModItems.SPECTRAL_LEGGINGS.get());
      boolean feet = player.getItemBySlot(EquipmentSlot.FEET).is((Item)ModItems.SPECTRAL_BOOTS.get());
      return head && chest && legs && feet;
   }

   /**
    * 0 = no phase, 1 = horizontal free (full set passive), 2 = vertical also
    * free (full set + boots toggle on). Called from the collide mixin on both
    * sides every movement, so it must stay allocation-free.
    */
   public static int spectralPhaseMode(Player player) {
      if (!hasFullSpectralSet(player)) {
         return 0;
      }

      boolean vertical = player.level().isClientSide
         ? ClientAbilityState.isPhaseVertical(player)
         : SPECTRAL_PHASE_ENABLED.getOrDefault(player.getUUID(), false);
      return vertical ? 2 : 1;
   }

   public static void togglePhaseVertical(ServerPlayer player) {
      ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
      if (boots.is((Item)ModItems.SPECTRAL_BOOTS.get())) {
         UUID uuid = player.getUUID();
         boolean enabled = !SPECTRAL_PHASE_ENABLED.getOrDefault(uuid, false);
         if (enabled && !hasFullSpectralSet(player)) {
            player.displayClientMessage(Component.literal("虚化需要穿戴全套幻灵盔甲").withStyle(ChatFormatting.RED), true);
            return;
         }

         SPECTRAL_PHASE_ENABLED.put(uuid, enabled);
         player.displayClientMessage(actionMessage("虚化", enabled ? "开" : "关", enabled ? ChatFormatting.GREEN : ChatFormatting.RED), true);
         saveToggleStates(player);
         syncAbilityState(player);
      }
   }

   private static void handleSpectralPhase(Player player) {
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
      if (jumpDown) {
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

   public static void syncAbilityState(ServerPlayer player) {
      PacketDistributor.sendToPlayer(
         player,
         new AbilityStateSyncPacket(
            LAVA_WALKER_ENABLED.getOrDefault(player.getUUID(), false),
            FROST_SLIDE_ENABLED.getOrDefault(player.getUUID(), false),
            HELMET_MODE.getOrDefault(player.getUUID(), 5),
            SPECTRAL_PHASE_ENABLED.getOrDefault(player.getUUID(), false)
         ),
         new CustomPacketPayload[0]
      );
   }

   private static void handleEmberChestplate(Player player) {
      ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
      if (chest.is((Item)ModItems.TRANSCENDIUM_CHESTPLATE.get())) {
         applyBarrierEffect(player, 3);
      } else if (chest.is((Item)ModItems.EMBER_METAL_CHESTPLATE.get())) {
         applyBarrierEffect(player, 1);
      } else {
         removeOwnEffect(player, MobEffects.DAMAGE_RESISTANCE);
      }
   }

   private static void applyBarrierEffect(Player player, int amplifier) {
      MobEffectInstance existing = player.getEffect(MobEffects.DAMAGE_RESISTANCE);
      if (existing == null) {
         player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6000, amplifier, false, false, false));
         markEffectAdded(player, MobEffects.DAMAGE_RESISTANCE);
      } else if (existing.getAmplifier() < amplifier) {
         player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
         player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6000, amplifier, false, false, false));
         markEffectAdded(player, MobEffects.DAMAGE_RESISTANCE);
      } else if (existing.getAmplifier() == amplifier) {
         Map<Holder<MobEffect>, Boolean> playerEffects = MOD_ADDED_EFFECTS.get(player.getUUID());
         boolean isOurs = playerEffects != null && playerEffects.containsKey(MobEffects.DAMAGE_RESISTANCE);
         if (isOurs && existing.getDuration() < 400) {
            player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6000, amplifier, false, false, false));
            markEffectAdded(player, MobEffects.DAMAGE_RESISTANCE);
         }
      }
   }

   private static void handleEmberLeggings(Player player) {
      ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
      UUID uuid = player.getUUID();
      if (leggings.is((Item)ModItems.EMBER_METAL_LEGGINGS.get())) {
         Level level = player.level();
         BlockPos playerPos = player.blockPosition();
         boolean inSoulFire = level.getBlockState(playerPos).is(Blocks.SOUL_FIRE)
            || level.getBlockState(playerPos).is(Blocks.SOUL_CAMPFIRE)
            || level.getBlockState(playerPos.above()).is(Blocks.SOUL_FIRE)
            || level.getBlockState(playerPos.above()).is(Blocks.SOUL_CAMPFIRE)
            || level.getBlockState(playerPos.below()).is(Blocks.SOUL_CAMPFIRE)
            || level.getBlockState(playerPos.below(2)).is(Blocks.SOUL_CAMPFIRE);
         boolean inFire = inSoulFire || player.isOnFire();
         if (!inFire) {
            inFire = level.getBlockState(playerPos).is(Blocks.CAMPFIRE)
               || level.getBlockState(playerPos.above()).is(Blocks.CAMPFIRE)
               || level.getBlockState(playerPos.below()).is(Blocks.CAMPFIRE);
         }

         if (inFire) {
            long now = player.level().getGameTime();
            long lastHealTime = EMBER_LEG_LAST_HEAL_TIME.getOrDefault(uuid, -999L);
            boolean wasSoulFire = EMBER_LEG_WAS_SOUL_FIRE.getOrDefault(uuid, false);
            if (inSoulFire != wasSoulFire) {
               player.removeEffect(MobEffects.REGENERATION);
               int amplifier = inSoulFire ? 2 : 1;
               player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 260, amplifier, false, false, false));
               EMBER_LEG_LAST_HEAL_TIME.put(uuid, now);
               EMBER_LEG_WAS_SOUL_FIRE.put(uuid, inSoulFire);
            } else if (now - lastHealTime >= 200L) {
               int amplifier = inSoulFire ? 2 : 1;
               player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 260, amplifier, false, false, false));
               EMBER_LEG_LAST_HEAL_TIME.put(uuid, now);
               EMBER_LEG_WAS_SOUL_FIRE.put(uuid, inSoulFire);
            }
         } else {
            EMBER_LEG_LAST_HEAL_TIME.remove(uuid);
            EMBER_LEG_WAS_SOUL_FIRE.remove(uuid);
         }
      }
   }

   public static boolean isFluidSwimming(Entity entity) {
      if (entity instanceof Player player && FLUID_SWIM.contains(player.getUUID())) {
         return true;
      }

      return false;
   }

   private static void handleSurfaceWalking(Player player) {
      ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
      boolean transBoots = boots.is((Item)ModItems.TRANSCENDIUM_BOOTS.get());
      boolean emberBoots = boots.is((Item)ModItems.EMBER_METAL_BOOTS.get());
      if (!transBoots && !emberBoots) {
         FLUID_SWIM.remove(player.getUUID());
      } else {
         UUID uuid = player.getUUID();
         Level level = player.level();
         BlockPos feetPos = player.blockPosition();
         boolean fluidAtFeet = isWalkableFluid(level.getFluidState(feetPos), transBoots, emberBoots);
         boolean fluidBelow = isWalkableFluid(level.getFluidState(feetPos.below()), transBoots, emberBoots);
         if (!fluidAtFeet && !fluidBelow) {
            FLUID_SWIM.remove(uuid);
            if (transBoots) {
               handleVoidPlane(player);
            }
         } else {
            if (FLUID_SWIM.contains(uuid)) {
               double surfaceY = findFluidSurfaceY(level, feetPos, transBoots, emberBoots);
               if (Double.isNaN(surfaceY) || !(player.getY() >= surfaceY - 0.05) || !(player.getDeltaMovement().y > 0.0)) {
                  player.fallDistance = 0.0F;
                  return;
               }

               FLUID_SWIM.remove(uuid);
            }

            if (player.isShiftKeyDown() && fluidAtFeet) {
               FLUID_SWIM.add(uuid);
               LOGGER.info(
                  "[DingDongJi][流体] side={} tick={} 按shift进入下潜模式 FLUID_SWIM",
                  level.isClientSide() ? "C" : "S", player.tickCount
               );
               player.setOnGround(false);
               player.fallDistance = 0.0F;
            } else {
               handleFluidSurface(player, transBoots, emberBoots);
               if (transBoots) {
                  handleVoidPlane(player);
               }
            }
         }
      }
   }

   private static boolean isWalkableFluid(FluidState state, boolean walkAny, boolean walkLava) {
      if (state == null || state.isEmpty()) {
         return false;
      } else {
         return walkAny ? true : walkLava && state.is(FluidTags.LAVA);
      }
   }

   private static double findFluidSurfaceY(Level level, BlockPos feetPos, boolean walkAny, boolean walkLava) {
      FluidState here = level.getFluidState(feetPos);
      FluidState below = level.getFluidState(feetPos.below());
      double surfaceY = Double.NaN;
      if (isWalkableFluid(here, walkAny, walkLava)) {
         BlockPos top = feetPos;
         FluidState topState = here;

         for (int i = 0; i < 8; i++) {
            BlockPos up = top.above();
            FluidState upState = level.getFluidState(up);
            if (!isWalkableFluid(upState, walkAny, walkLava)) {
               break;
            }

            top = up;
            topState = upState;
         }

         surfaceY = (double)((float)top.getY() + topState.getHeight(level, top));
      }

      if (isWalkableFluid(below, walkAny, walkLava)) {
         double belowSurface = (double)((float)feetPos.below().getY() + below.getHeight(level, feetPos.below()));
         if (Double.isNaN(surfaceY) || belowSurface > surfaceY) {
            surfaceY = belowSurface;
         }
      }

      return surfaceY;
   }

   /**
    * Invoked from Entity#move TAIL while surface-walking boots are worn. When
    * this self movement brings a descending player onto (or just through) the
    * top of a walkable fluid, clamp the feet exactly onto the surface the way
    * a solid-block collision would. Doing it inside move() keeps the entity's
    * pre-move position above the surface, so render interpolation never shows
    * the one-tick dip that a next-tick snap in the tick event would cause.
    */
   public static boolean landOnWalkableFluid(Player player) {
      ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
      boolean transBoots = boots.is((Item)ModItems.TRANSCENDIUM_BOOTS.get());
      boolean emberBoots = boots.is((Item)ModItems.EMBER_METAL_BOOTS.get());
      if (!transBoots && !emberBoots) {
         return false;
      } else if (isFluidSwimming(player) || player.isShiftKeyDown() || player.isFallFlying()) {
         // Diving, crouch-diving or gliding: pass through the surface.
         return false;
      } else {
         Vec3 mot = player.getDeltaMovement();
         if (mot.y > 0.0) {
            return false;
         } else {
            Level level = player.level();
            BlockPos feetPos = player.blockPosition();
            boolean fluidAtFeet = isWalkableFluid(level.getFluidState(feetPos), transBoots, emberBoots);
            boolean fluidBelow = isWalkableFluid(level.getFluidState(feetPos.below()), transBoots, emberBoots);
            if (!fluidAtFeet && !fluidBelow) {
               return false;
            } else if (isWalkableFluid(level.getFluidState(BlockPos.containing(player.getEyePosition())), transBoots, emberBoots)) {
               // Fully immersed: keep the buoyant swim-up path instead of
               // teleporting onto the surface.
               return false;
            } else {
               double surfaceY = findFluidSurfaceY(level, feetPos, transBoots, emberBoots);
               if (Double.isNaN(surfaceY)) {
                  return false;
               } else {
                  double feetY = player.getY();
                  if (feetY <= surfaceY + 0.1 && feetY >= surfaceY - 0.6) {
                     LOGGER.info(
                        "[DingDongJi][流体] side={} tick={} moveTAIL夹到液面：feetY={} surfaceY={} motY={}",
                        player.level().isClientSide() ? "C" : "S", player.tickCount,
                        feetY, surfaceY, mot.y
                     );
                     player.setPos(player.getX(), surfaceY, player.getZ());
                     player.setDeltaMovement(mot.x, 0.0, mot.z);
                     player.setOnGround(true);
                     player.fallDistance = 0.0F;
                     return true;
                  } else {
                     return false;
                  }
               }
            }
         }
      }
   }

   private static void handleFluidSurface(Player player, boolean walkAny, boolean walkLava) {
      Level level = player.level();
      BlockPos feetPos = player.blockPosition();
      double surfaceY = findFluidSurfaceY(level, feetPos, walkAny, walkLava);
      if (!Double.isNaN(surfaceY)) {
         boolean eyeInFluid = isWalkableFluid(level.getFluidState(BlockPos.containing(player.getEyePosition())), walkAny, walkLava);
         if (eyeInFluid) {
            // 全身浸没：脚部与眼部（覆盖整个 1.8 高身体的上下两端）都在
            // 可行走流体中。此时玩家是主动潜回水里，强制 motY ≥ +0.12
            // 失效，完全交还给原版游泳物理（空格上浮 / Shift 下潜）。
            boolean fullySubmerged = isWalkableFluid(level.getFluidState(feetPos), walkAny, walkLava);
            Vec3 mot = player.getDeltaMovement();
            if (fullySubmerged) {
               LOGGER.info(
                  "[DingDongJi][流体] side={} tick={} 全身浸没→强制上浮失效：feetY={} surfaceY={} motY={} shift={}",
                  level.isClientSide() ? "C" : "S", player.tickCount,
                  player.getY(), surfaceY, mot.y, player.isShiftKeyDown()
               );
            } else {
               BlockState belowBlock = level.getBlockState(feetPos.below());
               LOGGER.info(
                  "[DingDongJi][流体] side={} tick={} 眼睛在流体→强制上浮：feetY={} surfaceY={} 改前motY={} shift={} onGround={} 脚下方块={}",
                  level.isClientSide() ? "C" : "S", player.tickCount,
                  player.getY(), surfaceY, mot.y, player.isShiftKeyDown(), player.onGround(),
                  BuiltInRegistries.BLOCK.getKey(belowBlock.getBlock())
               );
               player.setDeltaMovement(mot.x * 0.5, Math.max(mot.y, 0.12), mot.z * 0.5);
            }
         } else {
            double feetY = player.getY();
            double dy = feetY - surfaceY;
            Vec3 mot = player.getDeltaMovement();
            if (dy <= 0.1) {
               if (dy < 0.0 || mot.y < 0.0) {
                  player.setPos(player.getX(), surfaceY, player.getZ());
                  player.setDeltaMovement(mot.x, 0.0, mot.z);
               }

               player.setOnGround(true);
               player.fallDistance = 0.0F;
            }

            player.fallDistance = 0.0F;
         }
      }
   }

   private static void handleVoidPlane(Player player) {
      double y = player.getY();
      if (!(y < -64.0)) {
         Vec3 mot = player.getDeltaMovement();
         if (y + mot.y < -64.0) {
            player.setPos(player.getX(), -64.0, player.getZ());
            player.setDeltaMovement(mot.x, 0.0, mot.z);
            player.setOnGround(true);
            player.fallDistance = 0.0F;
         } else if (y <= -63.95) {
            player.setOnGround(true);
            if (mot.y < 0.0) {
               player.setDeltaMovement(mot.x, 0.0, mot.z);
            }

            player.fallDistance = 0.0F;
         }
      }
   }

   private static void handleEmberBootsFirePath(Player player) {
      ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
      if (boots.is((Item)ModItems.EMBER_METAL_BOOTS.get())) {
         if (!player.level().isClientSide) {
            if (LAVA_WALKER_ENABLED.getOrDefault(player.getUUID(), false)) {
               Level level = player.level();
               BlockPos playerPos = player.blockPosition();
               if (player.tickCount % 2 == 0) {
                  if (level.getBlockState(playerPos).isAir()) {
                     level.setBlockAndUpdate(playerPos, Blocks.FIRE.defaultBlockState());
                  } else if (level.getBlockState(playerPos.above()).isAir()) {
                     level.setBlockAndUpdate(playerPos.above(), Blocks.FIRE.defaultBlockState());
                  }
               }
            }
         }
      }
   }

   private static void handleEmberArmorRepair(Player player) {
      if (!player.isOnFire() && !player.isInLava()) {
         Level level = player.level();
         BlockPos playerPos = player.blockPosition();
         if (isFireBlock(level, playerPos) || isFireBlock(level, playerPos.above()) || isFireBlock(level, playerPos.below())) {
            repairEmberArmor(player);
         }
      } else {
         repairEmberArmor(player);
      }
   }

   private static boolean isFireBlock(Level level, BlockPos pos) {
      BlockState bs = level.getBlockState(pos);
      return bs.is(Blocks.FIRE) || bs.is(Blocks.SOUL_FIRE) || bs.is(Blocks.CAMPFIRE) || bs.is(Blocks.SOUL_CAMPFIRE);
   }

   private static void repairEmberArmor(Player player) {
      if (player.tickCount % 4 == 0) {
         ItemStack[] emberPieces = new ItemStack[]{
            player.getItemBySlot(EquipmentSlot.HEAD),
            player.getItemBySlot(EquipmentSlot.CHEST),
            player.getItemBySlot(EquipmentSlot.LEGS),
            player.getItemBySlot(EquipmentSlot.FEET)
         };

         for (ItemStack piece : emberPieces) {
            if ((
                  piece.is((Item)ModItems.EMBER_METAL_HELMET.get())
                     || piece.is((Item)ModItems.EMBER_METAL_CHESTPLATE.get())
                     || piece.is((Item)ModItems.EMBER_METAL_LEGGINGS.get())
                     || piece.is((Item)ModItems.EMBER_METAL_BOOTS.get())
               )
               && piece.isDamaged()) {
               piece.setDamageValue(Math.max(0, piece.getDamageValue() - 1));
            }
         }
      }
   }

   private static void handleRoyalSteelChestplate(Player player) {
      if (player.getItemBySlot(EquipmentSlot.CHEST).is((Item)ModItems.ROYAL_STEEL_CHESTPLATE.get())) {
         addHiddenEffect(player, MobEffects.REGENERATION, 0);
      } else {
         removeOwnEffectAt(player, MobEffects.REGENERATION, 0);
      }
   }

   private static void handleTranscendiumHelmet(Player player) {
      ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
      if (helmet.is((Item)ModItems.TRANSCENDIUM_HELMET.get())) {
         if (player.hasEffect(MobEffects.DARKNESS)) {
            player.removeEffect(MobEffects.DARKNESS);
         }

         if (player.getRemainingFireTicks() > 0) {
            player.clearFire();
         }

         player.setAirSupply(player.getMaxAirSupply());
         if (GLOWING_VISION_ENABLED.getOrDefault(player.getUUID(), false) && player.tickCount % 20 == 0) {
            GlowingVisionComponent vision = (GlowingVisionComponent)helmet.get((DataComponentType)ModComponents.GLOWING_VISION.get());
            if (vision == null) {
               vision = GlowingVisionComponent.DEFAULT;
            }

            int range = vision.range() * 2;
            AABB area = player.getBoundingBox().inflate((double)range);

            for (LivingEntity living : player.level()
               .getEntitiesOfClass(
                  LivingEntity.class,
                  area,
                  e -> e != player && e.isAlive() && (e instanceof Enemy || e instanceof Mob mob && mob.getTarget() == player) && isHostileTo(e, player)
               )) {
               MobEffectInstance existing = living.getEffect(MobEffects.GLOWING);
               if (existing == null || existing.getDuration() <= 100) {
                  living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false, true));
               }
            }
         }
      }
   }

   public static void onLivingBreathe(LivingBreatheEvent event) {
      if (event.getEntity() instanceof Player player && player.getItemBySlot(EquipmentSlot.HEAD).is((Item)ModItems.TRANSCENDIUM_HELMET.get())) {
         event.setCanBreathe(true);
         if (player.getAirSupply() < player.getMaxAirSupply()) {
            ((EntityAirDataAccessor)player).ddj$getEntityData().set(EntityAirDataAccessor.ddj$getAirSupplyId(), player.getMaxAirSupply());
         }
      }
   }

   private static boolean isHostileTo(Entity entity, Player player) {
      if (entity instanceof Enemy) {
         return true;
      } else {
         if (entity instanceof Mob mob && mob.getTarget() == player) {
            return true;
         }

         return false;
      }
   }

   public static void toggleGlowingVision(ServerPlayer player) {
      ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
      if (helmet.is((Item)ModItems.TRANSCENDIUM_HELMET.get())) {
         UUID uuid = player.getUUID();
         int nextMode = (HELMET_MODE.getOrDefault(uuid, 5) + 1) % 6;
         HELMET_MODE.put(uuid, nextMode);
         boolean glowingOn = nextMode == 0 || nextMode == 4;
         GLOWING_VISION_ENABLED.put(uuid, glowingOn);
         String[] names = new String[]{"高亮开", "高亮关", "夜视开", "夜视关", "全开", "全关"};
         player.displayClientMessage(actionMessage("适应", names[nextMode], HELMET_MODE_COLORS[nextMode]), true);
         saveToggleStates(player);
         syncAbilityState(player);
      }
   }

   public static void toggleNeutronBarrier(ServerPlayer player) {
      ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
      if (leggings.is((Item)ModItems.TRANSCENDIUM_LEGGINGS.get())) {
         UUID uuid = player.getUUID();
         int state = NEUTRON_BARRIER_ENABLED.getOrDefault(uuid, 0);
         state = (state + 1) % 4;
         NEUTRON_BARRIER_ENABLED.put(uuid, state);
         switch (state) {
            case 0:
               player.displayClientMessage(actionMessage("中子屏罩", "关闭屏蔽", ChatFormatting.BLUE), true);
               break;
            case 1:
               player.displayClientMessage(actionMessage("中子屏罩", "屏蔽敌对生物", ChatFormatting.GREEN), true);
               break;
            case 2:
               player.displayClientMessage(actionMessage("中子屏罩", "屏蔽弹射物", ChatFormatting.AQUA), true);
               break;
            case 3:
               player.displayClientMessage(actionMessage("中子屏罩", "全部屏蔽", ChatFormatting.LIGHT_PURPLE), true);
         }

         saveToggleStates(player);
      }
   }

   private static void handleMeaninglessConversion(Player player) {
      for (ItemStack stack : player.getInventory().items) {
         tryConvertFrostMetalPiece(player, stack);
      }

      for (ItemStack stack : player.getInventory().armor) {
         tryConvertFrostMetalPiece(player, stack);
      }

      tryConvertFrostMetalPiece(player, player.getOffhandItem());
   }

   private static void tryConvertFrostMetalPiece(Player player, ItemStack stack) {
      Type type = null;
      if (stack.is((Item)ModItems.FROST_METAL_HELMET.get())) {
         type = Type.HELMET;
      } else if (stack.is((Item)ModItems.FROST_METAL_CHESTPLATE.get())) {
         type = Type.CHESTPLATE;
      } else if (stack.is((Item)ModItems.FROST_METAL_LEGGINGS.get())) {
         type = Type.LEGGINGS;
      } else {
         if (!stack.is((Item)ModItems.FROST_METAL_BOOTS.get())) {
            return;
         }

         type = Type.BOOTS;
      }

      convertPiece(player, stack, type);
   }

   private static void convertPiece(Player player, ItemStack stack, Type armorType) {
      ItemEnchantments enchantments = (ItemEnchantments)stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
      if (!enchantments.isEmpty()) {
         ItemEnchantments convertedEnchs = ((MeaninglessData)stack.getOrDefault((DataComponentType)ModComponents.MEANINGLESS_DATA.get(), MeaninglessData.EMPTY))
            .convertedEnchantments();
         int newLevels = 0;
         Mutable convertedEnchsMut = new Mutable(convertedEnchs);

         for (Holder<Enchantment> enchantment : enchantments.keySet()) {
            int level = enchantments.getLevel(enchantment);
            if (convertedEnchs.getLevel(enchantment) < level) {
               int addLevels = level - convertedEnchs.getLevel(enchantment);
               if (addLevels > 0) {
                  newLevels += addLevels;
               }

               convertedEnchsMut.set(enchantment, level);
            }
         }

         if (newLevels != 0) {
            int totalLevels = 0;

            for (Holder<Enchantment> e : convertedEnchsMut.keySet()) {
               totalLevels += convertedEnchsMut.getLevel(e);
            }

            stack.set((DataComponentType)ModComponents.MEANINGLESS_DATA.get(), new MeaninglessData(totalLevels, convertedEnchsMut.toImmutable()));
            float bonusArmor = (float)Math.round(Math.sqrt((double)totalLevels) * 1.5 + (double)totalLevels / 5.0);
            float bonusToughness = (float)totalLevels / 3.0F;
            Builder builder = ItemAttributeModifiers.builder();
            if (bonusArmor > 0.0F) {
               builder.add(
                  Attributes.ARMOR,
                  new AttributeModifier(MEANINGLESS_ARMOR_ID, (double)bonusArmor, Operation.ADD_VALUE),
                  EquipmentSlotGroup.bySlot(armorType.getSlot())
               );
            }

            if (bonusToughness > 0.0F) {
               builder.add(
                  Attributes.ARMOR_TOUGHNESS,
                  new AttributeModifier(MEANINGLESS_TOUGHNESS_ID, (double)bonusToughness, Operation.ADD_VALUE),
                  EquipmentSlotGroup.bySlot(armorType.getSlot())
               );
            }

            for (Entry entry : stack.getAttributeModifiers().modifiers()) {
               if (!entry.modifier().is(MEANINGLESS_ARMOR_ID) && !entry.modifier().is(MEANINGLESS_TOUGHNESS_ID)) {
                  builder.add(entry.attribute(), entry.modifier(), entry.slot());
               }
            }

            stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
         }
      }
   }

   private static void handleTranscendiumReflect(Player player) {
      ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
      if (leggings.is((Item)ModItems.TRANSCENDIUM_LEGGINGS.get())) {
         Level level = player.level();
         Vec3 center = player.position();
         double shieldRadius = 3.0;
         long gameTime = level.getGameTime();
         int __bs = NEUTRON_BARRIER_ENABLED.getOrDefault(player.getUUID(), 0);
         if (__bs == 2 || __bs == 3) {
            for (Projectile projectile : level.getEntitiesOfClass(
               Projectile.class,
               player.getBoundingBox().inflate(shieldRadius),
               p -> p.isAlive() && p.distanceToSqr(center) <= shieldRadius * shieldRadius && isHarmfulProjectile(p, player)
            )) {
               spawnAbsorptionRipple(level, projectile.position(), player);
               projectile.discard();
            }
         }

         int __state = NEUTRON_BARRIER_ENABLED.getOrDefault(player.getUUID(), 0);
         if (__state == 1 || __state == 3) {
            AABB range = player.getBoundingBox().inflate(2.0, 2.0, 2.0).expandTowards(0.0, -3.0, 0.0);

            for (Entity threat : level.getEntities(
               player, range, e -> e != player && e.isAlive() && (e instanceof Enemy || e instanceof Mob mob && mob.getTarget() == player)
            )) {
               UUID tid = threat.getUUID();
               Long cooldownEnd = BOSS_REPEL_COOLDOWN.get(tid);
               if (cooldownEnd == null || gameTime >= cooldownEnd) {
                  Vec3 knockDir = threat.position().subtract(player.position()).normalize();
                  threat.push(knockDir.x * 1.5, 0.1, knockDir.z * 1.5);
                  threat.hurtMarked = true;
                  spawnRepelBurst(player, threat, knockDir, gameTime, false);
               }
            }

            BOSS_PROXIMITY_START.keySet().removeIf(id -> {
               Entity e = ((ServerLevel)level).getEntity(id);
               return e == null || !e.isAlive() || !range.contains(e.position());
            });
            BOSS_REPEL_COOLDOWN.keySet().removeIf(id -> {
               Entity e = ((ServerLevel)level).getEntity(id);
               return e == null || !e.isAlive();
            });
         }
      }
   }

   private static boolean isHarmfulProjectile(Projectile projectile, Player player) {
      Entity owner = projectile.getOwner();
      if (owner == player) {
         return false;
      } else if (owner == null) {
         Vec3 vel = projectile.getDeltaMovement();
         if (vel.lengthSqr() < 0.01) {
            return false;
         } else {
            Vec3 toPlayer = player.position().subtract(projectile.position()).normalize();
            Vec3 velNorm = vel.normalize();
            return velNorm.dot(toPlayer) > 0.5;
         }
      } else if (owner instanceof Enemy) {
         return true;
      } else {
         if (owner instanceof Mob mob && mob.getTarget() == player) {
            return true;
         }

         return owner instanceof Player && owner != player ? projectile.getClass().getName().startsWith("net.minecraft.") : false;
      }
   }

   private static boolean isBoss(LivingEntity entity) {
      return (double)entity.getMaxHealth() >= 150.0;
   }

   private static void spawnAbsorptionRipple(Level level, Vec3 pos, Player player) {
      if (level instanceof ServerLevel serverLevel) {
         serverLevel.sendParticles(ModParticles.NEUTRON_BARRIER_ABSORB.get(), pos.x, pos.y + 0.5, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
      }
   }

   private static void spawnRepelBurst(Player player, Entity threat, Vec3 knockDir, long gameTime, boolean big) {
      if (player.level() instanceof ServerLevel serverLevel) {
         UUID var11 = player.getUUID();
         Long last = REPEL_PARTICLE_COOLDOWN.get(var11);
         if (last == null || gameTime - last >= 20L) {
            REPEL_PARTICLE_COOLDOWN.put(var11, gameTime);
            SimpleParticleType type = big ? ModParticles.NEUTRON_BARRIER_REPEL_BIG.get() : ModParticles.NEUTRON_BARRIER_REPEL.get();
            Vec3 chest = player.position().add(0.0, 1.5, 0.0);
            serverLevel.sendParticles(type, chest.x, chest.y, chest.z, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }
   }

   private static void handleTranscendiumBootsFlight(Player player) {
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

         if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
         }

         float targetSpeed = mode == 2 ? 0.1F : 0.05F;
         if (Math.abs(player.getAbilities().getFlyingSpeed() - targetSpeed) > 1.0E-4F) {
            player.getAbilities().setFlyingSpeed(targetSpeed);
            player.onUpdateAbilities();
         }

         IONOCRAFT_GRANTED.put(uuid, true);
         int broadcastMode = player.getAbilities().flying && !isCreative && !isSpectator ? mode : 0;
         syncIonocraftFlyingState(player, broadcastMode);
      }
   }

   public static void onBreakSpeed(BreakSpeed event) {
      Player speed = event.getEntity();
      if (speed instanceof Player) {
         float var6 = event.getOriginalSpeed();
         boolean modified = false;
         if (!speed.onGround()
            && speed.getAbilities().flying
            && !speed.getAbilities().instabuild
            && speed.getItemBySlot(EquipmentSlot.FEET).is((Item)ModItems.TRANSCENDIUM_BOOTS.get())) {
            var6 *= 5.0F;
            modified = true;
         }

         if (speed.isEyeInFluid(FluidTags.WATER) && speed.getItemBySlot(EquipmentSlot.HEAD).is((Item)ModItems.TRANSCENDIUM_HELMET.get())) {
            double submerged = speed.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED);
            if (submerged > 1.0E-5 && Math.abs(submerged - 1.0) > 1.0E-5) {
               var6 /= (float)submerged;
               modified = true;
            }
         }

         if (modified) {
            event.setNewSpeed(var6);
         }
      }
   }

   private static void handleTranscendiumFluidEfficiency(Player player) {
      boolean fullSet = hasFullTranscendiumSet(player);
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
         player.getAbilities().mayfly = true;
         player.getAbilities().setFlyingSpeed(mode == 2 ? 0.1F : 0.05F);
         player.onUpdateAbilities();
         Component msg = Component.empty()
            .append(Component.literal("蹈虚：").withStyle(ChatFormatting.WHITE))
            .append(Component.literal(mode == 2 ? "开" : "关").withStyle(mode == 2 ? ChatFormatting.GREEN : ChatFormatting.RED));
         player.displayClientMessage(msg, true);
         saveToggleStates(player);
      }
   }

   @SubscribeEvent
   public static void onPlayerLogin(PlayerLoggedInEvent event) {
      Player player = event.getEntity();
      if (!player.level().isClientSide) {
         loadToggleStates(player);
         if (player instanceof ServerPlayer serverPlayer) {
            syncAbilityState(serverPlayer);
         }
      }
   }

   @SubscribeEvent
   public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
      if (event.getEntity() instanceof Player player) {
         if (!player.level().isClientSide) {
            if (AnvilCraftCompat.isLoaded()) {
               ItemStack to = event.getTo();
               if (!to.isEmpty()) {
                  applyAnvilCraftComponent(to);
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLivingDamage(LivingIncomingDamageEvent event) {
      if (event.getEntity() instanceof Player player) {
         if (!player.level().isClientSide) {
            // Phasing players stand inside blocks by design; vanilla suffocation
            // would otherwise fire constantly.
            if (event.getSource().is(DamageTypes.IN_WALL) && hasFullSpectralSet(player)) {
               event.setCanceled(true);
               return;
            }

            if (isArmorProtectedDamage(player, event.getSource())) {
               event.setCanceled(true);
            }
         }
      }
   }

   public static boolean isArmorProtectedDamage(Player player, DamageSource source) {
      ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
      ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
      ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
      boolean transHelmet = helmet.is((Item)ModItems.TRANSCENDIUM_HELMET.get());
      boolean emberHelmet = helmet.is((Item)ModItems.EMBER_METAL_HELMET.get());
      boolean frostHelmet = helmet.is((Item)ModItems.FROST_METAL_HELMET.get());
      if (boots.is((Item)ModItems.TRANSCENDIUM_BOOTS.get()) && source.is(DamageTypes.FALL)) {
         return true;
      } else if (transHelmet && source.is(DamageTypes.IN_WALL)) {
         return true;
      } else if ((transHelmet || emberHelmet) && source.is(DamageTypeTags.IS_FIRE)) {
         return true;
      } else if (!transHelmet || !source.is(DamageTypeTags.IS_DROWNING) && !source.is(DamageTypeTags.IS_FREEZING)) {
         return frostHelmet && source.is(DamageTypeTags.IS_FREEZING)
            ? true
            : chest.is((Item)ModItems.TRANSCENDIUM_CHESTPLATE.get()) && isBarrierIIIgnored(source);
      } else {
         return true;
      }
   }

   public static boolean hasFullTranscendiumSet(Player player) {
      return player.getItemBySlot(EquipmentSlot.HEAD).is((Item)ModItems.TRANSCENDIUM_HELMET.get())
         && player.getItemBySlot(EquipmentSlot.CHEST).is((Item)ModItems.TRANSCENDIUM_CHESTPLATE.get())
         && player.getItemBySlot(EquipmentSlot.LEGS).is((Item)ModItems.TRANSCENDIUM_LEGGINGS.get())
         && player.getItemBySlot(EquipmentSlot.FEET).is((Item)ModItems.TRANSCENDIUM_BOOTS.get());
   }

   /**
    * 飞行相位偏移（穿墙 + 观察者式视觉）的判定：
    * 穿齐全套超限合金套 + 玩家真的在飞（mayfly=true 且 flying=true）。
    * 关飞行、下地面、脱靴子任何一种都立即失去。
    */
   public static boolean isTranscendiumFlightPhasing(Player player) {
      return hasFullTranscendiumSet(player) && player.getAbilities().flying;
   }

   public static boolean wearsHurtAnimationCancelArmor(Player player) {
      return player.getItemBySlot(EquipmentSlot.HEAD).is((Item)ModItems.TRANSCENDIUM_HELMET.get())
         || player.getItemBySlot(EquipmentSlot.HEAD).is((Item)ModItems.EMBER_METAL_HELMET.get())
         || player.getItemBySlot(EquipmentSlot.HEAD).is((Item)ModItems.FROST_METAL_HELMET.get())
         || player.getItemBySlot(EquipmentSlot.CHEST).is((Item)ModItems.TRANSCENDIUM_CHESTPLATE.get())
         || player.getItemBySlot(EquipmentSlot.LEGS).is((Item)ModItems.TRANSCENDIUM_LEGGINGS.get())
         || player.getItemBySlot(EquipmentSlot.FEET).is((Item)ModItems.TRANSCENDIUM_BOOTS.get());
   }

   public static boolean ignoresBlockSlowdown(Entity entity) {
      if (entity instanceof Player player) {
         ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
         if (!boots.is((Item)ModItems.TRANSCENDIUM_BOOTS.get()) && !boots.has((DataComponentType)ModComponents.FROST_WALK.get())) {
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            return chest.has((DataComponentType)ModComponents.FROST_WARD.get());
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   private static boolean isBarrierIIIgnored(DamageSource source) {
      return source.is(DamageTypes.MAGIC)
         || source.is(DamageTypes.INDIRECT_MAGIC)
         || source.is(DamageTypes.DRAGON_BREATH)
         || source.is(DamageTypes.WITHER)
         || source.is(DamageTypes.WITHER_SKULL)
         || source.is(DamageTypes.SONIC_BOOM)
         || source.is(DamageTypes.FELL_OUT_OF_WORLD)
         || source.is(DamageTypes.CACTUS)
         || source.is(DamageTypes.SWEET_BERRY_BUSH)
         || source.is(DamageTypes.THORNS)
         || source.is(DamageTypes.EXPLOSION)
         || source.is(DamageTypes.PLAYER_EXPLOSION)
         || source.is(DamageTypes.BAD_RESPAWN_POINT);
   }

   private static void saveToggleStates(Player player) {
      CompoundTag data = player.getPersistentData();
      UUID uuid = player.getUUID();
      data.putInt("dingdongji:helmet_mode", HELMET_MODE.getOrDefault(uuid, 5));
      data.putBoolean("dingdongji:lava_walker", LAVA_WALKER_ENABLED.getOrDefault(uuid, false));
      data.putBoolean("dingdongji:frost_slide", FROST_SLIDE_ENABLED.getOrDefault(uuid, false));
      data.putBoolean("dingdongji:spectral_phase", SPECTRAL_PHASE_ENABLED.getOrDefault(uuid, false));
      data.putBoolean("dingdongji:comfortable", COMFORTABLE_ENABLED.getOrDefault(uuid, false));
      data.putInt("dingdongji:neutron_barrier", NEUTRON_BARRIER_ENABLED.getOrDefault(uuid, 0));
      data.putInt("dingdongji:ionocraft_mode", IONOCRAFT_FLIGHT_MODE.getOrDefault(uuid, 1));
   }

   private static void loadToggleStates(Player player) {
      CompoundTag data = player.getPersistentData();
      UUID uuid = player.getUUID();
      int helmetMode = data.contains("dingdongji:helmet_mode") ? data.getInt("dingdongji:helmet_mode") : 5;
      HELMET_MODE.put(uuid, helmetMode);
      GLOWING_VISION_ENABLED.put(uuid, helmetMode == 0 || helmetMode == 4);
      LAVA_WALKER_ENABLED.put(uuid, data.getBoolean("dingdongji:lava_walker"));
      FROST_SLIDE_ENABLED.put(uuid, data.getBoolean("dingdongji:frost_slide"));
      SPECTRAL_PHASE_ENABLED.put(uuid, data.getBoolean("dingdongji:spectral_phase"));
      COMFORTABLE_ENABLED.put(uuid, data.getBoolean("dingdongji:comfortable"));
      NEUTRON_BARRIER_ENABLED.put(uuid, data.getInt("dingdongji:neutron_barrier"));
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

   private static void applyAnvilCraftComponent(ItemStack stack) {
      if (stack.is((Item)ModItems.JI_SWORD.get())
         || stack.is((Item)ModItems.JI_PICKAXE.get())
         || stack.is((Item)ModItems.JI_HELMET.get())
         || stack.is((Item)ModItems.JI_CHESTPLATE.get())
         || stack.is((Item)ModItems.JI_LEGGINGS.get())
         || stack.is((Item)ModItems.JI_BOOTS.get())) {
         AnvilCraftCompat.setFortune(stack);
      }

      if (stack.is((Item)ModItems.EMBER_METAL_HELMET.get())
         || stack.is((Item)ModItems.EMBER_METAL_CHESTPLATE.get())
         || stack.is((Item)ModItems.EMBER_METAL_LEGGINGS.get())
         || stack.is((Item)ModItems.EMBER_METAL_BOOTS.get())) {
         AnvilCraftCompat.setReforging(stack);
      }

      if (stack.is((Item)ModItems.TRANSCENDIUM_HELMET.get())
         || stack.is((Item)ModItems.TRANSCENDIUM_CHESTPLATE.get())
         || stack.is((Item)ModItems.TRANSCENDIUM_LEGGINGS.get())
         || stack.is((Item)ModItems.TRANSCENDIUM_BOOTS.get())) {
         AnvilCraftCompat.setEternal(stack);
      }
   }
}
