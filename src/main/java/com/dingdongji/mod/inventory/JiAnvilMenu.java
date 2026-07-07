package com.dingdongji.mod.inventory;

import net.minecraft.world.inventory.DataSlot;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.common.CommonHooks;

public class JiAnvilMenu extends AnvilMenu {

    // 翻倍概率：10%
    private static final float DOUBLE_CHANCE = 0.10f;

    // 反射获取 AnvilMenu 的私有字段
    private java.lang.reflect.Field costField;
    private java.lang.reflect.Field itemNameField;

    private DataSlot costData() {
        try {
            if (costField == null) {
                costField = AnvilMenu.class.getDeclaredField("cost");
                costField.setAccessible(true);
            }
            return (DataSlot) costField.get(this);
        } catch (Exception e) {
            return null;
        }
    }

    private String itemName() {
        try {
            if (itemNameField == null) {
                itemNameField = AnvilMenu.class.getDeclaredField("itemName");
                itemNameField.setAccessible(true);
            }
            return (String) itemNameField.get(this);
        } catch (Exception e) {
            return null;
        }
    }

    public JiAnvilMenu(int containerId, Inventory playerInventory) {
        super(containerId, playerInventory);
    }

    public JiAnvilMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(containerId, playerInventory, access);
    }

    @Override
    public void createResult() {
        ItemStack inputLeft = this.inputSlots.getItem(0);
        this.costData().set(1);
        int totalCost = 0;
        long repairCost = 0L;
        int repairCostT = 0;
        if (!inputLeft.isEmpty() && EnchantmentHelper.canStoreEnchantments(inputLeft)) {
            ItemStack inputLeftCopy = inputLeft.copy();
            ItemStack inputRight = this.inputSlots.getItem(1);
            final ItemEnchantments.Mutable enchantmentsOnLeft =
                new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(inputLeftCopy));
            repairCost += (long) inputLeft.getOrDefault(DataComponents.REPAIR_COST, 0)
                + (long) inputRight.getOrDefault(DataComponents.REPAIR_COST, 0);
            this.repairItemCountCost = 0;
            boolean hasStoredEnchantmentsOnInput2 = false;
            if (!CommonHooks.onAnvilChange(
                this, inputLeft, inputRight, this.resultSlots, this.itemName(), repairCost, this.player)) {
                return;
            }

            int damage;
            int repairItemCountCost;

            ChatFormatting extraFormat = null;
            if (inputRight.is(Items.NAME_TAG) && !inputLeft.isEmpty()) {
                if (!inputRight.has(DataComponents.CUSTOM_NAME)) {
                    this.resultSlots.setItem(0, ItemStack.EMPTY);
                    this.costData().set(0);
                    return;
                }
                Component formattingText = inputRight.get(DataComponents.CUSTOM_NAME);
                if (formattingText == null) {
                    this.resultSlots.setItem(0, ItemStack.EMPTY);
                    this.costData().set(0);
                    return;
                }
                String format = formattingText.getString();
                if (format.startsWith("&") && format.length() >= 2) {
                    extraFormat = ChatFormatting.getByCode(format.substring(1, 2).charAt(0));
                } else {
                    this.resultSlots.setItem(0, ItemStack.EMPTY);
                    this.costData().set(0);
                    return;
                }
            } else if (!inputRight.isEmpty()) {
                hasStoredEnchantmentsOnInput2 = inputRight.has(DataComponents.STORED_ENCHANTMENTS);
                int damageValue;
                if ((inputLeftCopy.isDamageableItem()
                    && inputLeftCopy.getItem().isValidRepairItem(inputLeft, inputRight))) {
                    damage = Math.min(inputLeftCopy.getDamageValue(), inputLeftCopy.getMaxDamage() / 4);
                    if (damage <= 0) {
                        this.resultSlots.setItem(0, ItemStack.EMPTY);
                        this.costData().set(0);
                        return;
                    }
                    for (repairItemCountCost = 0;
                         damage > 0 && repairItemCountCost < inputRight.getCount();
                         ++repairItemCountCost) {
                        damageValue = inputLeftCopy.getDamageValue() - damage;
                        inputLeftCopy.setDamageValue(damageValue);
                        ++totalCost;
                        damage = Math.min(inputLeftCopy.getDamageValue(), inputLeftCopy.getMaxDamage() / 4);
                    }
                    this.repairItemCountCost = repairItemCountCost;
                } else {
                    if (!hasStoredEnchantmentsOnInput2
                        && (!inputLeftCopy.is(inputRight.getItem())
                        || !inputLeftCopy.isDamageableItem())) {
                        this.resultSlots.setItem(0, ItemStack.EMPTY);
                        this.costData().set(0);
                        return;
                    }

                    if (inputLeftCopy.isDamageableItem() && !hasStoredEnchantmentsOnInput2) {
                        damage = inputLeft.getMaxDamage() - inputLeft.getDamageValue();
                        repairItemCountCost = inputRight.getMaxDamage() - inputRight.getDamageValue();
                        damageValue = repairItemCountCost + inputLeftCopy.getMaxDamage() * 12 / 100;
                        int k1 = damage + damageValue;
                        int l1 = inputLeftCopy.getMaxDamage() - k1;
                        if (l1 < 0) l1 = 0;
                        if (l1 < inputLeftCopy.getDamageValue()) {
                            inputLeftCopy.setDamageValue(l1);
                            totalCost += 2;
                        }
                    }

                    // ===== 叽砧核心：附魔翻倍逻辑 =====
                    ItemEnchantments enchantmentsOnRight = EnchantmentHelper.getEnchantmentsForCrafting(inputRight);
                    RandomSource random = this.player.getRandom();
                    boolean flag2 = false;
                    boolean flag3 = false;

                    for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantmentsOnRight.entrySet()) {
                        Holder<Enchantment> holder = entry.getKey();
                        int leftLevel = enchantmentsOnLeft.getLevel(holder);
                        int rightLevel = entry.getIntValue();
                        Enchantment enchantment = holder.value();

                        // 兼容性检查
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
                            continue;
                        }
                        flag2 = true;

                        // 叽砧：始终允许突破附魔上限
                        int resultLevel;

                        if (leftLevel == rightLevel) {
                            // 同级合并：10%概率翻倍，否则+1
                            if (random.nextFloat() < DOUBLE_CHANCE) {
                                resultLevel = leftLevel + rightLevel;
                            } else {
                                resultLevel = leftLevel + 1;
                            }
                        } else {
                            // 不同级：取较大值
                            resultLevel = Math.max(rightLevel, leftLevel);
                        }

                        enchantmentsOnLeft.set(holder, resultLevel);

                        int anvilCost = enchantment.getAnvilCost();
                        if (hasStoredEnchantmentsOnInput2) {
                            anvilCost = Math.max(1, anvilCost / 2);
                        }

                        // 叽砧费用计算：正常附魔时翻倍所需经验（×2），翻倍时无额外消耗
                        long enchantCost = (long) anvilCost * resultLevel * 2;
                        enchantCost = enchantCost * inputLeft.getCount() * inputLeft.getCount();
                        totalCost += (int) Math.min(enchantCost, Integer.MAX_VALUE);

                        if (inputLeft.getCount() > 1) {
                            totalCost = 99999999;
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
                if (!this.itemName().equals(currentName.getString())
                    && this.itemName() != null
                    && !this.itemName().isBlank()) {
                    currentName = Component.literal(this.itemName());
                }
                inputLeftCopy.set(DataComponents.CUSTOM_NAME, currentName.copy().withStyle(extraFormat));
            } else {
                if (this.itemName() != null && !StringUtil.isBlank(this.itemName())) {
                    boolean nameChanged = !this.itemName().equals(inputLeft.getHoverName().getString());
                    if (nameChanged) {
                        repairCostT = 1;
                        totalCost += repairCostT;
                        inputLeftCopy.set(DataComponents.CUSTOM_NAME, Component.literal(this.itemName()));
                    }
                } else {
                    if (inputLeft.has(DataComponents.CUSTOM_NAME)) {
                        repairCostT = 1;
                        totalCost += repairCostT;
                        inputLeftCopy.remove(DataComponents.CUSTOM_NAME);
                    }
                }
            }

            if (hasStoredEnchantmentsOnInput2 && !inputLeftCopy.isBookEnchantable(inputRight)) {
                inputLeftCopy = ItemStack.EMPTY;
            }

            damage = (int) Mth.clamp(repairCost + (long) totalCost, 0L, 2147483647L);
            this.costData().set(damage);
            if (totalCost <= 0) {
                inputLeftCopy = ItemStack.EMPTY;
            }

            if (repairCostT == totalCost && repairCostT > 0 && this.costData().get() >= 40) {
                this.costData().set(39);
            }

            if (!inputLeftCopy.isEmpty()) {
                repairItemCountCost = inputLeftCopy.getOrDefault(DataComponents.REPAIR_COST, 0);
                if (repairItemCountCost < inputRight.getOrDefault(DataComponents.REPAIR_COST, 0)) {
                    repairItemCountCost = inputRight.getOrDefault(DataComponents.REPAIR_COST, 0);
                }
                if (repairCostT != totalCost || repairCostT == 0) {
                    repairItemCountCost = calculateIncreasedRepairCost(repairItemCountCost);
                }
                inputLeftCopy.set(DataComponents.REPAIR_COST, repairItemCountCost);
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
