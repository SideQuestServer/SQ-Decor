package com.sqdecor.led;

public enum RedstoneMode {
    IGNORE,
    ANALOG,
    SWITCH;

    public static RedstoneMode byId(int id) {
        RedstoneMode[] values = values();
        return id >= 0 && id < values.length ? values[id] : IGNORE;
    }
}
