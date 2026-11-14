package com.sqdecor.lamp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

public final class ObjFile {
    public record Face(String object, LampPart part, @Nullable String material, int[] v, int[] vt, int[] vn) {}

    public final List<float[]> positions = new ArrayList<>();
    public final List<float[]> uvs = new ArrayList<>();
    public final List<float[]> normals = new ArrayList<>();
    public final List<Face> faces = new ArrayList<>();
    @Nullable
    public String mtllib;

    private ObjFile() {}

    public static ObjFile read(Reader reader) throws IOException {
        ObjFile obj = new ObjFile();
        String object = "";
        String material = null;
        BufferedReader in = new BufferedReader(reader);
        String line;
        while ((line = in.readLine()) != null) {
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] t = line.split("\\s+");
            String rest = line.substring(t[0].length()).strip();
            switch (t[0]) {
                case "v" -> obj.positions.add(floats(t, 3));
                case "vt" -> obj.uvs.add(floats(t, 2));
                case "vn" -> obj.normals.add(floats(t, 3));
                case "o", "g" -> object = rest;
                case "usemtl" -> material = rest;
                case "mtllib" -> obj.mtllib = rest;
                case "f" -> obj.addFace(t, object, material);
                default -> { }
            }
        }
        return obj;
    }

    public Set<LampPart> parts() {
        Set<LampPart> parts = EnumSet.noneOf(LampPart.class);
        for (Face f : faces) parts.add(f.part());
        return parts;
    }

    private void addFace(String[] t, String object, @Nullable String material) {
        int n = t.length - 1;
        if (n < 3) return;
        int[] v = new int[n];
        int[] vt = new int[n];
        int[] vn = new int[n];
        for (int i = 0; i < n; i++) {
            String[] idx = t[i + 1].split("/", -1);
            v[i] = index(idx, 0, positions.size());
            vt[i] = index(idx, 1, uvs.size());
            vn[i] = index(idx, 2, normals.size());
        }
        LampPart part = LampPart.of(object);
        for (int start = 1; start < n - 1; start += 2) {
            int count = Math.min(3, n - start);
            int[] corners = count == 3 ? new int[]{0, start, start + 1, start + 2} : new int[]{0, start, start + 1};
            faces.add(new Face(object, part, material, pick(v, corners), pick(vt, corners), pick(vn, corners)));
        }
    }

    private static int[] pick(int[] values, int[] corners) {
        int[] out = new int[corners.length];
        for (int i = 0; i < corners.length; i++) out[i] = values[corners[i]];
        return out;
    }

    private static int index(String[] idx, int slot, int size) {
        if (slot >= idx.length || idx[slot].isEmpty()) return -1;
        int i = Integer.parseInt(idx[slot]);
        return i < 0 ? size + i : i - 1;
    }

    private static float[] floats(String[] t, int count) {
        float[] out = new float[count];
        for (int i = 0; i < count && i + 1 < t.length; i++) out[i] = Float.parseFloat(t[i + 1]);
        return out;
    }
}
