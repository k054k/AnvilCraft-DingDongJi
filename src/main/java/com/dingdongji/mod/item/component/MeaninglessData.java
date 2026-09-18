package com.dingdongji.mod.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public record MeaninglessData(int totalLevels, ItemEnchantments convertedEnchantments) {
   public static final MeaninglessData EMPTY = new MeaninglessData(0, ItemEnchantments.EMPTY);
   public static final MapCodec<MeaninglessData> CODEC = RecordCodecBuilder.mapCodec(
      ins -> ins.group(
               Codec.INT.fieldOf("total_levels").forGetter(MeaninglessData::totalLevels),
               ItemEnchantments.CODEC.fieldOf("converted_enchantments").forGetter(MeaninglessData::convertedEnchantments)
            )
            .apply(ins, MeaninglessData::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, MeaninglessData> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.INT, MeaninglessData::totalLevels, ItemEnchantments.STREAM_CODEC, MeaninglessData::convertedEnchantments, MeaninglessData::new
   );
}
