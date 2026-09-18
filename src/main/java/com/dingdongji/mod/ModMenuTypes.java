package com.dingdongji.mod;

import com.dingdongji.mod.inventory.JiAnvilMenu;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
   public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, "dingdongji");
   public static final Supplier<MenuType<?>> JI_ANVIL = MENUS.register(
      "ji_anvil", () -> new MenuType(JiAnvilMenu::new, FeatureFlagSet.of(FeatureFlags.VANILLA))
   );
}
