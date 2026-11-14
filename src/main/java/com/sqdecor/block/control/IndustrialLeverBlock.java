package com.sqdecor.block.control;

import com.mojang.serialization.MapCodec;
import com.sqdecor.block.industrial.Industrial;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

public class IndustrialLeverBlock extends LeverBlock {
    public static final MapCodec<LeverBlock> CODEC = simpleCodec(IndustrialLeverBlock::new);

    public IndustrialLeverBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(Industrial.COLOR, DyeColor.WHITE));
    }

    @Override
    public MapCodec<LeverBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(Industrial.COLOR);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        return Industrial.dye(state, level, pos, stack);
    }
}
