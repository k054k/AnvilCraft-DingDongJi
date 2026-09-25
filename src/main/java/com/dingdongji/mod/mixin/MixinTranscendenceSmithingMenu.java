package com.dingdongji.mod.mixin;

import com.dingdongji.mod.item.ModComponents;
import com.dingdongji.mod.item.ModItems;
import com.dingdongji.mod.item.component.CreateTemplateMode;
import com.dingdongji.mod.util.CreateTemplatePinOrder;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
   targets = {"dev.dubhe.anvilcraft.inventory.TranscendenceSmithingMenu"},
   remap = false
)
public abstract class MixinTranscendenceSmithingMenu {
   @Unique
   private static final ResourceLocation CREATE_TEMPLATE_ID = ResourceLocation.parse("dingdongji:create_template");

   @Inject(
      method = {"refreshTemplateCatalog"},
      at = {@At("TAIL")}
   )
   private void dingdongji$expandCreateTemplate(CallbackInfo ci) {
      try {
         Class<?> clazz = Class.forName("dev.dubhe.anvilcraft.inventory.TranscendenceSmithingMenu");
         Field field = clazz.getDeclaredField("templates");
         field.setAccessible(true);
         List<ItemStack> templates = (List<ItemStack>)field.get(this);
         if (templates == null || templates.isEmpty()) {
            return;
         }

         int createIdx = -1;

         for (int i = 0; i < templates.size(); i++) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(templates.get(i).getItem());
            if (CREATE_TEMPLATE_ID.equals(id)) {
               createIdx = i;
               break;
            }
         }

         if (createIdx < 0) {
            return;
         }

         ItemStack base = templates.get(createIdx).copyWithCount(1);
         List<ItemStack> expansion = new ArrayList<>();
         expansion.add(base.copy());
         ItemStack beta = base.copy();
         beta.set((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get(), CreateTemplateMode.BETA);
         expansion.add(beta);
         ItemStack gamma = base.copy();
         gamma.set((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get(), CreateTemplateMode.GAMMA);
         expansion.add(gamma);
         ItemStack delta = base.copy();
         delta.set((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get(), CreateTemplateMode.DELTA);
         expansion.add(delta);
         ItemStack eps = base.copy();
         eps.set((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get(), CreateTemplateMode.EPSILON);
         expansion.add(eps);
         ItemStack zeta = base.copy();
         zeta.set((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get(), CreateTemplateMode.ZETA);
         expansion.add(zeta);

         // 服务端目录保持自然序：把基项原位替换为 6 个变体，绝不置顶。
         // 是否置顶是纯客户端表现（CreateTemplatePinOrder 读本地配置）。
         // 原方法产出的列表来自 Stream.toList()（不可变），故重建后整体写回。
         List<ItemStack> natural = new ArrayList<>(templates.size() + expansion.size() - 1);

         for (int ix = 0; ix < templates.size(); ix++) {
            if (ix != createIdx) {
               natural.add(templates.get(ix));
            }
         }

         natural.addAll(createIdx, expansion);
         field.set(this, natural);
         Field dirtyField = clazz.getDeclaredField("templateDataDirty");
         dirtyField.setAccessible(true);
         dirtyField.setBoolean(this, true);
      } catch (Exception var15) {
      }
   }

   @Inject(
      method = {"handleTemplateSync"},
      at = {@At("TAIL")},
      remap = false
   )
   private void dingdongji$pinOrderAfterSync(List<ItemStack> templates, List<?> favorites, ItemStack selected, CallbackInfo ci) {
      CreateTemplatePinOrder.onSynced(this);
   }

   @Inject(
      method = {"getMode"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void dingdongji$createTemplateMode(CallbackInfoReturnable<Object> cir) {
      try {
         Field f = Class.forName("dev.dubhe.anvilcraft.inventory.TranscendenceSmithingMenu").getDeclaredField("selectedTemplate");
         f.setAccessible(true);
         ItemStack sel = (ItemStack)f.get(this);
         if (sel == null || !ModItems.isCreateTemplate(sel)) {
            return;
         }

         CreateTemplateMode mode = (CreateTemplateMode)sel.getOrDefault((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get(), CreateTemplateMode.DEFAULT);
         String modeCls = mode.mode();

         String modeName = switch (modeCls) {
            case "beta", "gamma", "delta" -> "EMBER";
            case "epsilon", "zeta" -> "FROST";
            default -> "ROYAL";
         };
         Class<?> modeClass = Class.forName("dev.dubhe.anvilcraft.inventory.TranscendenceSmithingMenu$Mode");
         @SuppressWarnings({"unchecked", "rawtypes"})
         Object modeEnum = Enum.valueOf((Class)modeClass, modeName);
         cir.setReturnValue(modeEnum);
      } catch (Exception var8) {
      }
   }

   @Inject(
      method = {"getEmberInputSize"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void ddj$getEmberInputSize(CallbackInfoReturnable<Integer> cir) {
      try {
         Field f = Class.forName("dev.dubhe.anvilcraft.inventory.TranscendenceSmithingMenu").getDeclaredField("selectedTemplate");
         f.setAccessible(true);
         ItemStack sel = (ItemStack)f.get(this);
         if (ModItems.isCreateTemplate(sel)) {
            CreateTemplateMode m = (CreateTemplateMode)sel.getOrDefault((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get(), CreateTemplateMode.DEFAULT);
            String var5 = m.mode();

            cir.setReturnValue(switch (var5) {
               case "delta" -> 8;
               case "gamma" -> 4;
               case "beta" -> 2;
               default -> 0;
            });
         }
      } catch (Exception var7) {
      }
   }

   @Inject(
      method = {"createFrostResult"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void ddj$createFrostResult(CallbackInfo ci) {
      try {
         Class<?> clazz = Class.forName("dev.dubhe.anvilcraft.inventory.TranscendenceSmithingMenu");
         Field sf = clazz.getDeclaredField("selectedTemplate");
         sf.setAccessible(true);
         ItemStack sel = (ItemStack)sf.get(this);
         if (sel == null || !ModItems.isCreateTemplate(sel)) {
            return;
         }

         CreateTemplateMode mode = (CreateTemplateMode)sel.getOrDefault((DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get(), CreateTemplateMode.DEFAULT);
         boolean isPermutation = "epsilon".equals(mode.mode());
         boolean isDeformation = "zeta".equals(mode.mode());
         if (!isPermutation && !isDeformation) {
            return;
         }

         Field lf = clazz.getDeclaredField("level");
         lf.setAccessible(true);
         Level level = (Level)lf.get(this);
         Field rf = clazz.getDeclaredField("royalFrostInputs");
         rf.setAccessible(true);
         Container inputs = (Container)rf.get(this);
         Class<?> inputCls = Class.forName("dev.dubhe.anvilcraft.recipe.frost.FrostSmithingRecipeInput");
         Object input = inputCls.getConstructor(ItemStack.class, ItemStack.class, ItemStack.class).newInstance(sel, inputs.getItem(0), inputs.getItem(1));
         Method setFrost = clazz.getDeclaredMethod("setFrostResult", RecipeHolder.class, inputCls);
         setFrost.setAccessible(true);
         Class<?> modRecipeTypes = Class.forName("dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes");
         Object holder = isPermutation ? modRecipeTypes.getField("PERMUTATION_TYPE").get(null) : modRecipeTypes.getField("DEFORMATION_TYPE").get(null);
         Object type = holder.getClass().getMethod("get").invoke(holder);
         RecipeManager rm = level.getRecipeManager();
         Method getRecipesFor = RecipeManager.class.getMethod("getRecipesFor", RecipeType.class, RecipeInput.class, Level.class);
         List matches = (List)getRecipesFor.invoke(rm, type, input, level);
         if (!matches.isEmpty()) {
            setFrost.invoke(this, matches.get(0), input);
         }

         ci.cancel();
      } catch (Exception var21) {
      }
   }

   @Inject(
      method = {"isSelectedTemplate"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void ddj$isSelectedTemplate(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
      try {
         Field f = Class.forName("dev.dubhe.anvilcraft.inventory.TranscendenceSmithingMenu").getDeclaredField("selectedTemplate");
         f.setAccessible(true);
         ItemStack sel = (ItemStack)f.get(this);
         if (ModItems.isCreateTemplate(stack) && ModItems.isCreateTemplate(sel)) {
            CreateTemplateMode m1 = (CreateTemplateMode)stack.getOrDefault(
               (DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get(), CreateTemplateMode.DEFAULT
            );
            CreateTemplateMode m2 = (CreateTemplateMode)sel.getOrDefault(
               (DataComponentType)ModComponents.CREATE_TEMPLATE_MODE.get(), CreateTemplateMode.DEFAULT
            );
            cir.setReturnValue(m1.mode().equals(m2.mode()));
         }
      } catch (Exception var7) {
      }
   }
}
