package com.sqdecor.block.cable;

import com.mojang.serialization.MapCodec;
import com.sqdecor.block.tape.TapeAnchorBlock;
import com.sqdecor.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CableClipBlock extends TapeAnchorBlock {
    public static final MapCodec<CableClipBlock> CODEC = simpleCodec(CableClipBlock::new);
    private static final VoxelShape[] SHAPES = new VoxelShape[6];

    static {
        for (Direction d : Direction.values()) {
            double lo = 6, hi = 10, depth = 2;
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

    public CableClipBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected double tipOffset() {
        return -7 / 16.0;
    }

    @Override
    public Item linkItem() {
        return ModItems.THIN_CABLE.get();
    }

    @Override
    public DyeColor defaultColor() {
        return DyeColor.BLACK;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).get3DDataValue()];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos support = pos.relative(state.getValue(FACING).getOpposite());
        return !level.getBlockState(support).getCollisionShape(level, support).isEmpty();
    }
}
