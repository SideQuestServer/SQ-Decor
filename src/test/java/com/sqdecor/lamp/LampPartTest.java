package com.sqdecor.lamp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LampPartTest {
    @Test
    void namesFromBlockbench() {
        assertEquals(LampPart.BULB, LampPart.of("bulb"));
        assertEquals(LampPart.BULB, LampPart.of("bulb 2"));
        assertEquals(LampPart.BULB, LampPart.of("Bulb_glas"));
        assertEquals(LampPart.CASE, LampPart.of("case"));
        assertEquals(LampPart.CASE, LampPart.of("case 10"));
        assertEquals(LampPart.CASE, LampPart.of("case 2"));
        assertEquals(LampPart.CASE2, LampPart.of("case2"));
        assertEquals(LampPart.CASE2, LampPart.of("case2 1"));
        assertEquals(LampPart.CASE2, LampPart.of("case 2 1"));
        assertEquals(LampPart.CASE2, LampPart.of("case2_reflektor"));
        assertEquals(LampPart.CASE2, LampPart.of("case-2-3"));
        assertEquals(LampPart.FIXED, LampPart.of("screen_uvs"));
        assertEquals(LampPart.FIXED, LampPart.of(""));
    }

    @Test
    void tintIndex() {
        assertEquals(LampPart.BULB, LampPart.byTintIndex(0));
        assertEquals(LampPart.CASE, LampPart.byTintIndex(1));
        assertEquals(LampPart.CASE2, LampPart.byTintIndex(2));
        assertEquals(LampPart.FIXED, LampPart.byTintIndex(-1));
        assertEquals(LampPart.FIXED, LampPart.byTintIndex(7));
    }
}
