package com.dingdongji.mod.event;

import com.dingdongji.mod.init.ModRecipes;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.recipe.ItemPortalConversionRecipe;
import java.util.List;
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
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;

/**
 * 监听物品 ItemEntity 进入末地维度（玩家把套装部位丢进末地门，传送完成后
 * 在末地生成 ItemEntity 时触发）。按几率通过 {@link ItemPortalConversionRecipe}
 * 转换为对应的幻灵装备；未命中部分转为 AnvilCraft 的 end_dust 或原版末地石。
 * <p>
 * 配方驱动：不再硬编码 CONVERSION_MAP，所有转换规则由数据包中
 * {@code dingdongji:item_portal_conversion} 类型 JSON 配方定义，随资源包自动生效。
 */
public final class ModEndGatewayHandler {
   private static final ResourceKey<Level> THE_END =
      ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("minecraft:the_end"));
   /** 跨维度旅行时打的标记：只有从其他维度穿过末地传送门进入末地的物品才允许转换。 */
   private static final String TAG_FROM_END_PORTAL = "dingdongji_from_end_portal";
   private static Item cachedDustItem;

   private ModEndGatewayHandler() {
   }

   /** 末地尘：优先 AnvilCraft 的 end_dust，未安装时退化为原版末地石。 */
   private static Item dustItem() {
      if (cachedDustItem == null) {
         Item item = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.parse("anvilcraft:end_dust"));
         cachedDustItem = item == Items.AIR ? Items.END_STONE : item;
      }
      return cachedDustItem;
   }

   /** 在服务端 ServerLevel（末地）的配方管理器中，为给定输入找第一个匹配的
    *  ItemPortalConversionRecipe。空返回 = 没有适用配方。 */
   private static ItemPortalConversionRecipe findRecipe(ServerLevel level, ItemStack input) {
      RecipeType<ItemPortalConversionRecipe> type =
         (RecipeType<ItemPortalConversionRecipe>)ModRecipes.ITEM_PORTAL_CONVERSION_TYPE.get();
      for (var holder : level.getRecipeManager().getAllRecipesFor(type)) {
         ItemPortalConversionRecipe r = holder.value();
         if (r.matchesItem(input)) return r;
      }
      return null;
   }

   @SubscribeEvent
   public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
      if (!(event.getEntity() instanceof ItemEntity itemEntity)) {
         return;
      }
      if (!(itemEntity.level() instanceof ServerLevel level) || !level.dimension().equals(THE_END)) {
         return;
      }

      var persistentData = itemEntity.getPersistentData();
      if (!persistentData.getBoolean(TAG_FROM_END_PORTAL)) {
         return;
      }
      // 消费标记：防止物品长期滞留末地或存档重载后被重复转换
      persistentData.remove(TAG_FROM_END_PORTAL);

      ItemStack stack = itemEntity.getItem();
      ItemPortalConversionRecipe recipe = findRecipe(level, stack);
      if (recipe == null) {
         return;
      }
      // 只有"末地门" portalId（或配方 portalId = "" 通配）才在此处触发；
      // 未来若要支持 Nether End 或其他门，在配方 JSON 改 portal 字段即可。
      if (!recipe.portalId().toString().equals("minecraft:end_portal")) {
         return;
      }

      int count = stack.getCount();
      int targetCount = 0;
      var result = recipe.result();
      for (int i = 0; i < count; i++) {
         if (result.roll(level.getRandom().nextFloat())) {
            targetCount++;
         }
      }
      int dustCount = count - targetCount;

      if (targetCount > 0) {
         // 以原物品栈组件为基础转换，保留附魔、耐久、命名等，避免玩家财产损失
         ItemStack target = result.item().copy();
         target.applyComponents(stack.getComponents());
         target.setCount(targetCount);
         itemEntity.setItem(target);
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
