package com.sqdecor.block.lamp;

import com.sqdecor.led.LedLogic;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;

public class PropBlock extends LampBlock {
    public PropBlock(Properties properties, String model, double[] base) {
        super(properties, model, base);
        registerDefaultState(defaultBlockState().setValue(LedLogic.LIGHT_LEVEL, 0));
    }

    @Override
    public boolean hasLight() {
        return false;
    }

    @Override
    public boolean switchable() {
        return false;
    }

    @Override
    protected BlockState placementState(BlockPlaceContext context) {
        return super.placementState(context).setValue(LedLogic.LIGHT_LEVEL, 0);
    }
}
