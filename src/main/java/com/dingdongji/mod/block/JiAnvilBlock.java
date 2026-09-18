package com.dingdongji.mod.block;

import com.dingdongji.mod.inventory.JiAnvilMenu;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class JiAnvilBlock extends AnvilBlock {
   private static final Style GOLD_STYLE = Style.EMPTY.withColor(16236032);
   private static final VoxelShape SHAPE_NS = Shapes.or(
      Block.box(2.0, 0.0, 2.0, 14.0, 4.0, 14.0), new VoxelShape[]{Block.box(5.0, 4.0, 4.0, 11.0, 10.0, 12.0), Block.box(3.0, 10.0, 0.0, 13.0, 16.0, 16.0)}
   );
   private static final VoxelShape SHAPE_EW = Shapes.or(
      Block.box(2.0, 0.0, 2.0, 14.0, 4.0, 14.0), new VoxelShape[]{Block.box(4.0, 4.0, 5.0, 12.0, 10.0, 11.0), Block.box(0.0, 10.0, 3.0, 16.0, 16.0, 13.0)}
   );

   public JiAnvilBlock(Properties properties) {
      super(properties);
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      Direction facing = (Direction)state.getValue(BlockStateProperties.HORIZONTAL_FACING);
      return facing != Direction.NORTH && facing != Direction.SOUTH ? SHAPE_EW : SHAPE_NS;
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      Direction facing = (Direction)state.getValue(BlockStateProperties.HORIZONTAL_FACING);
      return facing != Direction.NORTH && facing != Direction.SOUTH ? SHAPE_EW : SHAPE_NS;
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (level.isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         player.openMenu(
            new SimpleMenuProvider(
               (id, inventory, p) -> new JiAnvilMenu(id, inventory, ContainerLevelAccess.create(level, pos)), Component.translatable("container.repair")
            )
         );
         player.awardStat(Stats.INTERACT_WITH_ANVIL);
         return InteractionResult.CONSUME;
      }
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("tooltip.dingdongji.ji_anvil.desc").setStyle(GOLD_STYLE));
   }

   public void onLand(Level level, BlockPos pos, BlockState state, BlockState replacedState, FallingBlockEntity entity) {
   }
}
