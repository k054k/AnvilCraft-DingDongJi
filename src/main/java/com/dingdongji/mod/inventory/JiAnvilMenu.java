package com.dingdongji.mod.inventory;

import com.dingdongji.mod.ModMenuTypes;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments.Mutable;
import net.neoforged.neoforge.common.CommonHooks;

public class JiAnvilMenu extends AnvilMenu {
   private static final float DOUBLE_CHANCE = 0.1F;
   private static final Map<String, Boolean> DOUBLE_RESULT_CACHE = new ConcurrentHashMap<>();
   private static final Map<String, Boolean> CURSE_RESULT_CACHE = new ConcurrentHashMap<>();
   private Field costField;
   private Field itemNameField;

   private static String curseCacheKey(UUID uuid, ItemStack left, ItemStack right) {
      return uuid.toString()
         + "|curse|"
         + left.getItem().hashCode()
         + "|"
         + right.getItem().hashCode()
         + "|"
         + left.getComponents().hashCode()
         + "|"
         + right.getComponents().hashCode();
   }

   private static String doubleCacheKey(UUID uuid, ItemStack left, ItemStack right, Holder<Enchantment> holder, int level) {
      String enchId = holder.unwrapKey().map(k -> k.location().toString()).orElse("unknown");
      return uuid.toString()
         + "|"
         + left.getItem().hashCode()
         + "|"
         + right.getItem().hashCode()
         + "|"
         + left.getComponents().hashCode()
         + "|"
         + right.getComponents().hashCode()
         + "|"
         + enchId
         + "|"
         + level;
   }

   private static void clearDoubleCache(UUID uuid) {
      DOUBLE_RESULT_CACHE.entrySet().removeIf(e -> e.getKey().startsWith(uuid.toString() + "|"));
      CURSE_RESULT_CACHE.entrySet().removeIf(e -> e.getKey().startsWith(uuid.toString() + "|"));
   }

   private DataSlot costData() {
      try {
         if (this.costField == null) {
            this.costField = AnvilMenu.class.getDeclaredField("cost");
            this.costField.setAccessible(true);
         }

         return (DataSlot)this.costField.get(this);
      } catch (Exception var2) {
         return null;
      }
   }

   private String itemName() {
      try {
         if (this.itemNameField == null) {
            this.itemNameField = AnvilMenu.class.getDeclaredField("itemName");
            this.itemNameField.setAccessible(true);
         }

         return (String)this.itemNameField.get(this);
      } catch (Exception var2) {
         return null;
      }
   }

   public JiAnvilMenu(int containerId, Inventory playerInventory) {
      super(containerId, playerInventory);
   }

   public JiAnvilMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
      super(containerId, playerInventory, access);
   }

   public MenuType<?> getType() {
      return ModMenuTypes.JI_ANVIL.get();
   }

   public int getCost() {
      return super.getCost();
   }

   public void onTake(Player player, ItemStack stack) {
      super.onTake(player, stack);
      clearDoubleCache(player.getUUID());
   }

   public void createResult() {
      ItemStack inputLeft = this.inputSlots.getItem(0);
      ItemStack inputRight = this.inputSlots.getItem(1);
      this.costData().set(1);
      int totalCost = 0;
      long repairCost = 0L;
      int repairCostT = 0;
      if (!inputLeft.isEmpty()) {
         ItemStack inputLeftCopy = inputLeft.copy();
         Mutable enchantmentsOnLeft = new Mutable(EnchantmentHelper.getEnchantmentsForCrafting(inputLeftCopy));
         repairCost += (long)((Integer)inputLeft.getOrDefault(DataComponents.REPAIR_COST, 0)).intValue()
            + (long)((Integer)inputRight.getOrDefault(DataComponents.REPAIR_COST, 0)).intValue();
         this.repairItemCountCost = 0;
         boolean hasStoredEnchantmentsOnInput2 = false;
         if (!CommonHooks.onAnvilChange(this, inputLeft, inputRight, this.resultSlots, this.itemName(), repairCost, this.player)) {
            return;
         }

         ChatFormatting extraFormat = null;
         if (inputRight.is(Items.NAME_TAG) && !inputLeft.isEmpty()) {
            if (!inputRight.has(DataComponents.CUSTOM_NAME)) {
               this.resultSlots.setItem(0, ItemStack.EMPTY);
               this.costData().set(0);
               return;
            }

            Component formattingText = (Component)inputRight.get(DataComponents.CUSTOM_NAME);
            if (formattingText == null) {
               this.resultSlots.setItem(0, ItemStack.EMPTY);
               this.costData().set(0);
               return;
            }

            String format = formattingText.getString();
            if (!format.startsWith("&") || format.length() < 2) {
               this.resultSlots.setItem(0, ItemStack.EMPTY);
               this.costData().set(0);
               return;
            }

            extraFormat = ChatFormatting.getByCode(format.substring(1, 2).charAt(0));
         } else if (!inputRight.isEmpty()) {
            hasStoredEnchantmentsOnInput2 = inputRight.has(DataComponents.STORED_ENCHANTMENTS);
            if (inputLeftCopy.isDamageableItem() && inputLeftCopy.getItem().isValidRepairItem(inputLeft, inputRight)) {
               int damage = Math.min(inputLeftCopy.getDamageValue(), inputLeftCopy.getMaxDamage() / 4);
               if (damage <= 0) {
                  this.resultSlots.setItem(0, ItemStack.EMPTY);
                  this.costData().set(0);
                  return;
               }

               int repairItemCountCost;
               for (repairItemCountCost = 0; damage > 0 && repairItemCountCost < inputRight.getCount(); repairItemCountCost++) {
                  int damageValue = inputLeftCopy.getDamageValue() - damage;
                  inputLeftCopy.setDamageValue(damageValue);
                  totalCost++;
                  damage = Math.min(inputLeftCopy.getDamageValue(), inputLeftCopy.getMaxDamage() / 4);
               }

               this.repairItemCountCost = repairItemCountCost;
            } else {
               if (!hasStoredEnchantmentsOnInput2 && (!inputLeftCopy.is(inputRight.getItem()) || !inputLeftCopy.isDamageableItem())) {
                  this.resultSlots.setItem(0, ItemStack.EMPTY);
                  this.costData().set(0);
                  return;
               }

               if (inputLeftCopy.isDamageableItem() && !hasStoredEnchantmentsOnInput2) {
                  int damagex = inputLeft.getMaxDamage() - inputLeft.getDamageValue();
                  int repairItemCountCost = inputRight.getMaxDamage() - inputRight.getDamageValue();
                  int damageValue = repairItemCountCost + inputLeftCopy.getMaxDamage() * 12 / 100;
                  int k1 = damagex + damageValue;
                  int l1 = inputLeftCopy.getMaxDamage() - k1;
                  if (l1 < 0) {
                     l1 = 0;
                  }

                  if (l1 < inputLeftCopy.getDamageValue()) {
                     inputLeftCopy.setDamageValue(l1);
                     totalCost += 2;
                  }
               }

               ItemEnchantments enchantmentsOnRight = hasStoredEnchantmentsOnInput2
                  ? (ItemEnchantments)inputRight.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY)
                  : EnchantmentHelper.getEnchantmentsForCrafting(inputRight);
               boolean flag2 = false;
               boolean flag3 = false;

               for (Entry<Holder<Enchantment>> entry : enchantmentsOnRight.entrySet()) {
                  Holder<Enchantment> holder = (Holder<Enchantment>)entry.getKey();
                  int leftLevel = enchantmentsOnLeft.getLevel(holder);
                  int rightLevel = entry.getIntValue();
                  Enchantment enchantment = (Enchantment)holder.value();
                  boolean flag1 = inputLeftCopy.supportsEnchantment(holder);
                  if (this.player.getAbilities().instabuild) {
                     flag1 = true;
                  }

                  for (Holder<Enchantment> holder1 : enchantmentsOnLeft.keySet()) {
                     if (!holder1.equals(holder) && !Enchantment.areCompatible(holder, holder1)) {
                        flag1 = false;
                        totalCost++;
                     }
                  }

                  if (!flag1) {
                     flag3 = true;
                  } else {
                     flag2 = true;
                     int resultLevel;
                     if (leftLevel == rightLevel) {
                        String cacheKey = doubleCacheKey(this.player.getUUID(), inputLeft, inputRight, holder, leftLevel);
                        Boolean cached = DOUBLE_RESULT_CACHE.get(cacheKey);
                        boolean doDouble;
                        if (cached != null) {
                           doDouble = cached;
                        } else {
                           doDouble = this.player.getRandom().nextFloat() < 0.1F;
                           DOUBLE_RESULT_CACHE.put(cacheKey, doDouble);
                        }

                        if (doDouble) {
                           resultLevel = leftLevel + rightLevel;
                        } else {
                           resultLevel = leftLevel + 1;
                        }
                     } else {
                        resultLevel = Math.max(rightLevel, leftLevel);
                     }

                     enchantmentsOnLeft.set(holder, resultLevel);

                     int anvilCost = enchantment.getAnvilCost();
                     if (hasStoredEnchantmentsOnInput2) {
                        anvilCost = Math.max(1, anvilCost / 2);
                     }

                     long enchantCost = (long)anvilCost * (long)rightLevel * 2L;
                     enchantCost = enchantCost * (long)inputLeft.getCount() * (long)inputLeft.getCount();
                     totalCost += (int)Math.min(enchantCost, 2147483647L);
                     if (inputLeft.getCount() > 1) {
                        totalCost = 99999999;
                     }
                  }
               }

               // 本次合成只要实际转移了至少一个附魔，就只掷一次 20%，最多附加一个诅咒
               if (flag2) {
                  String curseKey = curseCacheKey(this.player.getUUID(), inputLeft, inputRight);
                  Boolean cachedCurse = CURSE_RESULT_CACHE.get(curseKey);
                  if (cachedCurse == null) {
                     cachedCurse = this.player.getRandom().nextFloat() < 0.2F;
                     CURSE_RESULT_CACHE.put(curseKey, cachedCurse);
                  }

                  if (cachedCurse) {
                     List<Holder<Enchantment>> curses = new ArrayList<>();
                     RegistryLookup<Enchantment> enchRegistry = this.player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

                     for (Holder<Enchantment> possible : enchRegistry.listElements().toList()) {
                        if (possible.is(EnchantmentTags.CURSE) && enchantmentsOnLeft.getLevel(possible) <= 0 && inputLeftCopy.supportsEnchantment(possible)) {
                           curses.add(possible);
                        }
                     }

                     if (!curses.isEmpty()) {
                        Holder<Enchantment> curse = curses.get(this.player.getRandom().nextInt(curses.size()));
                        enchantmentsOnLeft.set(curse, 1);
                     }
                  }
               }

               if (flag3 && !flag2) {
                  this.resultSlots.setItem(0, ItemStack.EMPTY);
                  this.costData().set(0);
                  return;
               }
            }
         }

         if (extraFormat != null) {
            repairCostT = 1;
            totalCost += repairCostT * inputLeft.getCount() * inputRight.getCount();
            Component currentName = inputLeft.getHoverName();
            if (!this.itemName().equals(currentName.getString()) && this.itemName() != null && !this.itemName().isBlank()) {
               currentName = Component.literal(this.itemName());
            }

            inputLeftCopy.set(DataComponents.CUSTOM_NAME, currentName.copy().withStyle(extraFormat));
         } else if (this.itemName() != null && !StringUtil.isBlank(this.itemName())) {
            boolean nameChanged = !this.itemName().equals(inputLeft.getHoverName().getString());
            if (nameChanged) {
               repairCostT = 1;
               totalCost += repairCostT;
               inputLeftCopy.set(DataComponents.CUSTOM_NAME, Component.literal(this.itemName()));
            }
         } else if (inputLeft.has(DataComponents.CUSTOM_NAME)) {
            repairCostT = 1;
            totalCost += repairCostT;
            inputLeftCopy.remove(DataComponents.CUSTOM_NAME);
         }

         if (hasStoredEnchantmentsOnInput2 && !inputLeftCopy.isBookEnchantable(inputRight)) {
            inputLeftCopy = ItemStack.EMPTY;
         }

         int damagexx = (int)Mth.clamp(repairCost + (long)totalCost, 0L, 2147483647L);
         this.costData().set(damagexx);
         if (totalCost <= 0) {
            inputLeftCopy = ItemStack.EMPTY;
         }

         if (!inputLeftCopy.isEmpty()) {
            int repairItemCountCostx = (Integer)inputLeftCopy.getOrDefault(DataComponents.REPAIR_COST, 0);
            if (repairItemCountCostx < (Integer)inputRight.getOrDefault(DataComponents.REPAIR_COST, 0)) {
               repairItemCountCostx = (Integer)inputRight.getOrDefault(DataComponents.REPAIR_COST, 0);
            }

            if (repairCostT != totalCost || repairCostT == 0) {
               repairItemCountCostx = calculateIncreasedRepairCost(repairItemCountCostx);
            }

            inputLeftCopy.set(DataComponents.REPAIR_COST, repairItemCountCostx);
            EnchantmentHelper.setEnchantments(inputLeftCopy, enchantmentsOnLeft.toImmutable());
         }

         this.resultSlots.setItem(0, inputLeftCopy);
         this.broadcastChanges();
      } else {
         this.resultSlots.setItem(0, ItemStack.EMPTY);
         this.costData().set(0);
      }
   }
}
