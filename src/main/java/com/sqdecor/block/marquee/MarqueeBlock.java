package com.sqdecor.block.marquee;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.sqdecor.block.Attached;
import com.sqdecor.block.entity.MarqueeBlockEntity;
import com.sqdecor.client.MarqueeClient;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MarqueeBlock extends HorizontalDirectionalBlock implements EntityBlock, Attached {
    public static final MapCodec<MarqueeBlock> CODEC = simpleCodec(MarqueeBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final int MAX_ROW = 64;

    private static final VoxelShape[] SHAPES = new VoxelShape[4];

    static {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            double[] b = {0, 3, 15, 16, 13, 16};
            int turns = switch (d) {
                case EAST -> 1;
                case SOUTH -> 2;
                case WEST -> 3;
                default -> 0;
            };
            for (int i = 0; i < turns; i++) b = new double[]{16 - b[5], b[1], b[0], 16 - b[2], b[4], b[3]};
            SHAPES[d.get2DDataValue()] = Block.box(b[0], b[1], b[2], b[3], b[4], b[5]);
        }
    }

    public MarqueeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, true));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        Direction facing = face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MarqueeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public Direction supportDirection(BlockState state) {
        return state.getValue(FACING).getOpposite();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).get2DDataValue()];
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) MarqueeClient.open(pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof DyeItem dye)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MarqueeBlockEntity be) {
            int rgb = dye.getDyeColor().getTextureDiffuseColor() & 0xFFFFFF;
            for (BlockPos p : row(level, pos)) {
                if (level.getBlockEntity(p) instanceof MarqueeBlockEntity panel) {
                    panel.set(be.getText(), rgb, be.getSpeed(), be.isOn());
                }
            }
            player.displayClientMessage(Component.translatable("color.minecraft." + dye.getDyeColor().getName()), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof MarqueeBlockEntity be)) return;
        int signal = level.getBestNeighborSignal(pos);
        boolean wasPowered = be.getLastSignal() > 0;
        be.setLastSignal(signal);
        if (wasPowered == signal > 0) return;
        for (BlockPos p : row(level, pos)) {
            if (level.getBlockEntity(p) instanceof MarqueeBlockEntity panel) {
                panel.set(panel.getText(), panel.getRgb(), panel.getSpeed(), signal > 0);
            }
        }
    }

    public static Direction left(Direction facing) {
        return facing.getClockWise();
    }

    private static boolean samePanel(BlockGetter level, BlockPos pos, Direction facing) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof MarqueeBlock && state.getValue(FACING) == facing;
    }

    public static List<BlockPos> row(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        List<BlockPos> out = new ArrayList<>();
        if (!(state.getBlock() instanceof MarqueeBlock)) return out;
        Direction facing = state.getValue(FACING);
        Direction left = left(facing);
        BlockPos start = pos;
        for (int i = 0; i < MAX_ROW && samePanel(level, start.relative(left), facing); i++) start = start.relative(left);
        BlockPos p = start;
        for (int i = 0; i < MAX_ROW && samePanel(level, p, facing); i++) {
            out.add(p);
            p = p.relative(left.getOpposite());
        }
        return out;
    }
}
