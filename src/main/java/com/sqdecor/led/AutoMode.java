package com.sqdecor.led;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public enum AutoMode {
    OFF,
    NIGHT,
    NEARBY;

    public static final int DEFAULT_RANGE = 12;
    public static final int MIN_RANGE = 2;
    public static final int MAX_RANGE = 32;

    public static AutoMode byId(int id) {
        AutoMode[] values = values();
        return id >= 0 && id < values.length ? values[id] : OFF;
    }

    public boolean wantsLight(Level level, BlockPos pos, int range) {
        return switch (this) {
            case OFF -> false;
            case NIGHT -> level.isNight();
            case NEARBY -> level.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, range);
        };
    }
}
