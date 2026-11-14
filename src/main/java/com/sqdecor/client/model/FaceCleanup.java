package com.sqdecor.client.model;

import com.sqdecor.lamp.LampPart;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

final class FaceCleanup {
    private FaceCleanup() {}

    private static final float EPS = 1e-4f;
    private static final float BEHIND = 0.0015f;

    private record Rect(int axis, int sign, float plane, float u0, float u1, float v0, float v1) {
        boolean covers(Rect o) {
            return u0 <= o.u0 + EPS && u1 >= o.u1 - EPS && v0 <= o.v0 + EPS && v1 >= o.v1 - EPS;
        }
    }

    static boolean[] hidden(List<LampFace> faces, List<String> objects) {
        int n = faces.size();
        boolean[] drop = new boolean[n];
        Rect[] rects = new Rect[n];
        Map<TextureAtlasSprite, Boolean> opaque = new IdentityHashMap<>();
        Map<String, float[]> bounds = new HashMap<>();
        for (int i = 0; i < n; i++) {
            LampFace face = faces.get(i);
            float[] box = bounds.computeIfAbsent(objects.get(i), k -> new float[] {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                    -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE});
            for (int k = 0; k < 4; k++) {
                for (int a = 0; a < 3; a++) {
                    box[a] = Math.min(box[a], face.pos()[k * 3 + a]);
                    box[a + 3] = Math.max(box[a + 3], face.pos()[k * 3 + a]);
                }
            }
            if (opaque.computeIfAbsent(face.sprite(), FaceCleanup::opaque)) rects[i] = rect(face);
        }

        Map<Long, List<Integer>> byAxis = new HashMap<>();
        for (int i = 0; i < n; i++) {
            if (rects[i] == null) continue;
            byAxis.computeIfAbsent((long) rects[i].axis(), k -> new ArrayList<>()).add(i);
        }
        for (List<Integer> group : byAxis.values()) {
            for (int q : group) {
                Rect r = rects[q];
                float[] own = bounds.get(objects.get(q));
                for (int c : group) {
                    if (c == q) continue;
                    Rect o = rects[c];
                    if (!o.covers(r)) continue;
                    if (o.sign() != r.sign()) {
                        if (Math.abs(o.plane() - r.plane()) > EPS || objects.get(c).equals(objects.get(q))) continue;
                        if (solidBehind(own, r, -r.sign()) && solidBehind(bounds.get(objects.get(c)), r, r.sign())) {
                            drop[q] = true;
                            break;
                        }
                    } else {
                        float ahead = (o.plane() - r.plane()) * r.sign();
                        if (ahead < -1e-5f || ahead > BEHIND) continue;
                        if (ahead <= 1e-5f) {
                            int rq = rank(faces.get(q).part()), rc = rank(faces.get(c).part());
                            if (rc < rq || (rc == rq && c < q)) continue;
                        }
                        drop[q] = true;
                        break;
                    }
                }
            }
        }
        return drop;
    }

    private static boolean solidBehind(float[] box, Rect r, int side) {
        float min = box[r.axis()], max = box[r.axis() + 3];
        if (side > 0) return min >= r.plane() - EPS && max > r.plane() + 1e-3f;
        return max <= r.plane() + EPS && min < r.plane() - 1e-3f;
    }

    private static int rank(LampPart part) {
        return switch (part) {
            case BULB -> 3;
            case CASE2 -> 2;
            case CASE -> 1;
            default -> 0;
        };
    }

    private static Rect rect(LampFace face) {
        int axis = face.direction().getAxis().ordinal();
        int sign = face.direction().getAxisDirection().getStep();
        float[] p = face.pos();
        float plane = p[axis];
        for (int k = 1; k < 4; k++) if (Math.abs(p[k * 3 + axis] - plane) > EPS) return null;
        int ua = (axis + 1) % 3, va = (axis + 2) % 3;
        float u0 = Float.MAX_VALUE, u1 = -Float.MAX_VALUE, v0 = Float.MAX_VALUE, v1 = -Float.MAX_VALUE;
        for (int k = 0; k < 4; k++) {
            u0 = Math.min(u0, p[k * 3 + ua]);
            u1 = Math.max(u1, p[k * 3 + ua]);
            v0 = Math.min(v0, p[k * 3 + va]);
            v1 = Math.max(v1, p[k * 3 + va]);
        }
        if (u1 - u0 < EPS || v1 - v0 < EPS) return null;
        for (int k = 0; k < 4; k++) {
            float u = p[k * 3 + ua], v = p[k * 3 + va];
            boolean cu = Math.abs(u - u0) < EPS || Math.abs(u - u1) < EPS, cv = Math.abs(v - v0) < EPS || Math.abs(v - v1) < EPS;
            if (!cu || !cv) return null;
        }
        for (int k = 0; k < 4; k++) {
            for (int j = k + 1; j < 4; j++) {
                if (Math.abs(p[k * 3 + ua] - p[j * 3 + ua]) < EPS && Math.abs(p[k * 3 + va] - p[j * 3 + va]) < EPS) return null;
            }
        }
        return new Rect(axis, sign, plane, u0, u1, v0, v1);
    }

    private static boolean opaque(TextureAtlasSprite sprite) {
        try {
            SpriteContents contents = sprite.contents();
            for (int y = 0; y < contents.height(); y++) {
                for (int x = 0; x < contents.width(); x++) {
                    if (contents.isTransparent(0, x, y)) return false;
                }
            }
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
