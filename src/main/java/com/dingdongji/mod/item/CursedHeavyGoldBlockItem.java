package com.dingdongji.mod.item;

import dev.dubhe.anvilcraft.api.amulet.AmuletManager;
import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import dev.dubhe.anvilcraft.item.abnormal.IAbnormal;
import dev.dubhe.anvilcraft.item.abnormal.ICursed;
import dev.dubhe.anvilcraft.item.abnormal.IEnchantedGold;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

// 诅咒重金块：debuff 给予方式与本体诅咒金块一致（按携带个数分级，移除后延迟消失），
// 效果等级翻倍（虚弱IV/缓慢IV/饥饿IV）；携带一整组时每 5 秒引雷（附近有避雷针则豁免）
public class CursedHeavyGoldBlockItem extends BlockItem implements ICursed {
   private static final Map<UUID, Long> LAST_LIGHTNING = new HashMap<>();
   private static final long LIGHTNING_INTERVAL_TICKS = 100L;
   private static final int LIGHTNING_ROD_SEARCH_RANGE = 128;

   public CursedHeavyGoldBlockItem(Block block, Properties properties) {
      super(block, properties);
   }

   // 与本体诅咒金块同款分级（1-8 个仅虚弱，9-64 个加缓慢，65 个起加饥饿），
   // 效果等级翻倍为 IV 级
   @Override
   public void addEffect(Player player) {
      if (IEnchantedGold.isCarryingEnchantedGold(player)) {
         return;
      }
      player.addEffect(IAbnormal.makeEffectInstance(MobEffects.WEAKNESS, 3));
      int count = getItemCount(player);
      if (count > 8) {
         player.addEffect(IAbnormal.makeEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 3));
      }
      if (count > 64) {
         player.addEffect(IAbnormal.makeEffectInstance(MobEffects.HUNGER, 3));
      }
   }

   @Override
   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
      super.inventoryTick(stack, level, entity, slot, selected);
      ICursed.super.inventoryTick(stack, level, entity, slot, selected);
      if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof Player player)) {
         return;
      }
      if (player.getAbilities().instabuild || player.getAbilities().invulnerable) {
         return;
      }
      if (IEnchantedGold.isCarryingEnchantedGold(player)) {
         return;
      }
      AmuletEffectContext ctx = new AmuletEffectContext();
      AmuletManager.get(level.registryAccess()).trigger(player, ctx);
      if (ctx.getOrDefault(ModAmuletEffectContextKeys.IMMUNE_ABNORMAL_ITEM, false)) {
         return;
      }
      if (IAbnormal.getAbnormalCount(player, CursedHeavyGoldBlockItem.class) < 64) {
         return;
      }
      long now = level.getGameTime();
      Long last = LAST_LIGHTNING.get(player.getUUID());
      if (last != null && now - last < LIGHTNING_INTERVAL_TICKS) {
         return;
      }
      BlockPos pos = player.blockPosition();
      // 附近有避雷针时，雷电劈在最近的避雷针上而非玩家身上
      java.util.Optional<BlockPos> rodPos = serverLevel
         .getPoiManager()
         .findClosest(holder -> holder.is(PoiTypes.LIGHTNING_ROD), pos, LIGHTNING_ROD_SEARCH_RANGE, PoiManager.Occupancy.ANY);
      BlockPos strikePos = rodPos.orElse(pos);
      LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel);
      if (bolt == null) {
         return;
      }
      bolt.moveTo(Vec3.atBottomCenterOf(strikePos));
      serverLevel.addFreshEntity(bolt);
      LAST_LIGHTNING.put(player.getUUID(), now);
   }
}
