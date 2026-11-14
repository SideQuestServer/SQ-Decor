package com.sqdecor.block.switches;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.sqdecor.block.entity.LightSwitchBlockEntity;
import com.sqdecor.led.DeviceSetting;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class TimerSwitchBlock extends LightSwitchBlock {
    public static final MapCodec<TimerSwitchBlock> CODEC = simpleCodec(TimerSwitchBlock::new);
    private static final int CHECK_TICKS = 20;

    public TimerSwitchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public List<DeviceSetting> settings() {
        return List.of(DeviceSetting.ON_HOUR, DeviceSetting.OFF_HOUR);
    }

    @Override
    protected boolean ticks() {
        return true;
    }

    public static int hour(ServerLevel level) {
        return (int) (Math.floorMod(level.getDayTime() + 6000, 24000L) / 1000);
    }

    @Override
    protected void serverTick(ServerLevel level, BlockPos pos, BlockState state, LightSwitchBlockEntity be) {
        if (Math.floorMod(level.getGameTime() + pos.asLong(), CHECK_TICKS) != 0) return;
        int on = be.get(DeviceSetting.ON_HOUR);
        int off = be.get(DeviceSetting.OFF_HOUR);
        if (on == off) return;
        int h = hour(level);
        boolean inside = on < off ? h >= on && h < off : h >= on || h < off;
        int wanted = inside ? 1 : 0;
        if (wanted == be.lastWanted) return;
        be.lastWanted = wanted;
        switchChannel(level, pos, be, inside);
    }
}
