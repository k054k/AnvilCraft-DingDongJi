package com.dingdongji.mod.block;

import com.dingdongji.mod.inventory.JiAnvilMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public class JiAnvilBlock extends AnvilBlock {
    private static final Style GOLD_STYLE = Style.EMPTY.withColor(0xF7BE00);

    // 注意：FACING 由父类 AnvilBlock（铁砧工艺通过 Mixin 注入）提供，子类不可重复添加

    // 北/南方向：顶部沿 Z 轴全长（0~16）
    private static final VoxelShape SHAPE_NS = Shapes.or(
        Block.box(2, 0, 2, 14, 4, 14),
        Block.box(5, 4, 4, 11, 10, 12),
        Block.box(3, 10, 0, 13, 16, 16)
    );

    // 东/西方向：顶部沿 X 轴全长（0~16），中部与底座旋转 90°
    private static final VoxelShape SHAPE_EW = Shapes.or(
        Block.box(2, 0, 2, 14, 4, 14),
        Block.box(4, 4, 5, 12, 10, 11),
        Block.box(0, 10, 3, 16, 16, 13)
    );

    public JiAnvilBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new JiAnvilMenu(id, inventory, ContainerLevelAccess.create(level, pos)),
                Component.translatable("container.repair")
        ));
        player.awardStat(Stats.INTERACT_WITH_ANVIL);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("正常附魔时翻倍所需经验，但有10%概率翻倍附魔").setStyle(GOLD_STYLE));
    }
}
