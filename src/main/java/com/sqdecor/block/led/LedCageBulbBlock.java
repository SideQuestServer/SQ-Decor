package com.sqdecor.block.led;

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

    private static final double[] BASE = {5, 0, 2.75, 11, 3.5, 13.25};

    private static final VoxelShape[][] SHAPES = new VoxelShape[2][6];

    static {
        for (Direction d : Direction.values()) {
            SHAPES[0][d.get3DDataValue()] = shape(d, false);
            SHAPES[1][d.get3DDataValue()] = shape(d, true);
        }
    }

    private static double[] rotY(double[] b) {
        return new double[]{16 - b[5], b[1], b[0], 16 - b[2], b[4], b[3]};
    }

    private static double[] rotX(double[] b) {
        return new double[]{b[0], b[2], 16 - b[4], b[3], b[5], 16 - b[1]};
    }

    private static VoxelShape shape(Direction facing, boolean rotated) {
        double[] b = BASE;
        if (rotated) b = rotY(b);
        int xTurns = switch (facing) {
            case UP -> 0;
            case DOWN -> 2;
            default -> 1;
        };
        int yTurns = switch (facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        for (int i = 0; i < xTurns; i++) b = rotX(b);
        for (int i = 0; i < yTurns; i++) b = rotY(b);
        return Block.box(b[0], b[1], b[2], b[3], b[4], b[5]);
    }

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
        return SHAPES[state.getValue(ROTATED) ? 1 : 0][state.getValue(FACING).get3DDataValue()];
    }
}
