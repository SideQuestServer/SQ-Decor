package com.sqdecor.client.render;

import java.util.Arrays;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sqdecor.client.PerfProbe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class WireMesh implements VertexConsumer {
    private static final int N = 8;
    private static final int REBUILD_TICKS = 20;

    private float[] data = new float[N * 64];
    private int[] color = new int[64];
    private int[] light = new int[64];
    private int count;
    private int base = -1;
    private float[] boxes = new float[6 * 4];
    private int[] ends = new int[4];
    private int parts;
    private int partStart;

    final long builtAt;
    final int signature;

    WireMesh(long builtAt, int signature) {
        this.builtAt = builtAt;
        this.signature = signature;
    }

    boolean stale(long now, int signature, BlockPos pos) {
        return this.signature != signature || now < builtAt || now - builtAt >= REBUILD_TICKS + (pos.hashCode() & 15);
    }

    void endPart() {
        if (count == partStart) return;
        if (parts == ends.length) {
            ends = Arrays.copyOf(ends, parts * 2);
            boxes = Arrays.copyOf(boxes, parts * 12);
        }
        float x0 = Float.MAX_VALUE, y0 = x0, z0 = x0, x1 = -Float.MAX_VALUE, y1 = x1, z1 = x1;
        for (int i = partStart; i < count; i++) {
            int o = i * N;
            x0 = Math.min(x0, data[o]);
            y0 = Math.min(y0, data[o + 1]);
            z0 = Math.min(z0, data[o + 2]);
            x1 = Math.max(x1, data[o]);
            y1 = Math.max(y1, data[o + 1]);
            z1 = Math.max(z1, data[o + 2]);
        }
        int b = parts * 6;
        boxes[b] = x0;
        boxes[b + 1] = y0;
        boxes[b + 2] = z0;
        boxes[b + 3] = x1;
        boxes[b + 4] = y1;
        boxes[b + 5] = z1;
        ends[parts++] = count;
        partStart = count;
    }

    boolean isEmpty() {
        return count == 0;
    }

    void draw(VertexConsumer vc, PoseStack.Pose pose, BlockPos origin) {
        Frustum frustum;
        try {
            frustum = Minecraft.getInstance().levelRenderer.getFrustum();
        } catch (RuntimeException e) {
            frustum = null;
        }
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        float cx = (float) (camera.x - origin.getX()), cy = (float) (camera.y - origin.getY()), cz = (float) (camera.z - origin.getZ());
        int start = 0;
        for (int p = 0; p < parts; p++) {
            int end = ends[p];
            int b = p * 6;
            if (frustum != null && !frustum.isVisible(new AABB(origin.getX() + boxes[b], origin.getY() + boxes[b + 1],
                    origin.getZ() + boxes[b + 2], origin.getX() + boxes[b + 3], origin.getY() + boxes[b + 4],
                    origin.getZ() + boxes[b + 5]).inflate(0.1))) {
                if (PerfProbe.on) PerfProbe.wiresOutside++;
                start = end;
                continue;
            }
            for (int q = start; q + 4 <= end; q += 4) {
                int o = q * N;
                float mx = (data[o] + data[o + N] + data[o + 2 * N] + data[o + 3 * N]) * 0.25f;
                float my = (data[o + 1] + data[o + N + 1] + data[o + 2 * N + 1] + data[o + 3 * N + 1]) * 0.25f;
                float mz = (data[o + 2] + data[o + N + 2] + data[o + 2 * N + 2] + data[o + 3 * N + 2]) * 0.25f;
                if ((cx - mx) * data[o + 5] + (cy - my) * data[o + 6] + (cz - mz) * data[o + 7] < -0.01f) continue;
                for (int k = q; k < q + 4; k++) {
                    int v = k * N;
                    int c = color[k];
                    vc.addVertex(pose, data[v], data[v + 1], data[v + 2])
                            .setColor(c >> 16 & 255, c >> 8 & 255, c & 255, c >>> 24)
                            .setUv(data[v + 3], data[v + 4])
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(light[k])
                            .setNormal(pose, data[v + 5], data[v + 6], data[v + 7]);
                }
            }
            start = end;
        }
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        if (count == color.length) {
            data = Arrays.copyOf(data, data.length * 2);
            color = Arrays.copyOf(color, color.length * 2);
            light = Arrays.copyOf(light, light.length * 2);
        }
        base = count++;
        int o = base * N;
        data[o] = x;
        data[o + 1] = y;
        data[o + 2] = z;
        color[base] = 0xFFFFFFFF;
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        color[base] = (alpha & 255) << 24 | (red & 255) << 16 | (green & 255) << 8 | blue & 255;
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        data[base * N + 3] = u;
        data[base * N + 4] = v;
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        light[base] = (u & 0xFFFF) | (v & 0xFFFF) << 16;
        return this;
    }

    @Override
    public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
        data[base * N + 5] = normalX;
        data[base * N + 6] = normalY;
        data[base * N + 7] = normalZ;
        return this;
    }
}
