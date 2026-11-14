package com.sqdecor.block.industrial;

import com.sqdecor.block.entity.PaintBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public interface PaintedBlock extends EntityBlock {
    @Override
    default BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PaintBlockEntity(pos, state);
    }
}
