package com.dingdongji.mod.item.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public record CreateTemplateMode(String mode) {
   public static final Codec<CreateTemplateMode> CODEC = Codec.STRING.xmap(CreateTemplateMode::new, CreateTemplateMode::mode);
   public static final StreamCodec<ByteBuf, CreateTemplateMode> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(CreateTemplateMode::new, CreateTemplateMode::mode);
   public static final CreateTemplateMode ALPHA = new CreateTemplateMode("alpha");
   public static final CreateTemplateMode BETA = new CreateTemplateMode("beta");
   public static final CreateTemplateMode GAMMA = new CreateTemplateMode("gamma");
   public static final CreateTemplateMode DELTA = new CreateTemplateMode("delta");
   public static final CreateTemplateMode EPSILON = new CreateTemplateMode("epsilon");
   public static final CreateTemplateMode ZETA = new CreateTemplateMode("zeta");
   public static final CreateTemplateMode DEFAULT = ALPHA;
   /** 服务端校验用：只有这六个模式允许被网络包写入，防止伪造包写入任意字符串。 */
   public static final Set<String> VALID_MODES = Set.of("alpha", "beta", "gamma", "delta", "epsilon", "zeta");

   public static boolean isValid(String mode) {
      return mode != null && VALID_MODES.contains(mode);
   }

   public CreateTemplateMode next() {
      String var1 = this.mode;

      return switch (var1) {
         case "alpha" -> BETA;
         case "beta" -> GAMMA;
         case "gamma" -> DELTA;
         case "delta" -> hasEpsilonZeta() ? EPSILON : ALPHA;
         case "epsilon" -> hasEpsilonZeta() ? ZETA : ALPHA;
         case "zeta" -> ALPHA;
         default -> ALPHA;
      };
   }

   private static boolean hasEpsilonZeta() {
      Item item = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.parse("anvilcraft:permutation_smithing_template"));
      return item != Items.AIR;
   }

   public String getDisplayName() {
      String var1 = this.mode;

      return switch (var1) {
         case "alpha" -> "锻造模板-α";
         case "beta" -> "锻造模板-β";
         case "gamma" -> "锻造模板-γ";
         case "delta" -> "锻造模板-δ";
         case "epsilon" -> "锻造模板-ε";
         case "zeta" -> "锻造模板-ζ";
         default -> "锻造模板-α";
      };
   }

   public String getDescription() {
      String var1 = this.mode;

      return switch (var1) {
         case "alpha" -> "普通模板";
         case "beta" -> "二合一锻造模板";
         case "gamma" -> "四合一锻造模板";
         case "delta" -> "八合一锻造模板";
         case "epsilon" -> "嬗变模板";
         case "zeta" -> "形变模板";
         default -> "可替代所有锻造模板";
      };
   }

   public String getTargetTemplateId() {
      String var1 = this.mode;

      return switch (var1) {
         case "beta" -> "anvilcraft:two_to_one_smithing_template";
         case "gamma" -> "anvilcraft:four_to_one_smithing_template";
         case "delta" -> "anvilcraft:eight_to_one_smithing_template";
         case "epsilon" -> "anvilcraft:permutation_smithing_template";
         case "zeta" -> "anvilcraft:deformation_smithing_template";
         default -> null;
      };
   }
}
