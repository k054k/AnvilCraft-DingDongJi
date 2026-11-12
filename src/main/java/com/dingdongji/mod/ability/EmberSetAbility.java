package com.dingdongji.mod.ability;

import com.dingdongji.mod.item.ModItems;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class EmberSetAbility {
   static final Map<UUID, Long> EMBER_SET_LAST_HEAL = new HashMap<>();
   private static final int EMBER_SET_HEAL_INTERVAL = 200;
   private static final Map<UUID, Map<Holder<MobEffect>, Boolean>> MOD_ADDED_EFFECTS = new HashMap<>();

   private static void markEffectAdded(Player player, Holder<MobEffect> effect) {
      MOD_ADDED_EFFECTS.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(effect, true);
   }

   public static void addHiddenEffect(Player player, Holder<MobEffect> effect, int amplifier) {
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

   public static void removeOwnEffectAt(Player player, Holder<MobEffect> effect, int amplifier) {
      Map<Holder<MobEffect>, Boolean> playerEffects = MOD_ADDED_EFFECTS.get(player.getUUID());
      if (playerEffects != null && playerEffects.containsKey(effect)) {
         MobEffectInstance existing = player.getEffect(effect);
         if (existing != null && existing.getAmplifier() == amplifier) {
            player.removeEffect(effect);
         }

         playerEffects.remove(effect);
      }
   }

   /**
    * 余烬金属全套：处于火焰/熔岩/营火中时持续恢复生命（统一再生 I，
    * 不再有灵魂火翻倍）。火焰伤害免疫见伤害事件判定。
    */
   public static void handleEmberSet(Player player) {
      UUID uuid = player.getUUID();
      if (!ArmorSets.hasFullEmberSet(player)) {
         EMBER_SET_LAST_HEAL.remove(uuid);
         return;
      }
      Level level = player.level();
      BlockPos pos = player.blockPosition();
      boolean inHeat = player.isOnFire()
         || player.isInLava()
         || isFireBlock(level, pos)
         || isFireBlock(level, pos.above())
         || isFireBlock(level, pos.below());
      if (!inHeat) {
         EMBER_SET_LAST_HEAL.remove(uuid);
         return;
      }
      long now = player.level().getGameTime();
      if (now - EMBER_SET_LAST_HEAL.getOrDefault(uuid, -999L) >= (long)EMBER_SET_HEAL_INTERVAL) {
         player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 260, 1, false, false, false));
         EMBER_SET_LAST_HEAL.put(uuid, now);
      }
   }

   public static void handleEmberBootsFirePath(Player player) {
      ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
      if (boots.is((Item)ModItems.EMBER_METAL_BOOTS.get())) {
         if (!player.level().isClientSide
            && SurfaceWalking.LAVA_WALKER_ENABLED.getOrDefault(player.getUUID(), false)) {
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

   public static void handleEmberArmorRepair(Player player) {
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

   public static void clearOnLogout(UUID uuid) {
      EMBER_SET_LAST_HEAL.remove(uuid);
      MOD_ADDED_EFFECTS.remove(uuid);
   }

   public static void save(CompoundTag data, UUID uuid) {
   }

   public static void load(CompoundTag data, UUID uuid) {
   }
}
