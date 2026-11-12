package com.dingdongji.mod.mixin;

import com.dingdongji.mod.util.BeaconBaseHelper;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 玩家在信标 GUI 中放入支付物确认效果时的两条转化路径：
 *
 * <p>1. 附魔金锭 + 附魔金块/重质附魔金块底座 → 铭化信标（本类全权处理）。
 *    附魔金块走本体原表 2%/5%/20%/100%，重质附魔金块走重质表 18%/45%/100%/100%。
 *
 * <p>2. 诅咒金锭 + 重质诅咒金块底座 → 腐化信标。本体自己的 BeaconMenuMixin
 * 只统计纯诅咒金块（字节码实证 anvilcraft$updateBase 硬编码 CURSED_GOLD_BLOCK），
 * 重质（或金块/重质混用）底座层数恒为 0、永远不会掷骰。这里在「纯诅咒金块
 * 层数为 0」（即本体必定不掷骰）时按扩展底座（诅咒金块或重质诅咒金块）补判，
 * 重质走 18%/45%/100%/100%；纯诅咒金块底座完全交给本体，绝不二次掷骰。
 * 转化成功流程与本体逐行一致（TriggerUtil + 替换方块 + 雷暴）。
 *
 * <p>3. 4 层免锭特典：支付槽为空 + 诅咒侧底座铺满 4 层 → 直接转化腐化信标
 * （客户端由 BeaconConfirmButtonMixin 放行原版会禁用的确认按钮）。
 *
 * <p>转化成功后不取消原版流程：支付物被正常消耗、效果发放因信标方块已消失
 * 而静默跳过，与本体转化表现一致；掷骰失败则一切照旧。
 */
@Mixin(BeaconMenu.class)
public abstract class BeaconMenuMixin {
   private static Item ddj$enchantedGoldIngot;
   private static Item ddj$cursedGoldIngot;

   @Shadow
   @Final
   private ContainerLevelAccess access;

   @Inject(method = "updateEffects", at = @At("HEAD"))
   private void ddj$convertOnPayment(Optional<Holder<MobEffect>> primaryEffect,
                                     Optional<Holder<MobEffect>> secondaryEffect,
                                     CallbackInfo ci) {
      // 支付槽（包私有 PaymentSlot）是 BeaconMenu 构造时第一个 addSlot 的槽位，
      // 直接经 AbstractContainerMenu.getSlot(0) 取支付物，避免 @Shadow 包私有类型
      ItemStack payment = ((AbstractContainerMenu) (Object) this).getSlot(0).getItem();
      if (payment.isEmpty()) {
         // 4 层免锭特典（用户参数：四层 100% 无需放诅咒金锭）：
         // 空支付 + 诅咒金块/重质诅咒金块底座 4 层 → 直接转化腐化信标
         this.access.execute((level, pos) -> {
            if (!(level instanceof ServerLevel serverLevel)
                  || !level.getBlockState(pos).is(Blocks.BEACON)) {
               return;
            }
            if (BeaconBaseHelper.countCursedOrHeavyLayers(serverLevel, pos) >= 4) {
               BeaconBaseHelper.convertToCorrupted(serverLevel, pos);
            }
         });
         return;
      }
      boolean enchantedIngot = payment.is(ddj$enchantedGoldIngot());
      boolean cursedIngot = payment.is(ddj$cursedGoldIngot());
      if (!enchantedIngot && !cursedIngot) {
         return;
      }
      this.access.execute((level, pos) -> {
         if (!(level instanceof ServerLevel serverLevel)
               || !level.getBlockState(pos).is(Blocks.BEACON)) {
            return;
         }
         if (enchantedIngot) {
            // 附魔金块与重质附魔金块拆开掷骰：纯金块走本体原表，纯重质走重质表，
            // 混合底座按其中更高的概率取（重质是升级材料）
            int goldLayers = BeaconBaseHelper.countPureEnchantedGoldLayers(serverLevel, pos);
            int heavyLayers = BeaconBaseHelper.countHeavyEnchantedLayers(serverLevel, pos);
            if (goldLayers > 0 && ddj$roll(serverLevel, goldLayers)) {
               BeaconBaseHelper.convertToAnchored(serverLevel, pos);
               return;
            }
            if (heavyLayers > 0 && ddj$rollHeavy(serverLevel, heavyLayers)) {
               BeaconBaseHelper.convertToAnchored(serverLevel, pos);
            }
            return;
         }
         // 诅咒金锭：纯诅咒金块底座由本体掷骰（层数 >0 即跳过，避免双份掷骰）；
         // 本体统计为 0 时再按诅咒金块/重质诅咒金块扩展底座补判
         int pureLayers = BeaconBaseHelper.countLayers(
            serverLevel, pos, BeaconBaseHelper.cursedGoldBlock()
         );
         if (pureLayers > 0) {
            return;
         }
         int heavyLayers = BeaconBaseHelper.countCursedOrHeavyLayers(serverLevel, pos);
         if (heavyLayers > 0 && ddj$rollHeavy(serverLevel, heavyLayers)) {
            BeaconBaseHelper.convertToCorrupted(serverLevel, pos);
         }
      });
   }

   /** 附魔金块底座概率：1 层 2% / 2 层 5% / 3 层 20% / 4 层 100%。 */
   private static boolean ddj$roll(ServerLevel level, int layers) {
      Float chance = BeaconBaseHelper.GUI_CONVERT_CHANCE.get(layers);
      return chance != null && level.getRandom().nextFloat() < chance;
   }

   /** 重质附魔金块底座概率：1 层 18% / 2 层 45% / 3 层 100% / 4 层 100%。 */
   private static boolean ddj$rollHeavy(ServerLevel level, int layers) {
      Float chance = BeaconBaseHelper.HEAVY_GUI_CONVERT_CHANCE.get(layers);
      return chance != null && level.getRandom().nextFloat() < chance;
   }

   /** 本体附魔金锭。 */
   private static Item ddj$enchantedGoldIngot() {
      if (ddj$enchantedGoldIngot == null) {
         ddj$enchantedGoldIngot = BuiltInRegistries.ITEM.get(
            ResourceLocation.fromNamespaceAndPath("anvilcraft", "enchanted_gold_ingot")
         );
      }
      return ddj$enchantedGoldIngot;
   }

   /** 本体诅咒金锭。 */
   private static Item ddj$cursedGoldIngot() {
      if (ddj$cursedGoldIngot == null) {
         ddj$cursedGoldIngot = BuiltInRegistries.ITEM.get(
            ResourceLocation.fromNamespaceAndPath("anvilcraft", "cursed_gold_ingot")
         );
      }
      return ddj$cursedGoldIngot;
   }
}
