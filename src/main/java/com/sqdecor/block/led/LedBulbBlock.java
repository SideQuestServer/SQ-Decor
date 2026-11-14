package com.sqdecor.block.led;

import java.util.Map;

import com.sqdecor.led.LedLogic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LedBulbBlock extends LedBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.UP,    Block.box(5, 0, 5, 11, 1, 11),
            Direction.DOWN,  Block.box(5, 15, 5, 11, 16, 11),
            Direction.NORTH, Block.box(5, 5, 15, 11, 11, 16),
            Direction.SOUTH, Block.box(5, 5, 0, 11, 11, 1),
            Direction.EAST,  Block.box(0, 5, 5, 1, 11, 11),
            Direction.WEST,  Block.box(15, 5, 5, 16, 11, 11));

    public LedBulbBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(LedLogic.LIGHT_LEVEL, LedLogic.DEFAULT_LEVEL)
                .setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return placementState(context).setValue(FACING, context.getClickedFace());
    }

    @Override
    public Direction supportDirection(BlockState state) {
        return state.getValue(FACING).getOpposite();
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }
}
