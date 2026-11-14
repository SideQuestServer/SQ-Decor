package com.sqdecor.client.render;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.sqdecor.block.entity.LampBlockEntity;
import com.sqdecor.block.lamp.LampBlock;
import com.sqdecor.client.ClientConfig;
import com.sqdecor.lamp.LampPart;
import com.sqdecor.led.LedLogic;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class BeaconRenderer {
    private BeaconRenderer() {}

    private static final double CENTER = 5 / 16.0;
    private static final double START = 3 / 16.0;
    public static final double LENGTH = 3.5;
    private static final double SPREAD = Math.toRadians(13);
    private static final double TURNS = 1.5;
    private static final float MAX_ALPHA = 0.55f;
    private static final int SIDES = 12;
    private static final double[] SHELLS = {1.0, 0.68, 0.36};

    private static final double[] RING_AT = {0, 0.6, 1.6, LENGTH};
    private static final float[] RING_ALPHA = {1f, 0.55f, 0.2f, 0f};

    private static final RenderType BEAM = RenderType.create("sqdecor_beacon_beam", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 4096, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(LateBuffers.LIGHT_OVER)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .createCompositeState(false));

    public static void render(LampBlockEntity lamp, BlockState state, Level level, float partialTick, PoseStack pose,
                              MultiBufferSource buffers) {
        int lightLevel = state.getValue(LedLogic.LIGHT_LEVEL);
        if (lightLevel == 0) return;
        float strength = MAX_ALPHA * lightLevel / 15f * (float) (ClientConfig.CONE_STRENGTH.get() * 2);
        if (strength <= 0) return;
        int rgb = lamp.isLightTinted() ? lamp.color(LampPart.BULB) : 0xFFFFFF;
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        Direction facing = state.getValue(LampBlock.FACING);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(new Quaternionf().rotationTo(0, 1, 0, facing.getStepX(), facing.getStepY(), facing.getStepZ()));
        pose.translate(0, CENTER - 0.5, 0);
        Matrix4f matrix = pose.last().pose();
        VertexConsumer buffer = LateBuffers.get(BEAM, buffers);

        double phase = Math.floorMod(lamp.getBlockPos().asLong() * 0x9E3779B97F4A7C15L, 1000) / 1000.0 * Mth.TWO_PI;
        double angle = (level.getGameTime() % 24000 + partialTick) / 20.0 * TURNS * Mth.TWO_PI + phase;
        cone(buffer, matrix, angle, strength, r, g, b);
        cone(buffer, matrix, angle + Math.PI, strength, r, g, b);
        pose.popPose();
    }

    private static void cone(VertexConsumer buffer, Matrix4f matrix, double angle, float strength, int r, int g, int b) {
        float dx = (float) Math.cos(angle);
        float dz = (float) Math.sin(angle);
        float sx = -dz;
        float sz = dx;
        double tan = Math.tan(SPREAD);
        float shellStrength = strength / SHELLS.length * 1.4f;
        for (double shell : SHELLS) {
            for (int ring = 0; ring < RING_AT.length - 1; ring++) {
                for (int side = 0; side < SIDES; side++) {
                    double a0 = Mth.TWO_PI * side / SIDES;
                    double a1 = Mth.TWO_PI * (side + 1) / SIDES;
                    vertex(buffer, matrix, ring, a0, dx, dz, sx, sz, tan, shell, shellStrength, r, g, b);
                    vertex(buffer, matrix, ring + 1, a0, dx, dz, sx, sz, tan, shell, shellStrength, r, g, b);
                    vertex(buffer, matrix, ring + 1, a1, dx, dz, sx, sz, tan, shell, shellStrength, r, g, b);
                    vertex(buffer, matrix, ring, a1, dx, dz, sx, sz, tan, shell, shellStrength, r, g, b);
                }
            }
        }
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, int ring, double around, float dx, float dz,
                               float sx, float sz, double tan, double shell, float strength, int r, int g, int b) {
        double t = START + RING_AT[ring];
        double radius = (0.06 + RING_AT[ring] * tan) * shell;
        float up = (float) (Math.sin(around) * radius);
        float across = (float) (Math.cos(around) * radius);
        float x = (float) (dx * t) + sx * across;
        float z = (float) (dz * t) + sz * across;
        float light = Math.min(1f, RING_ALPHA[ring] * strength);
        buffer.addVertex(matrix, x, up, z)
                .setColor(Math.round(r * light), Math.round(g * light), Math.round(b * light), LateBuffers.cover(light));
    }
}
