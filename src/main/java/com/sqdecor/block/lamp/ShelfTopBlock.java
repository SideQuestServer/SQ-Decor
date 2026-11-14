package com.sqdecor.block.lamp;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ShelfTopBlock extends Block {
    public static final IntegerProperty TURN = LampBlock.TURN;
    private static final double[] FOOTPRINT = {0.6, 5, 15.4, 11};

    private final VoxelShape[] shapes = new VoxelShape[4];

    public ShelfTopBlock(Properties properties) {
        super(properties);
        double[] b = FOOTPRINT;
        for (int turn = 0; turn < 4; turn++) {
            shapes[turn] = Block.box(b[0], 0, b[1], b[2], 16, b[3]);
            b = new double[]{16 - b[3], b[0], 16 - b[1], b[2]};
        }
        registerDefaultState(stateDefinition.any().setValue(TURN, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TURN);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes[state.getValue(TURN)];
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos below = pos.below();
        BlockState lower = level.getBlockState(below);
        if (!(lower.getBlock() instanceof ShelfBlock shelf)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return shelf.useItemOn(stack, lower, level, below, player, hand,
                new BlockHitResult(hit.getLocation(), hit.getDirection(), below, hit.isInside()));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        BlockPos below = pos.below();
        BlockState lower = level.getBlockState(below);
        if (!(lower.getBlock() instanceof ShelfBlock shelf)) return InteractionResult.PASS;
        return shelf.useWithoutItem(lower, level, below, player,
                new BlockHitResult(hit.getLocation(), hit.getDirection(), below, hit.isInside()));
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos below = pos.below();
        if (!level.isClientSide && level.getBlockState(below).getBlock() instanceof TallShelfBlock) {
            level.destroyBlock(below, !player.isCreative(), player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())
                && level.getBlockState(pos.below()).getBlock() instanceof TallShelfBlock) {
            level.destroyBlock(pos.below(), true);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
