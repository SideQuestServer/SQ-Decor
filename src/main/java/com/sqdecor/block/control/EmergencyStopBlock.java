package com.sqdecor.block.control;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class EmergencyStopBlock extends ButtonBlock {
    public static final MapCodec<ButtonBlock> CODEC = simpleCodec(EmergencyStopBlock::new);

    public EmergencyStopBlock(Properties properties) {
        super(BlockSetType.IRON, 30, properties);
    }

    @Override
    public MapCodec<ButtonBlock> codec() {
        return CODEC;
    }
}
