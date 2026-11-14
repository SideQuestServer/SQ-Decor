package com.sqdecor.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sqdecor.block.entity.ImageMonitorBlockEntity;
import com.sqdecor.block.monitor.ImageMonitorBlock;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class ImageMonitorRenderer implements BlockEntityRenderer<ImageMonitorBlockEntity> {
    private static final int SCREEN_CHECK_TICKS = 10;
    private static final float PX = 1 / 16f;
    private static final float Z = 14 / 16f - 0.003f;

    private static final java.util.Set<ScreenTexture> LIVE = new java.util.HashSet<>();

    private static final class ScreenTexture {
        final DynamicTexture texture;
        final ResourceLocation location;
        final int width;
        final int height;
        int version = -1;

        ScreenTexture(int width, int height) {
            this.width = width;
            this.height = height;
            texture = new DynamicTexture(width, height, false);
            location = Minecraft.getInstance().getTextureManager().register("sqdecor_monitor", texture);
            LIVE.add(this);
        }

        void release() {
            if (LIVE.remove(this)) Minecraft.getInstance().getTextureManager().release(location);
        }
    }

    public static void releaseAll() {
        for (ScreenTexture tex : java.util.List.copyOf(LIVE)) tex.release();
    }

    public ImageMonitorRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(ImageMonitorBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Level level = be.getLevel();
        byte[] pixels = be.pixels();
        if (level == null || pixels == null) return;
        if (LateBuffers.shadowPass()) return;
        ImageMonitorBlock.Screen screen = screen(be, level);
        if (screen == null || !screen.origin().equals(be.getBlockPos())) return;
        if (tooFar(be, screen)) return;
        AABB screenBox = screenBox(be.getBlockPos(), screen);
        if (Occlusion.outsideView(screenBox) || Occlusion.hidden(be, screenBox)) return;

        ScreenTexture tex = texture(be);
        if (tex.version != be.version) upload(tex, be, pixels);

        float w = screen.width();
        float h = screen.height();
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        int[] rot = ImageMonitorBlock.modelRotation(screen.facing(), screen.face());
        pose.mulPose(Axis.YP.rotationDegrees(-rot[1]));
        pose.mulPose(Axis.XP.rotationDegrees(-rot[0]));
        pose.translate(-0.5, -0.5, -0.5);
        float left = 1 - PX;
        float right = 1 - w + PX;
        float bottom = PX;
        float top = h - PX;
        VertexConsumer buffer = buffers.getBuffer(RenderType.text(tex.location));
        PoseStack.Pose last = pose.last();
        vertex(buffer, last, left, top, 0, 0);
        vertex(buffer, last, left, bottom, 0, 1);
        vertex(buffer, last, right, bottom, 1, 1);
        vertex(buffer, last, right, top, 1, 0);
        pose.popPose();
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float u, float v) {
        buffer.addVertex(pose, x, y, Z).setColor(255, 255, 255, 255).setUv(u, v).setLight(LightTexture.FULL_BRIGHT);
    }

    private static ScreenTexture texture(ImageMonitorBlockEntity be) {
        if (be.texture instanceof ScreenTexture tex && tex.width == be.resWidth() && tex.height == be.resHeight()) return tex;
        if (be.texture instanceof ScreenTexture old) old.release();
        ScreenTexture tex = new ScreenTexture(be.resWidth(), be.resHeight());
        be.texture = tex;
        be.onClientRemoved = tex::release;
        return tex;
    }

    private static void upload(ScreenTexture tex, ImageMonitorBlockEntity be, byte[] pixels) {
        NativeImage image = tex.texture.getPixels();
        if (image == null) return;
        int y0 = tex.version < 0 ? 0 : Math.max(0, be.dirtyFrom);
        int y1 = tex.version < 0 ? tex.height : Math.min(tex.height, be.dirtyTo);
        be.dirtyFrom = 0;
        be.dirtyTo = 0;
        tex.version = be.version;
        if (y0 >= y1) return;
        for (int y = y0; y < y1; y++) {
            int i = y * tex.width * 3;
            for (int x = 0; x < tex.width; x++, i += 3) {
                if (i + 2 >= pixels.length) break;
                int abgr = 0xFF000000 | (pixels[i + 2] & 0xFF) << 16 | (pixels[i + 1] & 0xFF) << 8 | (pixels[i] & 0xFF);
                image.setPixelRGBA(x, y, abgr);
            }
        }
        tex.texture.bind();
        image.upload(0, 0, y0, 0, y0, tex.width, y1 - y0, false, false);
    }

    private static ImageMonitorBlock.Screen screen(ImageMonitorBlockEntity be, Level level) {
        long now = level.getGameTime();
        if (be.screen == null || be.screenCheckedAt < 0 || now - be.screenCheckedAt >= SCREEN_CHECK_TICKS || now < be.screenCheckedAt) {
            be.screen = ImageMonitorBlock.screen(level, be.getBlockPos());
            be.screenCheckedAt = now;
        }
        return be.screen;
    }

    private static AABB screenBox(BlockPos origin, ImageMonitorBlock.Screen screen) {
        BlockPos corner = origin.relative(screen.right(), screen.width() - 1).relative(screen.up(), screen.height() - 1);
        return new AABB(origin).minmax(new AABB(corner));
    }

    @Override
    public AABB getRenderBoundingBox(ImageMonitorBlockEntity be) {
        BlockPos pos = be.getBlockPos();
        int r = ImageMonitorBlock.MAX_SIZE;
        return new AABB(pos).inflate(r, 0, r).expandTowards(0, r, 0);
    }

    @Override
    public boolean shouldRenderOffScreen(ImageMonitorBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    private static boolean tooFar(ImageMonitorBlockEntity be, ImageMonitorBlock.Screen screen) {
        double base = com.sqdecor.client.ClientConfig.SCREEN_VIEW_DISTANCE.get();
        double limit = Math.min(96, Math.max(base, base * Math.max(screen.width(), screen.height()) / 4.0));
        var camera = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        return net.minecraft.world.phys.Vec3.atCenterOf(be.getBlockPos()).distanceToSqr(camera) > limit * limit;
    }
}
