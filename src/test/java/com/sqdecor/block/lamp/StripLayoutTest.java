package com.sqdecor.block.lamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class StripLayoutTest {
    @Test
    void segmentCounts() {
        Map<StripLayout, Integer> expected = Map.of(StripLayout.STRIP, 8, StripLayout.THIN, 16, StripLayout.DOUBLE, 16,
                StripLayout.CORNER, 13, StripLayout.RING, 12);
        for (StripLayout layout : StripLayout.values()) {
            assertEquals(expected.get(layout), layout.segments().size(), layout.name());
        }
    }

    @Test
    void segmentsAreBoxesInsideTheBlock() {
        for (StripLayout layout : StripLayout.values()) {
            for (double[] b : layout.segments()) {
                assertEquals(6, b.length);
                assertTrue(b[0] < b[3] && b[1] < b[4] && b[2] < b[5], layout.name());
                assertTrue(b[0] >= -8 && b[3] <= 8 && b[2] >= -8 && b[5] <= 8, layout.name());
                assertTrue(b[1] >= 0 && b[4] <= 16, layout.name());
            }
        }
    }

    @Test
    void segmentsAreCachedAndImmutable() {
        List<double[]> first = StripLayout.RING.segments();
        assertSame(first, StripLayout.RING.segments());
        assertThrows(UnsupportedOperationException.class, () -> first.add(new double[6]));
    }

    @Test
    void byName() {
        assertEquals(StripLayout.CORNER, StripLayout.byName("corner"));
        assertEquals(StripLayout.THIN, StripLayout.byName("Thin"));
        assertThrows(IllegalArgumentException.class, () -> StripLayout.byName("nope"));
    }
}
