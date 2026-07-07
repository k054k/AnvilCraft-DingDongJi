package com.dingdongji.mod.block;

import com.dingdongji.mod.KryptonMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class ModBlockTags {
    public static final TagKey<Block> CANT_BROKEN_ANVIL =
            TagKey.create(Registries.BLOCK, KryptonMod.modLoc("cant_broken_anvil"));
}
