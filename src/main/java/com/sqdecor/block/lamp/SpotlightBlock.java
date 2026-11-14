package com.sqdecor.block.lamp;

import org.jetbrains.annotations.Nullable;

import com.sqdecor.CommonConfig;
import com.sqdecor.block.entity.LampBlockEntity;
import com.sqdecor.led.LedLogic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class SpotlightBlock extends LampBlock {
    public static final int RANGE = 16;
    private static final int CHECK_TICKS = 10;

    public SpotlightBlock(Properties properties, String model, double[] base) {
        super(properties, model, base);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        BlockEntityTicker<T> led = super.getTicker(level, state, type);
        if (level.isClientSide || led == null) return led;
        return (lvl, pos, st, be) -> {
            led.tick(lvl, pos, st, be);
            if (be instanceof LampBlockEntity lamp && Math.floorMod(lvl.getGameTime() + pos.asLong(), CHECK_TICKS) == 0) {
                updateSpot(lvl, pos, st, lamp);
            }
        };
    }

    @Nullable
    public static BlockPos target(Level level, BlockPos pos, Direction dir) {
        BlockPos.MutableBlockPos p = pos.mutable();
        BlockPos last = null;
        int range = Math.min(RANGE, CommonConfig.SPOTLIGHT_RANGE.get());
        for (int i = 1; i <= range; i++) {
            p.move(dir);
            if (!level.isLoaded(p)) return null;
            BlockState state = level.getBlockState(p);
            if (state.isAir() || state.is(Blocks.LIGHT)) {
                last = p.immutable();
            } else if (state.isSolidRender(level, p) || !state.getCollisionShape(level, p).isEmpty()) {
                return last;
            }
        }
        return null;
    }

    private static void updateSpot(Level level, BlockPos pos, BlockState state, LampBlockEntity lamp) {
        int light = state.getValue(LedLogic.LIGHT_LEVEL);
        BlockPos target = light == 0 ? null : target(level, pos, state.getValue(FACING));
        if (lamp.spot != null && !lamp.spot.equals(target)) {
            removeLight(level, lamp.spot);
            lamp.spot = null;
            lamp.setChanged();
        }
        if (target == null) return;
        BlockState there = level.getBlockState(target);
        boolean ours = target.equals(lamp.spot) && there.is(Blocks.LIGHT);
        if (ours && there.getValue(LightBlock.LEVEL) == light) return;
        if (!there.isAir() && !ours) return;
        level.setBlock(target, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, light), Block.UPDATE_ALL);
        lamp.spot = target;
        lamp.setChanged();
    }

    private static void removeLight(Level level, BlockPos spot) {
        if (level.isLoaded(spot) && level.getBlockState(spot).is(Blocks.LIGHT)) {
            level.setBlock(spot, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof LampBlockEntity lamp && lamp.spot != null) {
            removeLight(level, lamp.spot);
            lamp.spot = null;
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
