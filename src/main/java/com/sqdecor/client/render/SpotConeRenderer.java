package com.sqdecor.client.render;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.sqdecor.block.entity.LampBlockEntity;
import com.sqdecor.client.ClientConfig;
import com.sqdecor.lamp.LampPart;
import com.sqdecor.led.LedLogic;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class SpotConeRenderer {
    private SpotConeRenderer() {}

    public static final double LENGTH = 12;
    private static final double SPREAD = Math.tan(Math.toRadians(18));
    private static final double[] SHELLS = {1.0, 0.82, 0.64, 0.46, 0.28};
    private static final float SHELL_ALPHA = 0.075f;
    private static final int SIDES = 20;
    private static final double[] RING_AT = {0, 0.5, 1.5, 3, 5, 8, LENGTH};
    private static final float[] RING_ALPHA = {1f, 0.85f, 0.6f, 0.38f, 0.2f, 0.07f, 0f};

    private static final RenderType CONE = RenderType.create("sqdecor_spot_cone", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 8192, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(LateBuffers.LIGHT_OVER)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .createCompositeState(false));

    public static void render(LampBlockEntity lamp, BlockState state, PoseStack pose, MultiBufferSource buffers) {
        int lightLevel = state.getValue(LedLogic.LIGHT_LEVEL);
        if (lightLevel == 0) return;
        GlowRenderer.Info info = GlowRenderer.info(state);
        Direction dir = info.light();
        if (dir == null) return;
        float strength = lightLevel / 15f * (float) (ClientConfig.CONE_STRENGTH.get() * 2) * SHELL_ALPHA;
        if (strength <= 0) return;

        int rgb = lamp.isLightTinted() ? lamp.color(LampPart.BULB) : 0xFFFFFF;
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        Matrix4f matrix = pose.last().pose();
        VertexConsumer buffer = LateBuffers.get(CONE, buffers);
        for (AABB part : info.parts()) cone(buffer, matrix, part, dir, strength, r, g, b);
    }

    public static double reach() {
        return LENGTH;
    }

    private static void cone(VertexConsumer buffer, Matrix4f matrix, AABB part, Direction dir, float strength,
                             int r, int g, int b) {
        int axis = dir.getAxis().ordinal();
        double[] lo = {part.minX, part.minY, part.minZ};
        double[] hi = {part.maxX, part.maxY, part.maxZ};
        double[] origin = new double[3];
        int u = (axis + 1) % 3;
        int v = (axis + 2) % 3;
        for (int a = 0; a < 3; a++) origin[a] = (lo[a] + hi[a]) / 2;
        origin[axis] = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? hi[axis] : lo[axis];
        double radius = Mth.clamp(Math.max(hi[u] - lo[u], hi[v] - lo[v]) / 2, 0.03, 0.25);
        double sign = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1;

        for (double shell : SHELLS) {
            for (int ring = 0; ring < RING_AT.length - 1; ring++) {
                for (int side = 0; side < SIDES; side++) {
                    double a0 = Mth.TWO_PI * side / SIDES;
                    double a1 = Mth.TWO_PI * (side + 1) / SIDES;
                    vertex(buffer, matrix, origin, axis, u, v, sign, radius, shell, ring, a0, strength, r, g, b);
                    vertex(buffer, matrix, origin, axis, u, v, sign, radius, shell, ring + 1, a0, strength, r, g, b);
                    vertex(buffer, matrix, origin, axis, u, v, sign, radius, shell, ring + 1, a1, strength, r, g, b);
                    vertex(buffer, matrix, origin, axis, u, v, sign, radius, shell, ring, a1, strength, r, g, b);
                }
            }
        }
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, double[] origin, int axis, int u, int v,
                               double sign, double radius, double shell, int ring, double around, float strength,
                               int r, int g, int b) {
        double t = RING_AT[ring];
        double rad = (radius + t * SPREAD) * shell;
        double[] p = origin.clone();
        p[axis] += sign * t;
        p[u] += Math.cos(around) * rad;
        p[v] += Math.sin(around) * rad;
        float light = Mth.clamp(RING_ALPHA[ring] * strength, 0f, 1f);
        buffer.addVertex(matrix, (float) p[0], (float) p[1], (float) p[2])
                .setColor(Math.round(r * light), Math.round(g * light), Math.round(b * light), LateBuffers.cover(light));
    }
}
