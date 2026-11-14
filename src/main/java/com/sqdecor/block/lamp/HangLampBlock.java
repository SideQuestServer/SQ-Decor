package com.sqdecor.block.lamp;

import com.sqdecor.led.LedLogic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class HangLampBlock extends LampBlock {
    public static final int MAX_CHAIN = 32;

    public HangLampBlock(Properties properties, String model, double[] base) {
        super(properties, model, base);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return placementState(context).setValue(FACING, Direction.UP).setValue(TURN, turnFor(context.getHorizontalDirection()));
    }

    @Override
    public Direction supportDirection(BlockState state) {
        return Direction.UP;
    }

    public static int chainLength(BlockGetter level, BlockPos pos) {
        BlockPos.MutableBlockPos p = pos.mutable();
        for (int i = 1; i <= MAX_CHAIN; i++) {
            p.move(Direction.UP);
            if (!level.getBlockState(p).isAir()) return i - 1;
        }
        return 0;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.CHAIN)) return super.useItemOn(stack, state, level, pos, player, hand, hit);
        if (!level.isClientSide) {
            BlockPos to = player.isShiftKeyDown() ? pos.above() : pos.below();
            if (move(level, pos, to, state)) {
                level.playSound(null, to, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.8f, 1.0f);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean move(Level level, BlockPos from, BlockPos to, BlockState state) {
        if (!level.getBlockState(to).isAir() || level.isOutsideBuildHeight(to)) return false;
        BlockEntity old = level.getBlockEntity(from);
        CompoundTag data = old == null ? null : old.saveWithoutMetadata(level.registryAccess());
        level.removeBlock(from, false);
        level.setBlock(to, state, Block.UPDATE_ALL);
        BlockEntity moved = level.getBlockEntity(to);
        if (data != null && moved != null) {
            moved.loadWithComponents(data, level.registryAccess());
            moved.setChanged();
            level.sendBlockUpdated(to, state, state, Block.UPDATE_ALL);
        }
        LedLogic.updateRedstone(level, to);
        return true;
    }
}
