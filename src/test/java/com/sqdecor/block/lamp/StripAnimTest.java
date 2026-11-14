package com.sqdecor.block.lamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StripAnimTest {
    @Test
    void nextCyclesThroughAll() {
        StripAnim a = StripAnim.STATIC;
        for (int i = 0; i < StripAnim.values().length; i++) a = a.next();
        assertEquals(StripAnim.STATIC, a);
        assertEquals(StripAnim.STATIC, StripAnim.RAINBOW.next());
    }

    @Test
    void byIndexClamps() {
        assertEquals(StripAnim.STATIC, StripAnim.byIndex(-3));
        assertEquals(StripAnim.CHASE, StripAnim.byIndex(1));
        assertEquals(StripAnim.RAINBOW, StripAnim.byIndex(99));
    }

    @Test
    void speedSteps() {
        assertEquals(0.5, StripAnim.speed(0));
        assertEquals(1, StripAnim.speed(1));
        assertEquals(2, StripAnim.speed(2));
        assertEquals(4, StripAnim.speed(3));
        assertEquals(1, StripAnim.speed(17));
    }

    @Test
    void levelsStayInRange() {
        for (StripAnim anim : StripAnim.values()) {
            for (int n : new int[]{1, 8, 16}) {
                for (int i = 0; i < n; i++) {
                    for (double t = 0; t < 10; t += 0.37) {
                        float level = anim.level(i, n, t, 12345L);
                        assertTrue(level >= 0f && level <= 1f, anim + " " + level);
                    }
                }
            }
        }
        assertEquals(1f, StripAnim.STATIC.level(3, 8, 5.0, 0L));
        assertEquals(1f, StripAnim.RAINBOW.level(3, 8, 5.0, 0L));
    }

    @Test
    void twinkleIsDeterministic() {
        for (int i = 0; i < 16; i++) {
            assertEquals(StripAnim.TWINKLE.level(i, 16, 2.5, 99L), StripAnim.TWINKLE.level(i, 16, 2.5, 99L));
        }
    }

    @Test
    void hueOnlyForRainbow() {
        assertEquals(-1f, StripAnim.CHASE.hue(0, 8, 1.0));
        for (int i = 0; i < 8; i++) {
            float hue = StripAnim.RAINBOW.hue(i, 8, 3.3);
            assertTrue(hue >= 0f && hue < 1f);
        }
    }

    @Test
    void keys() {
        assertEquals("rainbow", StripAnim.RAINBOW.key());
        assertEquals("static", StripAnim.STATIC.key());
    }
}
