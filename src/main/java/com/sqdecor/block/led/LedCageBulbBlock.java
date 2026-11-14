package com.sqdecor.block.led;

import java.util.Map;

import com.sqdecor.led.LedLogic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LedCageBulbBlock extends LedBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ROTATED = BooleanProperty.create("rotated");

    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.UP,    Block.box(2.5, 0, 2.5, 13.5, 4, 13.5),
            Direction.DOWN,  Block.box(2.5, 12, 2.5, 13.5, 16, 13.5),
            Direction.NORTH, Block.box(2.5, 2.5, 12, 13.5, 13.5, 16),
            Direction.SOUTH, Block.box(2.5, 2.5, 0, 13.5, 13.5, 4),
            Direction.EAST,  Block.box(0, 2.5, 2.5, 4, 13.5, 13.5),
            Direction.WEST,  Block.box(12, 2.5, 2.5, 16, 13.5, 13.5));

    public LedCageBulbBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(LedLogic.LIGHT_LEVEL, LedLogic.DEFAULT_LEVEL)
                .setValue(FACING, Direction.UP)
                .setValue(ROTATED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, ROTATED);
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!player.isShiftKeyDown()) return super.useWithoutItem(state, level, pos, player, hit);
        if (!level.isClientSide) level.setBlock(pos, state.setValue(ROTATED, !state.getValue(ROTATED)), Block.UPDATE_ALL);
        return InteractionResult.sidedSuccess(level.isClientSide);
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
