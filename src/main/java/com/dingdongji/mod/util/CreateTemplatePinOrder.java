package com.dingdongji.mod.util;

import com.dingdongji.mod.ModClientConfig;
import com.dingdongji.mod.item.ModItems;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Client-side presentation ordering for the smithing-table template
 * catalogs. The server always keeps the natural order; when the pin option is
 * enabled the client moves every create-template variant to the head of its
 * local list. Ordering never affects gameplay: variant clicks send the mode
 * via SelectTemplateModePacket, while borrow/favorite actions only use the
 * item id (matchesTemplate compares registry ids, never list positions).
 * <p>
 * Two menu shapes are supported:
 * <ul>
 *   <li>{@code dev.dubhe.anvilcraft.inventory.AdjacentSmithingMenu}
 *       (Royal/Ember/Frost tables) — catalog field {@code adjacentTemplates};</li>
 *   <li>{@code dev.dubhe.anvilcraft.inventory.TranscendenceSmithingMenu}
 *       — catalog field {@code templates}.</li>
 * </ul>
 * <p>
 * Each sync packet carries the server's natural list; that immutable list is
 * snapshotted before local reordering so toggling the option back off can be
 * restored instantly without a round trip.
 */
public final class CreateTemplatePinOrder {
   private static final String ADJACENT_MENU_CLASS =
      "dev.dubhe.anvilcraft.inventory.AdjacentSmithingMenu";
   private static final String TRANSCENDENCE_MENU_CLASS =
      "dev.dubhe.anvilcraft.inventory.TranscendenceSmithingMenu";
   private static final Map<Object, List<ItemStack>> NATURAL_SNAPSHOTS = new WeakHashMap<>();

   private static Class<?> adjacentMenuClass;
   private static Class<?> transcendenceMenuClass;
   private static Field adjacentTemplatesField;
   private static Field transcendenceTemplatesField;
   private static boolean lastPin;
   private static boolean initialized;

   private CreateTemplatePinOrder() {
   }

   public static boolean isMenu(Object menu) {
      if (menu == null) {
         return false;
      }
      Class<?> adjacent = adjacentClass();
      if (adjacent != null && adjacent.isInstance(menu)) {
         return true;
      }
      Class<?> transcendence = transcendenceClass();
      return transcendence != null && transcendence.isInstance(menu);
   }

   /**
    * Invoked from the handleTemplateSync TAIL mixin of either menu, after the
    * natural-order list has been stored in the menu's catalog field.
    */
   public static void onSynced(Object menu) {
      if (!FMLEnvironment.dist.isClient() || menu == null) {
         return;
      }

      List<ItemStack> natural = readField(menu);
      if (natural == null) {
         return;
      }

      synchronized (NATURAL_SNAPSHOTS) {
         NATURAL_SNAPSHOTS.put(menu, natural);
      }

      if (ModClientConfig.createTemplatePinEnabled()) {
         applyPinned(menu, natural);
      }
   }

   /** Invoked every client tick; reorders immediately when the option flips. */
   public static void tick(Object openMenu) {
      if (!FMLEnvironment.dist.isClient()) {
         return;
      }

      boolean pin = ModClientConfig.createTemplatePinEnabled();
      if (!initialized) {
         initialized = true;
         lastPin = pin;
         return;
      }

      if (pin == lastPin) {
         return;
      }

      lastPin = pin;
      if (openMenu == null || !isMenu(openMenu)) {
         return;
      }

      if (pin) {
         applyPinned(openMenu, readField(openMenu));
      } else {
         restoreNatural(openMenu);
      }
   }

   private static void applyPinned(Object menu, List<ItemStack> current) {
      if (current == null || current.isEmpty()) {
         return;
      }

      List<ItemStack> pinned = new ArrayList<>();
      List<ItemStack> rest = new ArrayList<>();

      for (ItemStack stack : current) {
         (ModItems.isCreateTemplate(stack) ? pinned : rest).add(stack);
      }

      if (pinned.isEmpty()) {
         return;
      }

      List<ItemStack> reordered = new ArrayList<>(pinned.size() + rest.size());
      reordered.addAll(pinned);
      reordered.addAll(rest);
      if (!reordered.equals(current)) {
         writeField(menu, reordered);
      }
   }

   private static void restoreNatural(Object menu) {
      List<ItemStack> natural;
      synchronized (NATURAL_SNAPSHOTS) {
         natural = NATURAL_SNAPSHOTS.get(menu);
      }

      if (natural != null) {
         writeField(menu, natural);
      }
      // Without a snapshot the list is left as-is; the next server sync
      // refreshes it and onSynced applies the current option.
   }

   private static List<ItemStack> readField(Object menu) {
      try {
         Field field = templatesFieldFor(menu);
         return field == null ? null : (List<ItemStack>)field.get(menu);
      } catch (Throwable t) {
         return null;
      }
   }

   private static void writeField(Object menu, List<ItemStack> list) {
      try {
         Field field = templatesFieldFor(menu);
         if (field != null) {
            field.set(menu, list);
         }
      } catch (Throwable t) {
      }
   }

   /** Returns the catalog field for whichever supported menu the object is. */
   private static Field templatesFieldFor(Object menu) {
      try {
         Class<?> adjacent = adjacentClass();
         if (adjacent != null && adjacent.isInstance(menu)) {
            if (adjacentTemplatesField == null) {
               adjacentTemplatesField = adjacent.getDeclaredField("adjacentTemplates");
               adjacentTemplatesField.setAccessible(true);
            }
            return adjacentTemplatesField;
         }
         Class<?> transcendence = transcendenceClass();
         if (transcendence != null && transcendence.isInstance(menu)) {
            if (transcendenceTemplatesField == null) {
               transcendenceTemplatesField = transcendence.getDeclaredField("templates");
               transcendenceTemplatesField.setAccessible(true);
            }
            return transcendenceTemplatesField;
         }
      } catch (Throwable t) {
         return null;
      }
      return null;
   }

   private static Class<?> adjacentClass() {
      if (adjacentMenuClass == null) {
         adjacentMenuClass = tryLoad(ADJACENT_MENU_CLASS);
      }
      return adjacentMenuClass;
   }

   private static Class<?> transcendenceClass() {
      if (transcendenceMenuClass == null) {
         transcendenceMenuClass = tryLoad(TRANSCENDENCE_MENU_CLASS);
      }
      return transcendenceMenuClass;
   }

   private static Class<?> tryLoad(String name) {
      try {
         return Class.forName(name);
      } catch (Throwable t) {
         return null;
      }
   }
}
