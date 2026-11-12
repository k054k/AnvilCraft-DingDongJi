package com.dingdongji.mod.entity;

import com.dingdongji.mod.event.PhantomDecoyManager;
import com.dingdongji.mod.init.ModEntities;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;

/**
 * 幻灵套装"幻影"留在原地的替身实体：
 * 外形是原玩家的渲染分身（客户端 PlayerRenderer 渲染），继承原玩家
 * 生命与装备；不自主移动、不被推动、不自然消失、不存盘，但正常受重力
 * 影响——激活幻影时若脚下无方块，替身会自然下坠直到落地。
 * 受伤扣除自身血量（血量池）并立即结束幻影：本体传回替身位置，
 * 传回后本体血量 = 替身剩余血量；血量归零时死亡，本体随之死亡。
 */
public class PhantomDecoyEntity extends PathfinderMob {
   private static final net.minecraft.network.syncher.EntityDataAccessor<Optional<UUID>> OWNER =
      SynchedEntityData.defineId(PhantomDecoyEntity.class, EntityDataSerializers.OPTIONAL_UUID);

   public PhantomDecoyEntity(EntityType<? extends PathfinderMob> type, Level level) {
      super(type, level);
      // 注意：不能 setNoAi(true)！noAi 生物在服务端 isControlledByLocalInstance
      // 返回 false，LivingEntity.travel 会被整体跳过——重力（travel 内应用）
      // 与移动全部失效，替身会悬浮在原地。构造时不注册任何 goal，AI 空转
      // 无副作用；朝向/动画由 tick() 每帧锁死。
      this.setPersistenceRequired();
   }

   public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
      // 最大生命基准与普通玩家一致（20）；生成时再按主人实时最大生命值覆写。
      return PathfinderMob.createMobAttributes()
         .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.0)
         .add(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, 1.0)
         .add(net.minecraft.world.entity.ai.attributes.Attributes.SCALE, 1.0);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(OWNER, Optional.empty());
   }

   public void setOwner(UUID owner) {
      this.entityData.set(OWNER, Optional.ofNullable(owner));
   }

   public UUID getOwner() {
      return this.entityData.get(OWNER).orElse(null);
   }

   /** 出生位置与朝向初始化（位置不再锁定：替身正常受重力下坠、落地）。 */
   public void anchorAt(double x, double y, double z) {
      this.setPosRaw(x, y, z);
      // 定格激活幻影瞬间的朝向，之后永久锁死（替身不转头、不回头、不做任何动画）
      this.finalYaw = this.getYRot();
   }

   private float finalYaw;

   @Override
   public void tick() {
      super.tick();
      // 双端每帧锁死朝向与走路动画：替身是"留在原地的定格剪影"，
      // 不随幻影状态玩家转动镜头，也不播放摆臂/抬头等任何动作。
      // 服务端以 anchorAt 定格的 finalYaw 为准；客户端以服务端同步来的
      // 朝向为准（两者相等），此后不再变化。
      float yaw = this.level().isClientSide ? this.getYRot() : this.finalYaw;
      this.setYRot(yaw);
      this.yRotO = yaw;
      this.setXRot(0.0F);
      this.xRotO = 0.0F;
      this.yHeadRot = yaw;
      this.yHeadRotO = yaw;
      this.yBodyRot = yaw;
      this.yBodyRotO = yaw;
      this.walkAnimation.setSpeed(0.0F);
      this.attackAnim = 0.0F;
      this.oAttackAnim = 0.0F;
      this.setSpeed(0.0F);
      if (!this.level().isClientSide) {
         UUID owner = this.getOwner();
         if (owner == null || !PhantomDecoyManager.sessionExists(owner)) {
            // 所属幻影状态已结束（下线/死亡/重载后无主）
            this.discard();
         }
      }
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (this.level().isClientSide || amount <= 0.0F) {
         return true;
      }
      if (source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.GENERIC_KILL)) {
         return false;
      }
      // 受伤反馈（红闪/声音），不击退（位置锚定）
      if (this.level() instanceof ServerLevel serverLevel) {
         serverLevel.broadcastEntityEvent(this, (byte) 2);
         this.playHurtSound(source);
      }
      // 创造模式：替身无敌（实时跟随主人当前模式）
      UUID ownerId = this.getOwner();
      Player owner = ownerId == null ? null : this.ownerPlayer(ownerId);
      if (owner != null && owner.isCreative()) {
         return true;
      }
      // 玩家攻击者：遵循 PVP 游戏规则与队伍关系——
      // 服务器关闭 PVP，或攻击者与主人同队（或同队联盟）时，替身不可被攻击；
      // 但替身主人本人除外：等同自伤，不受 PVP/队伍限制，可正常打自己的替身
      if (source.getEntity() instanceof Player attacker && this.level() instanceof ServerLevel serverLevel
            && !attacker.getUUID().equals(ownerId)) {
         if (!serverLevel.getServer().isPvpAllowed()) {
            return true;
         }
         var ownerTeam = owner == null ? null : owner.getTeam();
         if (ownerTeam != null && ownerTeam.isAlliedTo(attacker.getTeam())) {
            return true;
         }
      }
      // 真实血量池：直接扣自身血量，归零走原版死亡流程（由幻影管理器
      // 监听替身死亡并让本体传回同死）
      float newHealth = this.getHealth() - amount;
      this.setHealth(Math.max(0.0F, newHealth));
      if (this.isDeadOrDying()) {
         // 血量池归零：本体传回替身位置并随之死亡（同伤害源）
         this.die(source);
      } else {
         // 受伤即还原：本体立即传回替身位置（该次伤害由血量池结算，
         // 传回后本体血量 = 替身剩余血量，动量/药水按快照清理）
         PhantomDecoyManager.onDecoyHurt(this);
      }
      return true;
   }

   @Override
   public void die(DamageSource source) {
      super.die(source);
      if (!this.level().isClientSide) {
         PhantomDecoyManager.onDecoyDeath(this, source);
      }
   }

   private Player ownerPlayer(UUID uuid) {
      if (this.level() instanceof ServerLevel level) {
         return level.getServer().getPlayerList().getPlayer(uuid);
      }
      return null;
   }

   @Override
   public boolean isPushable() {
      return false;
   }

   @Override
   protected void pushEntities() {
   }

   @Override
   public boolean fireImmune() {
      return true;
   }

   @Override
   public boolean canBeLeashed() {
      return false;
   }

   @Override
   protected InteractionResult mobInteract(Player player, InteractionHand hand) {
      return InteractionResult.PASS;
   }

   @Override
   public boolean removeWhenFarAway(double distanceToClosestPlayer) {
      return false;
   }

   @Override
   public boolean canHoldItem(ItemStack stack) {
      return false;
   }

   @Override
   protected boolean shouldDropLoot() {
      return false;
   }

   @Override
   protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
   }

   @Override
   public boolean save(CompoundTag tag) {
      return false;
   }

   /** 只同步原玩家的装备/手持（手部变化）；朝向与姿势保持生成时的状态，替身不晃动。 */
   public void ddj$copyAppearance(Player owner) {
      for (EquipmentSlot slot : EquipmentSlot.values()) {
         ItemStack target = owner.getItemBySlot(slot);
         if (!ItemStack.matches(this.getItemBySlot(slot), target)) {
            this.setItemSlot(slot, target.copy());
         }
      }
   }

   /** 替身不受任何状态效果：效果跳伤害（中毒/凋零等 tick）不应触发"替身受伤→玩家传回复原"。 */
   @Override
   public boolean addEffect(net.minecraft.world.effect.MobEffectInstance effect, Entity source) {
      return false;
   }

   public static EntityType<PhantomDecoyEntity> type() {
      return ModEntities.PHANTOM_DECOY.get();
   }
}
