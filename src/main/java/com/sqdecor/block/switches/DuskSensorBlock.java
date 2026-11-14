package com.sqdecor.block.switches;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.sqdecor.block.entity.LightSwitchBlockEntity;
import com.sqdecor.led.DeviceSetting;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class DuskSensorBlock extends LightSwitchBlock {
    public static final MapCodec<DuskSensorBlock> CODEC = simpleCodec(DuskSensorBlock::new);
    private static final int CHECK_TICKS = 20;

    public DuskSensorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public List<DeviceSetting> settings() {
        return List.of(DeviceSetting.THRESHOLD);
    }

    @Override
    protected boolean ticks() {
        return true;
    }

    public static int daylight(ServerLevel level, BlockPos pos) {
        if (!level.dimensionType().hasSkyLight()) return 0;
        return Math.max(0, level.getBrightness(LightLayer.SKY, pos) - level.getSkyDarken());
    }

    @Override
    protected void serverTick(ServerLevel level, BlockPos pos, BlockState state, LightSwitchBlockEntity be) {
        if (Math.floorMod(level.getGameTime() + pos.asLong(), CHECK_TICKS) != 0) return;
        int wanted = daylight(level, pos) < be.get(DeviceSetting.THRESHOLD) ? 1 : 0;
        if (wanted == be.lastWanted) return;
        be.lastWanted = wanted;
        switchChannel(level, pos, be, wanted == 1);
    }
}
