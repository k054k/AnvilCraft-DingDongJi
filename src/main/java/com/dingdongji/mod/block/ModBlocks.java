package com.dingdongji.mod.block;

import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Blocks;

public class ModBlocks {
   public static final Blocks BLOCKS = DeferredRegister.createBlocks("dingdongji");
   private static final SoundType JI_ANVIL_SOUND_TYPE = new SoundType(
      0.3F,
      1.0F,
      SoundEvents.CHICKEN_HURT,
      SoundEvents.CHICKEN_STEP,
      SoundEvents.CHICKEN_AMBIENT,
      SoundEvents.ANVIL_HIT,
      SoundEvents.ANVIL_FALL
   );
   public static final DeferredBlock<JiAnvilBlock> JI_ANVIL = BLOCKS.register(
      "ji_anvil", () -> new JiAnvilBlock(Properties.of().strength(5.0F, 1200.0F).requiresCorrectToolForDrops().sound(JI_ANVIL_SOUND_TYPE).noOcclusion())
   );
   public static final DeferredBlock<KejiBlock> KEJI_BLOCK = BLOCKS.register(
      "keji_block", () -> new KejiBlock(Properties.of().strength(10.0F, 3.0F).requiresCorrectToolForDrops().sound(SoundType.METAL))
   );
   // 三种重质金块：属性完全对齐本体重质铁块——下界合金块属性副本
   // （防爆1200/下界合金音效/需正确工具掉落），noOcclusion，硬度5
   public static final DeferredBlock<Block> HEAVY_GOLD_BLOCK = BLOCKS.register(
      "heavy_gold_block", () -> new Block(
         Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.NETHERITE_BLOCK)
            .strength(5.0F, 1200.0F).noOcclusion())
   );
   public static final DeferredBlock<Block> CURSED_HEAVY_GOLD_BLOCK = BLOCKS.register(
      "cursed_heavy_gold_block", () -> new CursedHeavyGoldBlock(
         Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.NETHERITE_BLOCK)
            .strength(5.0F, 1200.0F).noOcclusion())
   );
   public static final DeferredBlock<Block> ENCHANTED_HEAVY_GOLD_BLOCK = BLOCKS.register(
      "enchanted_heavy_gold_block", () -> new EnchantedHeavyGoldBlock(
         Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.NETHERITE_BLOCK)
            .strength(5.0F, 1200.0F).noOcclusion())
   );
   // 属性对齐本体浮霜金属块：复制铁块(5.0F/METAL) + 爆炸抗性1200 + 光带发光(亮度9、不遮挡、自发光渲染)
   // 对应本体超温余烬金属块：带冷却时间（默认400tick），倒计时结束变回浮霜金属块
   public static final DeferredBlock<Block> SUPERCOOLED_FROST_METAL_BLOCK = BLOCKS.register(
      "supercooled_frost_metal_block",
      () -> new SupercooledFrostMetalBlock(
         Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.IRON_BLOCK)
            .explosionResistance(1200.0F)
            .lightLevel(s -> 9)
            .noOcclusion()
            .emissiveRendering((state, level, pos) -> true)
      )
   );
   public static final DeferredBlock<AnchoredBeaconBlock> ANCHORED_BEACON = BLOCKS.register(
      "anchored_beacon",
      () -> new AnchoredBeaconBlock(
         Properties.of().strength(3.0F).lightLevel(s -> 15).noOcclusion().isRedstoneConductor((state, level, pos) -> false)
      )
   );

   public static Supplier<BlockItem> createBlockItem(DeferredBlock<? extends Block> block) {
      return () -> new BlockItem((Block)block.get(), new net.minecraft.world.item.Item.Properties());
   }
}
