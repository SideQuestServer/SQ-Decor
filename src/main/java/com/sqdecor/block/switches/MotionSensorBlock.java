package com.sqdecor.block.switches;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.sqdecor.block.entity.LightSwitchBlockEntity;
import com.sqdecor.led.DeviceSetting;
import com.sqdecor.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class MotionSensorBlock extends LightSwitchBlock {
    public static final MapCodec<MotionSensorBlock> CODEC = simpleCodec(MotionSensorBlock::new);
    private static final int CHECK_TICKS = 10;

    public MotionSensorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public List<DeviceSetting> settings() {
        return List.of(DeviceSetting.RANGE, DeviceSetting.DELAY);
    }

    @Override
    protected boolean ticks() {
        return true;
    }

    @Override
    protected void serverTick(ServerLevel level, BlockPos pos, BlockState state, LightSwitchBlockEntity be) {
        long now = level.getGameTime();
        if (Math.floorMod(now + pos.asLong(), CHECK_TICKS) != 0) return;
        boolean seen = level.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                be.get(DeviceSetting.RANGE));
        if (seen) {
            be.lastSeen = now;
            if (!be.active) {
                be.active = true;
                ModSounds.play(level, pos, ModSounds.MOTION_SENSOR, 0.5f);
                switchChannel(level, pos, be, true);
            }
        } else if (be.active && now - be.lastSeen >= be.get(DeviceSetting.DELAY) * 20L) {
            be.active = false;
            switchChannel(level, pos, be, false);
        }
    }
}
