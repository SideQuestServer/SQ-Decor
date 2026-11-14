package com.sqdecor.block.industrial;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

public class StyleFenceBlock extends FenceBlock implements PaintedBlock {
    public StyleFenceBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(Industrial.COLOR, DyeColor.WHITE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
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
    public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target,
                                       net.minecraft.world.level.LevelReader level, BlockPos pos, Player player) {
        return Industrial.pick(state, level, pos);
    }

    @Override
    protected java.util.List<ItemStack> getDrops(BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        return Industrial.withPaint(super.getDrops(state, builder), builder);
    }
}
