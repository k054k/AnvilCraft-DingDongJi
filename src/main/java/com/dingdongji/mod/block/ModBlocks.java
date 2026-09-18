package com.dingdongji.mod.block;

import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Blocks;

public class ModBlocks {
   public static final Blocks BLOCKS = DeferredRegister.createBlocks("dingdongji");
   private static final SoundType JI_ANVIL_SOUND_TYPE = SoundType.ANVIL;
   public static final DeferredBlock<JiAnvilBlock> JI_ANVIL = BLOCKS.register(
      "ji_anvil", () -> new JiAnvilBlock(Properties.of().strength(5.0F, 1200.0F).requiresCorrectToolForDrops().sound(JI_ANVIL_SOUND_TYPE).noOcclusion())
   );
   public static final DeferredBlock<KejiBlock> KEJI_BLOCK = BLOCKS.register(
      "keji_block", () -> new KejiBlock(Properties.of().strength(10.0F, 3.0F).requiresCorrectToolForDrops().sound(SoundType.METAL))
   );

   public static Supplier<BlockItem> createBlockItem(DeferredBlock<? extends Block> block) {
      return () -> new BlockItem((Block)block.get(), new net.minecraft.world.item.Item.Properties());
   }
}
