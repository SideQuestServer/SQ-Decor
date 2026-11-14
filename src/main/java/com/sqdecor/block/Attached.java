package com.sqdecor.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public interface Attached {
    @Nullable
    Direction supportDirection(BlockState state);
}
