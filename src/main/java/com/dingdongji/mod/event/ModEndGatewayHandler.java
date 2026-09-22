package com.dingdongji.mod.event;

import com.dingdongji.mod.item.ModItems;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * 监听物品掉落物进入末地维度（玩家把套装部位丢进末地门，传送完成后
 * 在末地生成 ItemEntity 时触发）。按概率转换为对应的幻灵装备，未中
 * 部分转换为末地尘。
 *
 * 各套装概率与 AnvilCraft 数据包 portal_conversion 中对应材料砧变幻灵
 * 砧的概率一致：铁套 3%（铁砧）、皇家钢套 50%（皇家砧）、余烬金属套
 * 100%（余烬砧）、浮霜金属套 100%（霜寒砧）、超限合金套 100%（超限砧）。
 * 不使用该数据包配方本身，因其只支持 FallingBlock 方块转换、不支持物品。
 */
public final class ModEndGatewayHandler {
   private static final float RATE_IRON = 0.03F;
   private static final float RATE_ROYAL = 0.5F;
   private static final float RATE_FULL = 1.0F;
   private static final ResourceKey<Level> THE_END =
      ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("minecraft:the_end"));
   private static final Map<Item, Conversion> CONVERSION_MAP = new HashMap<>();
   private static Item cachedDustItem;

   private ModEndGatewayHandler() {
   }

   private record Conversion(Item target, float rate) {
   }

   private static void initConversionMap() {
      if (!CONVERSION_MAP.isEmpty()) {
         return;
      }
      // 铁套 → 幻灵套
      register(Items.IRON_HELMET, (Item)ModItems.SPECTRAL_HELMET.get(), RATE_IRON);
      register(Items.IRON_CHESTPLATE, (Item)ModItems.SPECTRAL_CHESTPLATE.get(), RATE_IRON);
      register(Items.IRON_LEGGINGS, (Item)ModItems.SPECTRAL_LEGGINGS.get(), RATE_IRON);
      register(Items.IRON_BOOTS, (Item)ModItems.SPECTRAL_BOOTS.get(), RATE_IRON);
      // 皇家钢套 → 幻灵套，50%
      register((Item)ModItems.ROYAL_STEEL_HELMET.get(), (Item)ModItems.SPECTRAL_HELMET.get(), RATE_ROYAL);
      register((Item)ModItems.ROYAL_STEEL_CHESTPLATE.get(), (Item)ModItems.SPECTRAL_CHESTPLATE.get(), RATE_ROYAL);
      register((Item)ModItems.ROYAL_STEEL_LEGGINGS.get(), (Item)ModItems.SPECTRAL_LEGGINGS.get(), RATE_ROYAL);
      register((Item)ModItems.ROYAL_STEEL_BOOTS.get(), (Item)ModItems.SPECTRAL_BOOTS.get(), RATE_ROYAL);
      // 余烬金属套 → 幻灵套，100%
      register((Item)ModItems.EMBER_METAL_HELMET.get(), (Item)ModItems.SPECTRAL_HELMET.get(), RATE_FULL);
      register((Item)ModItems.EMBER_METAL_CHESTPLATE.get(), (Item)ModItems.SPECTRAL_CHESTPLATE.get(), RATE_FULL);
      register((Item)ModItems.EMBER_METAL_LEGGINGS.get(), (Item)ModItems.SPECTRAL_LEGGINGS.get(), RATE_FULL);
      register((Item)ModItems.EMBER_METAL_BOOTS.get(), (Item)ModItems.SPECTRAL_BOOTS.get(), RATE_FULL);
      // 浮霜金属套 → 幻灵套，100%
      register((Item)ModItems.FROST_METAL_HELMET.get(), (Item)ModItems.SPECTRAL_HELMET.get(), RATE_FULL);
      register((Item)ModItems.FROST_METAL_CHESTPLATE.get(), (Item)ModItems.SPECTRAL_CHESTPLATE.get(), RATE_FULL);
      register((Item)ModItems.FROST_METAL_LEGGINGS.get(), (Item)ModItems.SPECTRAL_LEGGINGS.get(), RATE_FULL);
      register((Item)ModItems.FROST_METAL_BOOTS.get(), (Item)ModItems.SPECTRAL_BOOTS.get(), RATE_FULL);
      // 超限合金套 → 幻灵套，100%
      register((Item)ModItems.TRANSCENDIUM_HELMET.get(), (Item)ModItems.SPECTRAL_HELMET.get(), RATE_FULL);
      register((Item)ModItems.TRANSCENDIUM_CHESTPLATE.get(), (Item)ModItems.SPECTRAL_CHESTPLATE.get(), RATE_FULL);
      register((Item)ModItems.TRANSCENDIUM_LEGGINGS.get(), (Item)ModItems.SPECTRAL_LEGGINGS.get(), RATE_FULL);
      register((Item)ModItems.TRANSCENDIUM_BOOTS.get(), (Item)ModItems.SPECTRAL_BOOTS.get(), RATE_FULL);
   }

   private static void register(Item source, Item target, float rate) {
      CONVERSION_MAP.put(source, new Conversion(target, rate));
   }

   /** 末地尘：优先 AnvilCraft 的 end_dust，未安装时退化为原版末地石。 */
   private static Item dustItem() {
      if (cachedDustItem == null) {
         Item item = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.parse("anvilcraft:end_dust"));
         cachedDustItem = item == Items.AIR ? Items.END_STONE : item;
      }
      return cachedDustItem;
   }

   @SubscribeEvent
   public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
      if (!(event.getEntity() instanceof ItemEntity itemEntity)) {
         return;
      }
      if (!(itemEntity.level() instanceof ServerLevel level) || !level.dimension().equals(THE_END)) {
         return;
      }
      initConversionMap();
      ItemStack stack = itemEntity.getItem();
      Conversion conv = CONVERSION_MAP.get(stack.getItem());
      if (conv == null) {
         return;
      }

      int count = stack.getCount();
      int spectralCount = 0;
      for (int i = 0; i < count; i++) {
         if (level.getRandom().nextFloat() < conv.rate()) {
            spectralCount++;
         }
      }
      int dustCount = count - spectralCount;

      // 原掉落物保留幻灵部分（若有），末地尘部分生成新掉落物；
      // 全部未中则把原掉落物直接改为末地尘。
      if (spectralCount > 0) {
         itemEntity.setItem(new ItemStack(conv.target(), spectralCount));
         if (dustCount > 0) {
            ItemEntity dustEntity = new ItemEntity(
               level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
               new ItemStack(dustItem(), dustCount)
            );
            dustEntity.setDeltaMovement(itemEntity.getDeltaMovement());
            level.addFreshEntity(dustEntity);
         }
      } else {
         itemEntity.setItem(new ItemStack(dustItem(), dustCount));
      }
   }
}
