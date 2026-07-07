package com.dingdongji.mod.block;

import com.dingdongji.mod.KryptonMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(KryptonMod.MODID);

    public static final DeferredBlock<JiAnvilBlock> JI_ANVIL =
            BLOCKS.register("ji_anvil",
                    () -> new JiAnvilBlock(BlockBehaviour.Properties.of()
                            .strength(5.0f, 1200.0f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.ANVIL)
                            .noOcclusion()
                    )
            );

    /** 叽块 */
    public static final DeferredBlock<KejiBlock> KEJI_BLOCK =
            BLOCKS.register("keji_block",
                    () -> new KejiBlock(BlockBehaviour.Properties.of()
                            .strength(10.0f, 3.0f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                    )
            );

    // BlockItem helper
    public static Supplier<BlockItem> createBlockItem(DeferredBlock<? extends Block> block) {
        return () -> new BlockItem(block.get(), new Item.Properties());
    }
}
