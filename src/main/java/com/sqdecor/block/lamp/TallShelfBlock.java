package com.sqdecor.block.lamp;

import org.jetbrains.annotations.Nullable;

import com.sqdecor.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class TallShelfBlock extends ShelfBlock {
    public TallShelfBlock(Properties properties, String model, double[] base, boolean lit) {
        super(properties, model, base, lit, 5);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos above = context.getClickedPos().above();
        if (above.getY() >= level.getMaxBuildHeight() || !level.getBlockState(above).canBeReplaced(context)) return null;
        return placementState(context).setValue(FACING, Direction.UP)
                .setValue(TURN, turnFor(context.getHorizontalDirection()));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        level.setBlock(pos.above(), ModBlocks.SHELF_TOP.get().defaultBlockState().setValue(TURN, state.getValue(TURN)),
                Block.UPDATE_ALL);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockState(pos.above()).is(ModBlocks.SHELF_TOP.get())) {
            level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
