package com.sqdecor.client.render;

import java.util.List;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.sqdecor.block.entity.LampBlockEntity;
import com.sqdecor.block.lamp.AnimatedStripBlock;
import com.sqdecor.block.lamp.LampBlock;
import com.sqdecor.block.lamp.StripAnim;
import com.sqdecor.lamp.LampPart;
import com.sqdecor.led.LedLogic;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class StripAnimRenderer {
    private StripAnimRenderer() {}

    private static final RenderType SEGMENTS = RenderType.create("sqdecor_strip_segments", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 2048, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    private static final float IDLE = 0.10f;

    public static void render(LampBlockEntity lamp, BlockState state, Level level, float partialTick, PoseStack pose,
                              MultiBufferSource buffers) {
        if (!(state.getBlock() instanceof AnimatedStripBlock strip)) return;
        List<double[]> segments = strip.layout().segments();
        int n = segments.size();
        int lightLevel = state.getValue(LedLogic.LIGHT_LEVEL);
        float scale = lightLevel / 15f;
        StripAnim anim = StripAnim.byIndex(lamp.anim());
        int rgb = lamp.isLightTinted() ? lamp.color(LampPart.BULB) : 0xFFFFFF;
        float r = ((rgb >> 16) & 0xFF) / 255f, g = ((rgb >> 8) & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
        double t = (level.getGameTime() % 1_200_000 + partialTick) / 20.0 * StripAnim.speed(lamp.animSpeed());
        long seed = lamp.getBlockPos().asLong();

        Direction facing = state.getValue(LampBlock.FACING);
        int yTurns = switch (facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        int xTurns = switch (facing) {
            case UP -> 0;
            case DOWN -> 2;
            default -> 1;
        };
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-90f * yTurns));
        pose.mulPose(Axis.XP.rotationDegrees(-90f * xTurns));
        pose.mulPose(Axis.YP.rotationDegrees(-90f * state.getValue(LampBlock.TURN)));
        pose.translate(-0.5, -0.5, -0.5);
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = buffers.getBuffer(SEGMENTS);
        for (int i = 0; i < n; i++) {
            float level01 = lightLevel == 0 ? 0f : anim.level(i, n, t, seed);
            float hue = anim.hue(i, n, t);
            float cr = r, cg = g, cb = b;
            if (hue >= 0) {
                int c = Mth.hsvToRgb(hue, 0.9f, 1f);
                cr = ((c >> 16) & 0xFF) / 255f; cg = ((c >> 8) & 0xFF) / 255f; cb = (c & 0xFF) / 255f;
            }
            float k = (IDLE + (1 - IDLE) * level01 * Math.max(scale, 0.35f));
            if (lightLevel == 0) k = IDLE * 0.5f;
            box(vc, m, segments.get(i), cr * k, cg * k, cb * k);
        }
        pose.popPose();
    }

    private static void box(VertexConsumer vc, Matrix4f m, double[] s, float r, float g, float b) {
        float x0 = (float) (0.5 + s[0] / 16), y0 = (float) (s[1] / 16), z0 = (float) (0.5 + s[2] / 16);
        float x1 = (float) (0.5 + s[3] / 16), y1 = (float) (s[4] / 16), z1 = (float) (0.5 + s[5] / 16);
        quad(vc, m, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, r, g, b, 1f);
        quad(vc, m, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, r, g, b, 0.75f);
        quad(vc, m, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, 0.75f);
        quad(vc, m, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, 0.85f);
        quad(vc, m, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, r, g, b, 0.85f);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, float r, float g, float b, float shade) {
        int ir = Math.min(255, Math.round(r * shade * 255)), ig = Math.min(255, Math.round(g * shade * 255)),
                ib = Math.min(255, Math.round(b * shade * 255));
        vc.addVertex(m, ax, ay, az).setColor(ir, ig, ib, 255);
        vc.addVertex(m, bx, by, bz).setColor(ir, ig, ib, 255);
        vc.addVertex(m, cx, cy, cz).setColor(ir, ig, ib, 255);
        vc.addVertex(m, dx, dy, dz).setColor(ir, ig, ib, 255);
    }
}
