package com.sqdecor.block.led;

import org.jetbrains.annotations.Nullable;

import com.sqdecor.block.Attached;
import com.sqdecor.block.entity.LedBlockEntity;
import com.sqdecor.client.RemoteClient;
import com.sqdecor.item.ChannelItem;
import com.sqdecor.item.LedTunerItem;
import com.sqdecor.led.LedLogic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

public abstract class LedBlock extends Block implements EntityBlock, Attached {
    protected LedBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LedLogic.LIGHT_LEVEL);
    }

    protected BlockState placementState(BlockPlaceContext context) {
        return defaultBlockState().setValue(LedLogic.LIGHT_LEVEL, LedLogic.placementLevel(context.getPlayer()));
    }

    @Nullable
    @Override
    public Direction supportDirection(BlockState state) {
        return null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LedBlockEntity(pos, state);
    }

    public boolean switchable() {
        return true;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || !switchable()) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof LedBlockEntity led) led.serverTick(lvl, pos, st);
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!switchable()) return InteractionResult.PASS;
        if (!level.isClientSide) LedLogic.cycle(level, pos, state, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof DyeItem dye) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof LedBlockEntity be) {
                be.setRgb(dye.getDyeColor().getTextureDiffuseColor());
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!switchable()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (stack.getItem() instanceof LedTunerItem) {
            if (!level.isClientSide) LedTunerItem.apply(stack, level, pos, player, player.isShiftKeyDown());
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof ChannelItem item && (player.isShiftKeyDown() || item.togglesOnUse())) {
            if (player.isShiftKeyDown()) {
                if (!level.isClientSide) ChannelItem.link(stack, level, pos, player);
            } else if (level.isClientSide) {
                RemoteClient.open(hand);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !switchable() || !(level.getBlockEntity(pos) instanceof LedBlockEntity be)) return;
        int placed = state.getValue(LedLogic.LIGHT_LEVEL);
        if (placed > 0) be.setOnLevel(placed);
        if (be.getLastSignal() < 0) {
            be.setLastSignal(0);
            LedLogic.updateRedstone(level, pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide && switchable()) LedLogic.updateRedstone(level, pos);
    }
}
