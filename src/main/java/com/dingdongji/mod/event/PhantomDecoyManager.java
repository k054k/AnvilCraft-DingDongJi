package com.dingdongji.mod.event;

import com.dingdongji.mod.entity.PhantomDecoyEntity;
import com.dingdongji.mod.platform.ClientViews;
import com.dingdongji.mod.init.ModEntities;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;

/**
 * 幻灵套完整套装"幻影"状态的服务端会话管理（双端判定入口）。
 *
 * 激活：原地生成一个继承玩家生命与装备渲染的替身（替身拥有独立血量池）；
 * 玩家本体不再隐身，客户端把本体渲染为幻灵色半透明
 * （PhantomPlayerRenderHandler），可自由穿墙行动。
 * 结束：替身受伤/死亡/本体受伤/切换装备/再按键/被清除等→本体传回替身位置，
 * 清除幻影期间积累的动量/坠落/火焰，药水效果按开启时快照恢复，血量以替身
 * 血量池为准（替身受到的伤害由血量池结算；血量归零时本体随之死亡）。
 * 幻影中不能跨维度、不能攻击。
 */
public final class PhantomDecoyManager {
   private static final Map<UUID, Session> SESSIONS = new HashMap<>();

   private PhantomDecoyManager() {
   }

   /**
    * 幻影状态广播：发给追踪该玩家的所有客户端（含本人），
    * 使所有观察者都把幻影玩家本体渲染为幻灵色半透明。
    */
   private static void broadcastPhantomState(ServerPlayer player, boolean state) {
      net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(
         player, new com.dingdongji.mod.network.PhantomStateSyncPacket(player.getUUID(), state));
   }

   /** 玩家登录时，把当前同维度所有幻影玩家的状态补给该客户端（防止中途加入看不到）。 */
   public static void syncAllPhantomStatesTo(ServerPlayer joining) {
      for (Map.Entry<UUID, Session> entry : SESSIONS.entrySet()) {
         Player owner = joining.level().getPlayerByUUID(entry.getKey());
         if (owner != null && owner.level() == joining.level()) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
               joining, new com.dingdongji.mod.network.PhantomStateSyncPacket(entry.getKey(), true));
         }
      }
   }

   private static final class Session {
      final PhantomDecoyEntity decoy;
      long lastRetarget;
      /** 会话开始的游戏时刻（用于药水快照剩余时长折算） */
      final long startTick;
      /** 开启幻影时的药水效果快照：传回时清除全部效果后按剩余时长重新施加。 */
      final List<net.minecraft.world.effect.MobEffectInstance> effectSnapshot;
      /** 近战模拟：mobId -> 下次允许攻击的游戏时刻 */
      final Map<Integer, Long> meleeCooldown = new HashMap<>();

      Session(PhantomDecoyEntity decoy, long startTick, List<net.minecraft.world.effect.MobEffectInstance> effectSnapshot) {
         this.decoy = decoy;
         this.startTick = startTick;
         this.effectSnapshot = effectSnapshot;
      }
   }

   /** 双端判定：该玩家当前是否处于幻影状态（客户端仅本地玩家有效）。 */
   public static boolean isPhantomState(Player player) {
      if (player == null) {
         return false;
      }
      if (player.level().isClientSide) {
         return ClientViews.get().isPhantom(player);
      }
      return SESSIONS.containsKey(player.getUUID());
   }

   public static boolean sessionExists(UUID owner) {
      return SESSIONS.containsKey(owner);
   }

   public static void toggle(ServerPlayer player) {
      if (SESSIONS.containsKey(player.getUUID())) {
         end(player, true);
         player.displayClientMessage(
            ModArmorSetHandler.actionMessage("幻影", "关", ChatFormatting.RED), true
         );
         return;
      }
      start(player);
      // start 内部在未穿全套时仅提示、不注册会话；只有真正开启成功才报"开"
      if (SESSIONS.containsKey(player.getUUID())) {
         player.displayClientMessage(
            ModArmorSetHandler.actionMessage("幻影", "开", ChatFormatting.GREEN), true
         );
      }
   }

   private static void start(ServerPlayer player) {
      if (!ModArmorSetHandler.hasFullSpectralSet(player)) {
         player.displayClientMessage(
            Component.literal("幻影需要穿戴全套幻灵盔甲").withStyle(ChatFormatting.RED), true
         );
         return;
      }
      ServerLevel level = (ServerLevel) player.level();
      PhantomDecoyEntity decoy = new PhantomDecoyEntity(ModEntities.PHANTOM_DECOY.get(), level);
      decoy.setOwner(player.getUUID());
      decoy.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
      decoy.anchorAt(player.getX(), player.getY(), player.getZ());
      // 最大生命值与玩家实时一致（含生命提升/饰品等修正后的最终值），再同步当前血量
      var ownerMaxHealth = player.getAttribute(Attributes.MAX_HEALTH);
      var decoyMaxHealth = decoy.getAttribute(Attributes.MAX_HEALTH);
      if (ownerMaxHealth != null && decoyMaxHealth != null) {
         decoyMaxHealth.setBaseValue(ownerMaxHealth.getValue());
      }
      decoy.setHealth(player.getHealth());
      // Jade 等信息显示玩家自己的名字；不显示悬浮名牌
      decoy.setCustomName(Component.literal(player.getGameProfile().getName()));
      decoy.setCustomNameVisible(false);
      decoy.ddj$copyAppearance(player);
      List<net.minecraft.world.effect.MobEffectInstance> snapshot = new java.util.ArrayList<>();
      for (net.minecraft.world.effect.MobEffectInstance inst : player.getActiveEffects()) {
         snapshot.add(new net.minecraft.world.effect.MobEffectInstance(inst));
      }
      SESSIONS.put(player.getUUID(), new Session(decoy, level.getGameTime(), snapshot));
      level.addFreshEntity(decoy);
      ModArmorSetHandler.syncAbilityState(player);
      broadcastPhantomState(player, true);
   }

   /**
    * @param teleport 是否把玩家传送回替身位置（装备变化/再按键/替身死亡=true；
    *                 玩家自己受伤=false，幻影原地消失）
    */
   public static void end(ServerPlayer player, boolean teleport) {
      end(player, teleport, null);
   }

   /**
    * 结束幻影并把“视角恢复回替身”：
    * 清除幻影期间积累的一切（动量/坠落/火焰/新增药水），血量以替身血量池为准；
    * killSource 非空表示替身被击杀，本体传回替身位置后随之死亡。
    *
    * @param teleport 是否把玩家传送回替身位置
    * @param killSource 替身死亡的伤害源（null = 非死亡结束）
    */
   public static void end(ServerPlayer player, boolean teleport, DamageSource killSource) {
      Session session = SESSIONS.remove(player.getUUID());
      if (session == null) {
         return;
      }
      PhantomDecoyEntity decoy = session.decoy;
      double x = decoy.getX();
      double y = decoy.getY();
      double z = decoy.getZ();
      float yaw = decoy.getYRot();
      float pitch = player.getXRot();
      float decoyHealth = decoy.getHealth();
      if (decoy.isAlive()) {
         decoy.discard();
      }
      if (teleport) {
         player.teleportTo((ServerLevel) player.level(), x, y, z, java.util.EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class), yaw, pitch);
         // 移除幻影状态积累的动量与环境伤害源：传回落地不摔、不烧
         player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
         player.fallDistance = 0.0F;
         player.clearFire();
         if (killSource == null) {
            // 药水效果不继承：清除全部，按快照剩余时长重新施加开启时的效果
            long elapsed = player.level().getGameTime() - session.startTick;
            player.removeAllEffects();
            for (net.minecraft.world.effect.MobEffectInstance inst : session.effectSnapshot) {
               net.minecraft.world.effect.MobEffectInstance copy = new net.minecraft.world.effect.MobEffectInstance(inst);
               if (!copy.isInfiniteDuration()) {
                  long remain = copy.getDuration() - elapsed;
                  if (remain <= 0) {
                     continue;
                  }
                  copy.mapDuration(d -> (int) remain);
               }
               player.addEffect(copy);
            }
            // 血量不继承幻影期间变化：以替身血量池为准
            player.setHealth(Math.min(Math.max(0.1F, decoyHealth), player.getMaxHealth()));
         }
      }
      if (killSource != null) {
         // 替身被击杀：本体传回替身位置后随之死亡
         player.hurt(killSource, Float.MAX_VALUE);
      }
      player.setNoGravity(false);
      ModArmorSetHandler.syncAbilityState(player);
      broadcastPhantomState(player, false);
   }

   /** 替身受伤（未致死）：本体立即传回替身位置，幻影结束。 */
   public static void onDecoyHurt(PhantomDecoyEntity decoy) {
      UUID ownerId = decoy.getOwner();
      if (ownerId == null || !(decoy.level() instanceof ServerLevel level)) {
         return;
      }
      ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
      if (owner == null || !SESSIONS.containsKey(ownerId)) {
         return;
      }
      end(owner, true);
   }

   /** 替身被击杀：本体传回替身位置并随之死亡（血量池归零）。 */
   public static void onDecoyDeath(PhantomDecoyEntity decoy, DamageSource source) {
      UUID ownerId = decoy.getOwner();
      if (ownerId == null || !(decoy.level() instanceof ServerLevel level)) {
         return;
      }
      ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
      if (owner == null || !SESSIONS.containsKey(ownerId)) {
         return;
      }
      end(owner, true, source);
   }

   /** 玩家本体受伤：幻影立即消失并传回替身位置（幻影期间的伤害不结算，血量以替身血量池为准）。 */
   public static void onOwnerHurt(ServerPlayer player) {
      if (SESSIONS.containsKey(player.getUUID())) {
         end(player, true);
      }
   }

   /** 穿脱/切换任意装备：立即传回替身位置并结束。 */
   public static void onEquipmentChanged(Player player) {
      if (player instanceof ServerPlayer serverPlayer && SESSIONS.containsKey(player.getUUID())) {
         end(serverPlayer, true);
      }
   }

   /** 幻影状态尝试跨维度：拦截。返回 true 表示已取消。 */
   public static boolean onTravelAttempt(Entity entity) {
      if (entity instanceof ServerPlayer player && SESSIONS.containsKey(player.getUUID())) {
         player.displayClientMessage(
            Component.literal("幻影状态下无法进入其他维度").withStyle(ChatFormatting.RED), true
         );
         return true;
      }
      return false;
   }

   @SubscribeEvent
   public static void onEntityTravelToDimension(EntityTravelToDimensionEvent event) {
      if (onTravelAttempt(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   public static void onLogout(ServerPlayer player) {
      if (SESSIONS.containsKey(player.getUUID())) {
         // 退出登录认替身：传回替身位置并做与手动关闭一致的全部清理
         end(player, true);
      }
   }

   /** 每 tick（服务端玩家 tick）维护会话。 */
   public static void serverTick(ServerPlayer player) {
      Session session = SESSIONS.get(player.getUUID());
      if (session == null) {
         return;
      }
      PhantomDecoyEntity decoy = session.decoy;
      if (!decoy.isAlive()) {
         // 替身因被击杀而消失的场景已由 onDecoyDeath 处理（含同死）；
         // 这里兜底其他消失原因（被清除/卸载等）：仅传回，不同死
         end(player, true);
         return;
      }
      if (player.isDeadOrDying() || !player.isAlive()) {
         // 玩家自身死亡时不传送（尸体会由原版死亡流程处理）
         end(player, false);
         return;
      }
      decoy.ddj$copyAppearance(player);

      long now = player.level().getGameTime();
      boolean creative = player.isCreative();
      if (now - session.lastRetarget >= 10L) {
         session.lastRetarget = now;
         if (creative) {
            releaseNearbyHostiles(player, session, decoy);
         } else {
            redirectNearbyHostiles(player, decoy);
         }
      }
      if (!creative) {
         emulateMeleeHits((ServerLevel) player.level(), session, decoy, now);
      }
   }

   /** 创造模式：替身不吸引仇恨——释放一切已锁定替身的怪物目标并清空近战冷却。 */
   private static void releaseNearbyHostiles(ServerPlayer player, Session session, PhantomDecoyEntity decoy) {
      session.meleeCooldown.clear();
      ServerLevel level = (ServerLevel) player.level();
      AABB area = player.getBoundingBox().inflate(28.0);
      for (Mob mob : level.getEntitiesOfClass(Mob.class, area)) {
         if (mob.getTarget() == decoy) {
            mob.setTarget(null);
         }
      }
   }

   /**
    * 僵尸等近战怪物的攻击目标只接受 Player，无法直接攻击替身；
    * 对所有已把替身当作目标、进入攻击距离且有视线的近战敌人，按
    * 20 tick 冷却模拟一次近战攻击（伤害取其攻击伤害属性）。
    */
   private static void emulateMeleeHits(ServerLevel level, Session session, PhantomDecoyEntity decoy, long now) {
      UUID ownerId = decoy.getOwner();
      Player owner = ownerId == null ? null : level.getPlayerByUUID(ownerId);
      net.minecraft.world.scores.Team ownerTeam = owner == null ? null : owner.getTeam();
      AABB area = decoy.getBoundingBox().inflate(2.5);
      for (Mob mob : level.getEntitiesOfClass(Mob.class, area)) {
         if (!(mob instanceof Enemy) || isNeutralMob(mob) || mob.getTarget() != decoy || !mob.hasLineOfSight(decoy)
               || (ownerTeam != null && ownerTeam.isAlliedTo(mob.getTeam()))) {
            continue;
         }
         double w = mob.getBbWidth() * 2.0F;
         double limit = w * w + decoy.getBbWidth();
         if (mob.distanceToSqr(decoy) > limit) {
            continue;
         }
         Long next = session.meleeCooldown.get(mob.getId());
         if (next != null && now < next) {
            continue;
         }
         session.meleeCooldown.put(mob.getId(), now + 20L);
         float damage = 2.0F;
         var attr = mob.getAttribute(Attributes.ATTACK_DAMAGE);
         if (attr != null) {
            damage = (float) attr.getValue();
         }
         mob.swing(InteractionHand.MAIN_HAND);
         decoy.hurt(mob.damageSources().mobAttack(mob), Math.max(1.0F, damage));
         if (!decoy.isAlive()) {
            return;
         }
      }
   }

   /** 中立生物不会主动攻击替身——末影人、僵尸猪灵、蜜蜂、蜘蛛等只在被激怒时才敌对。 */
   private static boolean isNeutralMob(Mob mob) {
      return mob instanceof EnderMan
         || mob instanceof ZombifiedPiglin
         || mob instanceof Bee
         || mob instanceof Spider
         || mob instanceof CaveSpider
         || mob instanceof PolarBear
         || mob instanceof Wolf
         || mob instanceof Goat
         || mob instanceof Dolphin;
   }

   private static void redirectNearbyHostiles(ServerPlayer player, PhantomDecoyEntity decoy) {
      ServerLevel level = (ServerLevel) player.level();
      AABB area = player.getBoundingBox().inflate(28.0);
      net.minecraft.world.scores.Team ownerTeam = player.getTeam();
      for (Mob mob : level.getEntitiesOfClass(Mob.class, area)) {
         // 与主人同队（或同队联盟）的生物不仇恨替身
         if (ownerTeam != null && ownerTeam.isAlliedTo(mob.getTeam())) {
            continue;
         }
         LivingEntity target = mob.getTarget();
         // 只重定向完全敌对的生物（排除中立生物），且仅当该怪当前无目标时
         if ((mob instanceof Enemy)
               && !isNeutralMob(mob)
               && target == null
               && mob.distanceToSqr(decoy) <= 16.0 * 16.0
               && mob.hasLineOfSight(decoy)) {
            mob.setTarget(decoy);
         }
      }
   }
}
