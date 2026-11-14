package com.sqdecor.block.tape;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.sqdecor.block.entity.TapeAnchorBlockEntity;
import com.sqdecor.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TapeAnchorBlock extends DirectionalBlock implements EntityBlock {
    public static final MapCodec<TapeAnchorBlock> CODEC = simpleCodec(TapeAnchorBlock::new);
    private static final VoxelShape[] SHAPES = new VoxelShape[6];

    static {
        for (Direction d : Direction.values()) {
            double lo = 6, hi = 10, depth = 9;
            SHAPES[d.get3DDataValue()] = switch (d) {
                case NORTH -> Block.box(lo, lo, 16 - depth, hi, hi, 16);
                case SOUTH -> Block.box(lo, lo, 0, hi, hi, depth);
                case WEST -> Block.box(16 - depth, lo, lo, 16, hi, hi);
                case EAST -> Block.box(0, lo, lo, depth, hi, hi);
                case UP -> Block.box(lo, 0, lo, hi, depth, hi);
                case DOWN -> Block.box(lo, 16 - depth, lo, hi, 16, hi);
            };
        }
    }

    public TapeAnchorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultState(context.getClickedFace());
    }

    public BlockState defaultState(Direction facing) {
        return defaultBlockState().setValue(FACING, facing);
    }

    public static Vec3 tip(BlockPos pos, BlockState state) {
        Direction d = state.getValue(FACING);
        double o = state.getBlock() instanceof TapeAnchorBlock anchor ? anchor.tipOffset() : 0.12;
        return Vec3.atCenterOf(pos).add(d.getStepX() * o, d.getStepY() * o, d.getStepZ() * o);
    }

    protected double tipOffset() {
        return 0.12;
    }

    public net.minecraft.world.item.Item linkItem() {
        return ModItems.CAUTION_TAPE.get();
    }

    public net.minecraft.world.item.DyeColor defaultColor() {
        return net.minecraft.world.item.DyeColor.YELLOW;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).get3DDataValue()];
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction wall = state.getValue(FACING).getOpposite();
        BlockPos support = pos.relative(wall);
        return level.getBlockState(support).isFaceSturdy(level, support, state.getValue(FACING));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        return state;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof TapeAnchorBlockEntity be) {
            for (BlockPos other : be.links()) {
                if (level.getBlockEntity(other) instanceof TapeAnchorBlockEntity ob && ob.links().contains(pos)) {
                    ob.removeLink(pos);
                    Block.popResource(level, pos, new ItemStack(linkItem()));
                } else if (level.getBlockEntity(other) instanceof com.sqdecor.block.entity.CableRackBlockEntity rack) {
                    int removed = rack.removeLinksTo(pos);
                    for (int i = 0; i < removed; i++) Block.popResource(level, pos, new ItemStack(linkItem()));
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof DyeItem dye) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof TapeAnchorBlockEntity be) {
                be.setColor(dye.getDyeColor());
                for (BlockPos other : be.links()) {
                    if (level.getBlockEntity(other) instanceof TapeAnchorBlockEntity ob) ob.setColor(dye.getDyeColor());
                    else if (level.getBlockEntity(other) instanceof com.sqdecor.block.entity.CableRackBlockEntity rack) rack.colorLinksTo(pos, dye.getDyeColor());
                }
                player.displayClientMessage(Component.translatable(this instanceof com.sqdecor.block.cable.CableClipBlock
                                ? "message.sqdecor.thin_cable_painted" : "message.sqdecor.tape_painted",
                        Component.translatable("color.minecraft." + dye.getDyeColor().getName())), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TapeAnchorBlockEntity(pos, state);
    }
}
