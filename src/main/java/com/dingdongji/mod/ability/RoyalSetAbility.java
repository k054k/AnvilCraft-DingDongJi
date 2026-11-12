package com.dingdongji.mod.ability;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public class RoyalSetAbility {
   /** 皇家钢全套：隐藏再生 I（原皇家钢胸甲单件效果，0.1.0 改为全套）。 */
   public static void handleRoyalSet(Player player) {
      if (ArmorSets.hasFullRoyalSet(player)) {
         EmberSetAbility.addHiddenEffect(player, MobEffects.REGENERATION, 0);
      } else {
         EmberSetAbility.removeOwnEffectAt(player, MobEffects.REGENERATION, 0);
      }
   }

   public static void clearOnLogout(UUID uuid) {
   }

   public static void save(CompoundTag data, UUID uuid) {
   }

   public static void load(CompoundTag data, UUID uuid) {
   }
}
