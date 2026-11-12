package com.dingdongji.mod.event;

import com.dingdongji.mod.ability.ArmorSets;
import com.dingdongji.mod.ability.ComfortableAbility;
import com.dingdongji.mod.ability.EmberSetAbility;
import com.dingdongji.mod.ability.GlowingVision;
import com.dingdongji.mod.ability.IonocraftBoots;
import com.dingdongji.mod.ability.JiSetAbility;
import com.dingdongji.mod.ability.NeutronBarrier;
import com.dingdongji.mod.ability.RoyalSetAbility;
import com.dingdongji.mod.ability.SpectralPhase;
import com.dingdongji.mod.ability.SurfaceWalking;
import com.dingdongji.mod.ability.TranscendiumSet;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.network.AbilityStateSyncPacket;
import com.dingdongji.mod.util.AnvilCraftCompat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre;
import net.neoforged.neoforge.network.PacketDistributor;

public class ModArmorSetHandler {
   public static final Map<UUID, Integer> HELMET_MODE = new HashMap<>();
   public static final ChatFormatting[] HELMET_MODE_COLORS = new ChatFormatting[]{
      ChatFormatting.GREEN, ChatFormatting.RED, ChatFormatting.AQUA, ChatFormatting.YELLOW, ChatFormatting.LIGHT_PURPLE, ChatFormatting.BLUE
   };
   public static final int FLIGHT_OFF = IonocraftBoots.FLIGHT_OFF;
   public static final int FLIGHT_NORMAL = IonocraftBoots.FLIGHT_NORMAL;
   public static final int FLIGHT_SUPER = IonocraftBoots.FLIGHT_SUPER;

   /** 套装开关 action bar 统一格式：标题白色 + 状态色（开绿/关红）。 */
   public static Component actionMessage(String title, String suffix, ChatFormatting suffixColor) {
      return ArmorSets.actionMessage(title, suffix, suffixColor);
   }

   @SubscribeEvent
   public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
      Player player = event.getEntity();
      UUID uuid = player.getUUID();
      saveToggleStates(player);
      SurfaceWalking.clearOnLogout(uuid);
      SpectralPhase.clearOnLogout(uuid);
      GlowingVision.clearOnLogout(uuid);
      GlowingVision.clearGlowingTargets(player);
      ComfortableAbility.clearOnLogout(uuid);
      NeutronBarrier.clearOnLogout(uuid);
      TranscendiumSet.clearOnLogout(uuid);
      JiSetAbility.clearOnLogout(uuid);
      HELMET_MODE.remove(uuid);
      IonocraftBoots.clearOnLogout(uuid);
      EmberSetAbility.clearOnLogout(uuid);
      if (player instanceof ServerPlayer serverPlayer) {
         PhantomDecoyManager.onLogout(serverPlayer);
         SquatGeneratorManager.onLogout(serverPlayer);
      }
   }

   @SubscribeEvent
   public static void onPlayerTick(Pre event) {
      Player player = event.getEntity();
      // 双端（客户端 LocalPlayer 同样执行，保证移动手感与服务端一致）
      SurfaceWalking.handleSurfaceWalking(player);
      TranscendiumSet.handleVoidFloor(player);
      // 幻灵虚化垂直控制必须双端执行：客户端负责手感（跳 0.2/潜 -0.15/悬停），
      // 服务端负责潜行下潜；曾因被误收进服务端分支导致跳跃/悬停失效。
      SpectralPhase.handleSpectralPhase(player);
      if (!player.level().isClientSide) {
         JiSetAbility.handleJiSet(player);
         ComfortableAbility.handleComfortable(player);
         RoyalSetAbility.handleRoyalSet(player);
         EmberSetAbility.handleEmberSet(player);
         EmberSetAbility.handleEmberArmorRepair(player);
         EmberSetAbility.handleEmberBootsFirePath(player);
         GlowingVision.handleTranscendiumHelmet(player);
         NeutronBarrier.handleTranscendiumReflect(player);
         TranscendiumSet.handleMeaninglessConversion(player);
         IonocraftBoots.handleTranscendiumBootsFlight(player);
         IonocraftBoots.handleTranscendiumFluidEfficiency(player);
         if (player instanceof ServerPlayer serverPlayer) {
            PhantomDecoyManager.serverTick(serverPlayer);
            SquatGeneratorManager.serverTick(serverPlayer);
         }
      }
   }

   public static void syncAbilityState(ServerPlayer player) {
      PacketDistributor.sendToPlayer(
         player,
         new AbilityStateSyncPacket(
            SurfaceWalking.LAVA_WALKER_ENABLED.getOrDefault(player.getUUID(), false),
            SurfaceWalking.FROST_SLIDE_ENABLED.getOrDefault(player.getUUID(), false),
            HELMET_MODE.getOrDefault(player.getUUID(), 5),
            SpectralPhase.SPECTRAL_PHASE_ENABLED.getOrDefault(player.getUUID(), false),
            TranscendiumSet.TRANSCENDIUM_PHASE_ENABLED.getOrDefault(player.getUUID(), false),
            PhantomDecoyManager.sessionExists(player.getUUID())
         ),
         new CustomPacketPayload[0]
      );
   }

   public static void toggleJiSet(ServerPlayer player) {
      JiSetAbility.toggleJiSet(player);
   }

   public static boolean hasFullJiSet(Player player) {
      return ArmorSets.hasFullJiSet(player);
   }

   public static void toggleComfortable(ServerPlayer player) {
      ComfortableAbility.toggleComfortable(player);
   }

   public static void toggleLavaWalker(ServerPlayer player) {
      SurfaceWalking.toggleLavaWalker(player);
   }

   public static boolean isLavaWalkerEnabled(Player player) {
      return SurfaceWalking.isLavaWalkerEnabled(player);
   }

   public static void toggleFrostSlide(ServerPlayer player) {
      SurfaceWalking.toggleFrostSlide(player);
   }

   public static boolean isFrostSlideEnabled(Player player) {
      return SurfaceWalking.isFrostSlideEnabled(player);
   }

   public static boolean hasFullSpectralSet(Player player) {
      return ArmorSets.hasFullSpectralSet(player);
   }

   public static int spectralPhaseMode(Player player) {
      return SpectralPhase.spectralPhaseMode(player);
   }

   public static boolean isWallPhaseActive(Player player) {
      return SpectralPhase.isWallPhaseActive(player);
   }

   public static boolean isHorizontalPhaseActive(Player player) {
      return SpectralPhase.isHorizontalPhaseActive(player);
   }

   public static void togglePhaseVertical(ServerPlayer player) {
      SpectralPhase.togglePhaseVertical(player);
   }

   public static boolean isVerticalPhaseActive(Player player) {
      return SpectralPhase.isVerticalPhaseActive(player);
   }

   public static boolean isBodyClippingBlock(Entity entity) {
      return SpectralPhase.isBodyClippingBlock(entity);
   }

   public static void applyVerticalPhase(Player player, boolean jumpDown, boolean sneakDown) {
      SpectralPhase.applyVerticalPhase(player, jumpDown, sneakDown);
   }

   public static boolean hasFullEmberSet(Player player) {
      return ArmorSets.hasFullEmberSet(player);
   }

   public static boolean hasFullFrostSet(Player player) {
      return ArmorSets.hasFullFrostSet(player);
   }

   public static boolean hasFullRoyalSet(Player player) {
      return ArmorSets.hasFullRoyalSet(player);
   }

   public static boolean isFluidSwimming(Entity entity) {
      return SurfaceWalking.isFluidSwimming(entity);
   }

   public static boolean landOnWalkableFluid(Player player) {
      return SurfaceWalking.landOnWalkableFluid(player);
   }

   public static void onLivingBreathe(LivingBreatheEvent event) {
      GlowingVision.onLivingBreathe(event);
   }

   public static void toggleGlowingVision(ServerPlayer player) {
      GlowingVision.toggleGlowingVision(player);
   }

   public static void toggleNeutronBarrier(ServerPlayer player) {
      NeutronBarrier.toggleNeutronBarrier(player);
   }

   public static void toggleTranscendiumPhase(ServerPlayer player) {
      TranscendiumSet.toggleTranscendiumPhase(player);
   }

   /**
    * 手持功能盔甲经轮盘（本体"切换工具模式"键）直接设置功能状态。
    * 状态按玩家持久化、与穿戴解耦，故此处不做穿戴校验；功能激活仍需穿戴
    * 对应盔甲（虚化/相位类另需全套），手持预设状态穿上即生效。
    */
   public static void selectArmorFunctionFromHand(ServerPlayer player, InteractionHand hand, int state) {
      ItemStack stack = player.getItemInHand(hand);
      UUID uuid = player.getUUID();
      if (stack.is((Item)ModItems.ROYAL_STEEL_BOOTS.get())) {
         boolean enabled = state == 1;
         ComfortableAbility.COMFORTABLE_ENABLED.put(uuid, enabled);
         player.displayClientMessage(ArmorSets.actionMessage("舒适", enabled ? "开" : "关", enabled ? ChatFormatting.GREEN : ChatFormatting.RED), true);
         saveToggleStates(player);
      } else if (stack.is((Item)ModItems.EMBER_METAL_BOOTS.get())) {
         boolean enabled = state == 1;
         SurfaceWalking.LAVA_WALKER_ENABLED.put(uuid, enabled);
         player.displayClientMessage(ArmorSets.actionMessage("蹈火", enabled ? "开" : "关", enabled ? ChatFormatting.GREEN : ChatFormatting.RED), true);
         saveToggleStates(player);
         syncAbilityState(player);
      } else if (stack.is((Item)ModItems.FROST_METAL_BOOTS.get())) {
         boolean enabled = state == 1;
         SurfaceWalking.FROST_SLIDE_ENABLED.put(uuid, enabled);
         player.displayClientMessage(ArmorSets.actionMessage("凌霜", enabled ? "开" : "关", enabled ? ChatFormatting.GREEN : ChatFormatting.RED), true);
         saveToggleStates(player);
         syncAbilityState(player);
      } else if (stack.is((Item)ModItems.SPECTRAL_BOOTS.get())) {
         boolean enabled = state == 1;
         SpectralPhase.SPECTRAL_PHASE_ENABLED.put(uuid, enabled);
         player.displayClientMessage(ArmorSets.actionMessage("虚化", enabled ? "开" : "关", enabled ? ChatFormatting.GREEN : ChatFormatting.RED), true);
         saveToggleStates(player);
         syncAbilityState(player);
      } else if (stack.is((Item)ModItems.TRANSCENDIUM_BOOTS.get())) {
         // 蹈虚只有 普通(1)/超速(2) 两态；飞行能力由 handleTranscendiumBootsFlight
         // 每 tick 按模式应用，手持设置无需触碰 abilities。
         int mode = state == 2 ? 2 : 1;
         IonocraftBoots.IONOCRAFT_FLIGHT_MODE.put(uuid, mode);
         player.displayClientMessage(ArmorSets.actionMessage("蹈虚", mode == 2 ? "开" : "关", mode == 2 ? ChatFormatting.GREEN : ChatFormatting.RED), true);
         saveToggleStates(player);
      } else if (stack.is((Item)ModItems.TRANSCENDIUM_HELMET.get())) {
         int mode = Math.min(Math.max(state, 0), 5);
         HELMET_MODE.put(uuid, mode);
         GlowingVision.GLOWING_VISION_ENABLED.put(uuid, mode == 0 || mode == 4);
         String[] names = new String[]{"高亮开", "高亮关", "夜视开", "夜视关", "全开", "全关"};
         player.displayClientMessage(ArmorSets.actionMessage("适应", names[mode], HELMET_MODE_COLORS[mode]), true);
         saveToggleStates(player);
         syncAbilityState(player);
      } else if (stack.is((Item)ModItems.TRANSCENDIUM_CHESTPLATE.get())) {
         int barrierState = Math.min(Math.max(state, 0), 3);
         NeutronBarrier.NEUTRON_BARRIER_ENABLED.put(uuid, barrierState);
         switch (barrierState) {
            case 0:
               player.displayClientMessage(ArmorSets.actionMessage("中子力场", "关闭屏蔽", ChatFormatting.BLUE), true);
               break;
            case 1:
               player.displayClientMessage(ArmorSets.actionMessage("中子力场", "屏蔽敌对生物", ChatFormatting.GREEN), true);
               break;
            case 2:
               player.displayClientMessage(ArmorSets.actionMessage("中子力场", "屏蔽弹射物", ChatFormatting.AQUA), true);
               break;
            case 3:
               player.displayClientMessage(ArmorSets.actionMessage("中子力场", "全部屏蔽", ChatFormatting.LIGHT_PURPLE), true);
         }

         saveToggleStates(player);
      }
   }

   @SubscribeEvent
   public static void onBreakSpeed(BreakSpeed event) {
      JiSetAbility.onBreakSpeed(event);
   }

   public static void toggleIonocraftFlight(ServerPlayer player) {
      IonocraftBoots.toggleIonocraftFlight(player);
   }

   @SubscribeEvent
   public static void onPlayerLogin(PlayerLoggedInEvent event) {
      Player player = event.getEntity();
      if (!player.level().isClientSide) {
         loadToggleStates(player);
         if (player instanceof ServerPlayer serverPlayer) {
            syncAbilityState(serverPlayer);
            PhantomDecoyManager.syncAllPhantomStatesTo(serverPlayer);
         }
      }
   }

   /** 死亡重生时重新加载套装开关状态（PersistentData 保留，但内存 Map 在重生后丢失）。 */
   @SubscribeEvent
   public static void onPlayerClone(net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone event) {
      Player player = event.getEntity();
      if (!player.level().isClientSide && event.isWasDeath()) {
         loadToggleStates(player);
      }
   }

   @SubscribeEvent
   public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
      if (event.getEntity() instanceof Player player) {
         if (!player.level().isClientSide) {
            if (AnvilCraftCompat.isLoaded()) {
               ItemStack to = event.getTo();
               if (!to.isEmpty()) {
                  TranscendiumSet.applyAnvilCraftComponent(to);
               }
            }
            // 幻影状态下穿脱任意盔甲：立即传回替身位置并结束（手持物切换
            // 仅每 tick 同步替身渲染，不结束状态）
            if (event.getSlot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
               PhantomDecoyManager.onEquipmentChanged(player);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLivingDamage(LivingIncomingDamageEvent event) {
      if (event.getEntity() instanceof Player player) {
         if (!player.level().isClientSide) {
            // Phasing players stand inside blocks by design; vanilla suffocation
            // would otherwise fire constantly. 仅墙体相位激活时豁免
            //（幻灵虚化开 / 超限相位偏移开），穿全套但未穿墙时不免窒息。
            if (event.getSource().is(DamageTypes.IN_WALL) && SpectralPhase.isWallPhaseActive(player)) {
               event.setCanceled(true);
               return;
            }

            if (ArmorSets.isArmorProtectedDamage(player, event.getSource())) {
               event.setCanceled(true);
            }

            // 幻影中的本体：伤害完全不结算（不继承幻影期间的任何伤害），
            // 同时结束幻影并传回替身位置（血量以替身血量池为准）。
            // 虚空/自净清除类伤害放行，避免幻影在虚空中无限滞留。
            if (!event.isCanceled() && event.getAmount() > 0.0F && player instanceof ServerPlayer serverPlayer
                  && PhantomDecoyManager.isPhantomState(player)) {
               if (event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)
                     || event.getSource().is(DamageTypes.GENERIC_KILL)) {
                  PhantomDecoyManager.onOwnerHurt(serverPlayer);
                  return;
               }
               event.setCanceled(true);
               // 1.21 的点燃在伤害管线内部结算，直接取消事件会连带跳过着火——
               // 火焰/熔岩伤害取消后手动补点燃（视觉着火，伤害仍不结算，
               // 传回时 clearFire 会熄灭）
               if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
                     || event.getSource().is(DamageTypes.LAVA)) {
                  player.igniteForSeconds(8.0F);
               }
               PhantomDecoyManager.onOwnerHurt(serverPlayer);
            }
         }
      }
   }

   public static boolean isArmorProtectedDamage(Player player, DamageSource source) {
      return ArmorSets.isArmorProtectedDamage(player, source);
   }

   public static boolean hasFullTranscendiumSet(Player player) {
      return ArmorSets.hasFullTranscendiumSet(player);
   }

   /**
    * 相位偏移（穿墙 + 观察者式视觉）的判定：
    * 穿齐全套超限合金套 + 玩家按键开启相位偏移（默认关，状态持久化）。
    * 开启即激活横向穿墙；飞行中升级为全向穿透，非飞行时垂直碰撞保持原版
    * （幻灵 mode1 同款），脚下有方块即可站立，不会坠入地下。
    */
   public static boolean isTranscendiumFlightPhasing(Player player) {
      return IonocraftBoots.isTranscendiumFlightPhasing(player);
   }

   /**
    * 是否为铁砧工艺的太空维度（全套超限"无视星球引力"仅在这些维度生效）：
    * 月球 anvilcraft:mun、锻星砧类地星球 anvilcraft:overworld_like、
    * 锻星砧虚空星球 anvilcraft:void_planet。
    * 普通维度（主世界/下界/末地）绝不命中，保持原版重力。
    */
   public static boolean isAnvilSpaceDimension(Entity entity) {
      return ArmorSets.isAnvilSpaceDimension(entity);
   }

   public static boolean ignoresBlockSlowdown(Entity entity) {
      return ArmorSets.ignoresBlockSlowdown(entity);
   }

   public static void saveToggleStates(Player player) {
      CompoundTag data = player.getPersistentData();
      UUID uuid = player.getUUID();
      data.putInt("dingdongji:helmet_mode", HELMET_MODE.getOrDefault(uuid, 5));
      SurfaceWalking.save(data, uuid);
      SpectralPhase.save(data, uuid);
      TranscendiumSet.save(data, uuid);
      ComfortableAbility.save(data, uuid);
      NeutronBarrier.save(data, uuid);
      IonocraftBoots.save(data, uuid);
      JiSetAbility.save(data, uuid);
   }

   private static void loadToggleStates(Player player) {
      CompoundTag data = player.getPersistentData();
      UUID uuid = player.getUUID();
      int helmetMode = data.contains("dingdongji:helmet_mode") ? data.getInt("dingdongji:helmet_mode") : 5;
      HELMET_MODE.put(uuid, helmetMode);
      GlowingVision.GLOWING_VISION_ENABLED.put(uuid, helmetMode == 0 || helmetMode == 4);
      SurfaceWalking.load(data, uuid);
      SpectralPhase.load(data, uuid);
      TranscendiumSet.load(data, uuid);
      ComfortableAbility.load(data, uuid);
      NeutronBarrier.load(data, uuid);
      JiSetAbility.load(data, uuid);
      IonocraftBoots.load(data, uuid);
   }
}
