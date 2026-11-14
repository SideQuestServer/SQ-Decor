package com.sqdecor.lamp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class ObjFileTest {
    private static final String QUAD = String.join("\n",
            "mtllib x.mtl",
            "o bulb 1",
            "v 0 0 0", "v 1 0 0", "v 1 1 0", "v 0 1 0",
            "vt 0 0", "vt 1 0", "vt 1 1", "vt 0 1",
            "vn 0 0 1",
            "usemtl glass",
            "f 1/1/1 2/2/1 3/3/1 4/4/1",
            "o case2_rahmen",
            "f 1/1 2/2 3/3",
            "g screen_uvs",
            "vt 0.5 0.5");

    @Test
    void readsQuadAndTriangle() throws IOException {
        ObjFile obj = ObjFile.read(new StringReader(QUAD));
        assertEquals(4, obj.positions.size());
        assertEquals(5, obj.uvs.size());
        assertEquals(1, obj.normals.size());
        assertEquals("x.mtl", obj.mtllib);
        assertEquals(2, obj.faces.size());
        ObjFile.Face quad = obj.faces.get(0);
        assertEquals(LampPart.BULB, quad.part());
        assertEquals("glass", quad.material());
        assertArrayEquals(new int[]{0, 1, 2, 3}, quad.v());
        assertArrayEquals(new int[]{0, 0, 0, 0}, quad.vn());
        ObjFile.Face tri = obj.faces.get(1);
        assertEquals(LampPart.CASE2, tri.part());
        assertArrayEquals(new int[]{-1, -1, -1}, tri.vn());
    }

    @Test
    void markerLineDoesNotChangeParts() throws IOException {
        ObjFile obj = ObjFile.read(new StringReader(QUAD));
        assertEquals(EnumSet.of(LampPart.BULB, LampPart.CASE2), obj.parts());
    }

    @Test
    void polygonIsSplit() throws IOException {
        String text = "v 0 0 0\nv 1 0 0\nv 2 1 0\nv 1 2 0\nv 0 1 0\nf 1 2 3 4 5\n";
        ObjFile obj = ObjFile.read(new StringReader(text));
        assertEquals(2, obj.faces.size());
        assertArrayEquals(new int[]{0, 1, 2, 3}, obj.faces.get(0).v());
        assertArrayEquals(new int[]{0, 3, 4}, obj.faces.get(1).v());
    }

    @Test
    void negativeIndicesCountFromEnd() throws IOException {
        ObjFile obj = ObjFile.read(new StringReader("v 0 0 0\nv 1 0 0\nv 0 1 0\nf -3 -2 -1\n"));
        assertArrayEquals(new int[]{0, 1, 2}, obj.faces.get(0).v());
    }

    @Test
    void shippedModelsParseWithValidIndices() throws IOException {
        Path dir = Path.of("src/main/resources/assets/sqdecor/models");
        List<Path> files;
        try (Stream<Path> s = Files.walk(dir)) {
            files = s.filter(p -> p.toString().endsWith(".obj")).toList();
        }
        assertFalse(files.isEmpty());
        List<String> bad = new ArrayList<>();
        for (Path p : files) {
            ObjFile obj;
            try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                obj = ObjFile.read(r);
            }
            if (obj.faces.isEmpty()) bad.add(p + ": keine Flaechen");
            for (ObjFile.Face f : obj.faces) {
                for (int v : f.v()) if (v < 0 || v >= obj.positions.size()) bad.add(p + ": Punkt " + v);
                for (int t : f.vt()) if (t >= obj.uvs.size()) bad.add(p + ": UV " + t);
                for (int n : f.vn()) if (n >= obj.normals.size()) bad.add(p + ": Normale " + n);
            }
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad.subList(0, Math.min(20, bad.size()))));
    }
}
