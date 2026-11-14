package com.sqdecor.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sqdecor.SQDecor;
import com.sqdecor.block.entity.MarqueeBlockEntity;
import com.sqdecor.block.marquee.MarqueeBlock;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MarqueeRenderer implements BlockEntityRenderer<MarqueeBlockEntity> {
    private static final ResourceLocation DOT = SQDecor.id("textures/misc/marquee_dot.png");
    public static final int COLUMNS = 16;
    private static final int ROW_CHECK_TICKS = 10;
    private static final float PX = 1 / 16f;
    private static final float Z = 15 / 16f - 0.004f;

    public MarqueeRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(MarqueeBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null || !be.isOn() || LateBuffers.shadowPass()) return;
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof MarqueeBlock)) return;
        if (Occlusion.hidden(be, getRenderBoundingBox(be))) return;
        Direction facing = state.getValue(MarqueeBlock.FACING);

        updateRow(be, level, facing);
        byte[] columns = columns(be);
        if (columns.length == 0) return;

        int width = be.rowLength * COLUMNS;
        int start;
        if (be.getSpeed() <= 0) {
            start = columns.length >= width ? 0 : -(width - columns.length) / 2;
        } else {
            double time = (level.getGameTime() + partialTick) / 20.0;
            int cycle = columns.length + width;
            start = (int) Math.floor(time * be.getSpeed()) % cycle - width;
        }

        int rgb = be.getRgb();
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-modelRotation(facing)));
        pose.translate(-0.5, 0, -0.5);
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(DOT));
        PoseStack.Pose last = pose.last();
        for (int c = 0; c < COLUMNS; c++) {
            int textColumn = start + be.rowIndex * COLUMNS + c;
            if (textColumn < 0 || textColumn >= columns.length) continue;
            int bits = columns[textColumn];
            if (bits == 0) continue;
            float x1 = (15 - c) * PX;
            float x2 = (16 - c) * PX;
            for (int row = 0; row < MarqueeFont.ROWS; row++) {
                if ((bits & (1 << row)) == 0) continue;
                float y1 = (11 - row) * PX;
                float y2 = (12 - row) * PX;
                dot(buffer, last, x1, y1, x2, y2, r, g, b);
            }
        }
        pose.popPose();
    }

    private static int modelRotation(Direction facing) {
        return ((int) facing.toYRot() + 180) % 360;
    }

    private static void dot(VertexConsumer buffer, PoseStack.Pose pose, float x1, float y1, float x2, float y2, int r, int g, int b) {
        vertex(buffer, pose, x2, y2, 0, 0, r, g, b);
        vertex(buffer, pose, x2, y1, 0, 1, r, g, b);
        vertex(buffer, pose, x1, y1, 1, 1, r, g, b);
        vertex(buffer, pose, x1, y2, 1, 0, r, g, b);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float u, float v, int r, int g, int b) {
        buffer.addVertex(pose, x, y, Z)
                .setColor(r, g, b, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0, 0, -1);
    }

    private static byte[] columns(MarqueeBlockEntity be) {
        String text = be.getText();
        if (be.columns == null || !text.equals(be.columnsText) || be.columnsGeneration != MarqueeFont.generation()) {
            be.columns = MarqueeFont.columns(text);
            be.columnsText = text;
            be.columnsGeneration = MarqueeFont.generation();
        }
        return be.columns;
    }

    private static void updateRow(MarqueeBlockEntity be, Level level, Direction facing) {
        long now = level.getGameTime();
        if (be.rowCheckedAt >= 0 && now - be.rowCheckedAt < ROW_CHECK_TICKS && now >= be.rowCheckedAt) return;
        be.rowCheckedAt = now;
        Direction left = MarqueeBlock.left(facing);
        BlockPos pos = be.getBlockPos();
        int index = 0;
        while (index < MarqueeBlock.MAX_ROW && same(level, pos.relative(left, index + 1), facing)) index++;
        int right = 0;
        while (index + right < MarqueeBlock.MAX_ROW && same(level, pos.relative(left.getOpposite(), right + 1), facing)) right++;
        be.rowIndex = index;
        be.rowLength = index + right + 1;
    }

    private static boolean same(Level level, BlockPos pos, Direction facing) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof MarqueeBlock && state.getValue(MarqueeBlock.FACING) == facing;
    }

    @Override
    public int getViewDistance() {
        return com.sqdecor.client.ClientConfig.SCREEN_VIEW_DISTANCE.get();
    }
}
