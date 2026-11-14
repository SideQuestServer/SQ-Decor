package com.sqdecor.resources;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class PngMetadataTest {
    private static final byte[] SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'};
    private static final Set<String> KEEP = Set.of("IHDR", "PLTE", "tRNS", "IDAT", "IEND", "acTL", "fcTL", "fdAT",
            "gAMA", "cHRM", "sRGB", "iCCP", "cICP", "sBIT", "pHYs");

    static List<String> problems(byte[] data) {
        List<String> out = new ArrayList<>();
        if (data.length < 8 || !Arrays.equals(Arrays.copyOf(data, 8), SIGNATURE)) {
            out.add("kein PNG");
            return out;
        }
        ByteBuffer buf = ByteBuffer.wrap(data);
        int i = 8;
        while (i + 8 <= data.length) {
            int length = buf.getInt(i);
            String type = new String(data, i + 4, 4, StandardCharsets.ISO_8859_1);
            if (!KEEP.contains(type)) out.add(type);
            i += 12 + length;
            if (type.equals("IEND")) break;
        }
        if (i < data.length) out.add("Daten nach IEND");
        return out;
    }

    @Test
    void pngsHaveNoMetadata() throws IOException {
        List<Path> pngs = Res.all(".png");
        assertFalse(pngs.isEmpty());
        List<String> bad = new ArrayList<>();
        for (Path p : pngs) {
            List<String> found = problems(Files.readAllBytes(p));
            if (!found.isEmpty()) bad.add(p + " " + found);
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }
}
