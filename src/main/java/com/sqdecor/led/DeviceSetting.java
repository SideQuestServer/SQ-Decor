package com.sqdecor.led;

import net.minecraft.network.chat.Component;

public enum DeviceSetting {
    RANGE("range", 2, 16, 6),
    DELAY("delay", 1, 120, 10),
    THRESHOLD("threshold", 1, 15, 8),
    ON_HOUR("on_hour", 0, 23, 18),
    OFF_HOUR("off_hour", 0, 23, 7);

    public final String key;
    public final int min;
    public final int max;
    public final int defaultValue;

    DeviceSetting(String key, int min, int max, int defaultValue) {
        this.key = key;
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
    }

    public int clamp(int value) {
        return Math.max(min, Math.min(max, value));
    }

    public Component label() {
        return Component.translatable("gui.sqdecor.device." + key);
    }
}
