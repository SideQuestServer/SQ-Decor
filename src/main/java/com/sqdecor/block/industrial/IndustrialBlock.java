package com.sqdecor.block.industrial;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

public class IndustrialBlock extends Block implements PaintedBlock {
    public IndustrialBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(Industrial.COLOR, net.minecraft.world.item.DyeColor.WHITE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(Industrial.COLOR);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        ItemInteractionResult result = Industrial.dye(state, level, pos, stack);
        return result != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION ? result
                : super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    public net.minecraft.world.item.ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target,
                                                                net.minecraft.world.level.LevelReader level, BlockPos pos,
                                                                net.minecraft.world.entity.player.Player player) {
        return Industrial.pick(state, level, pos);
    }

    @Override
    protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        return Industrial.withPaint(super.getDrops(state, builder), builder);
    }
}
