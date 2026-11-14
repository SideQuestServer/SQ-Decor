package com.sqdecor.led;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RedstoneModeTest {
    @Test
    void byIdFallsBackToIgnore() {
        assertEquals(RedstoneMode.IGNORE, RedstoneMode.byId(0));
        assertEquals(RedstoneMode.ANALOG, RedstoneMode.byId(1));
        assertEquals(RedstoneMode.SWITCH, RedstoneMode.byId(2));
        assertEquals(RedstoneMode.IGNORE, RedstoneMode.byId(-1));
        assertEquals(RedstoneMode.IGNORE, RedstoneMode.byId(3));
    }
}
