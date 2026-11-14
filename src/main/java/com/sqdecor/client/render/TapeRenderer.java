package com.sqdecor.client.render;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sqdecor.SQDecor;
import com.sqdecor.block.entity.TapeAnchorBlockEntity;
import com.sqdecor.block.tape.TapeAnchorBlock;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class TapeRenderer implements BlockEntityRenderer<TapeAnchorBlockEntity> {
    private static final ResourceLocation TEXTURE = SQDecor.id("textures/misc/caution_tape.png");
    private static final float HALF_WIDTH = 0.07f;
    private static final float SAG_FACTOR = 0.12f;
    private static final float SAG_MAX = 1.6f;
    private static final ResourceLocation CABLE_TEXTURE = SQDecor.id("textures/misc/thin_cable.png");
    private static final float CABLE_RADIUS = 0.025f;
    private static final float CABLE_SAG_FACTOR = 0.035f;
    private static final float CABLE_SAG_MAX = 0.5f;
    private static final java.util.Map<TapeAnchorBlockEntity, WireMesh> MESHES = new java.util.WeakHashMap<>();

    public TapeRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public boolean shouldRenderOffScreen(TapeAnchorBlockEntity be) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(TapeAnchorBlockEntity be) {
        AABB box = new AABB(be.getBlockPos());
        for (BlockPos p : be.links()) box = box.minmax(new AABB(p));
        return box.inflate(2);
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public void render(TapeAnchorBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null || be.links().isEmpty()) return;
        if (!(be.getBlockState().getBlock() instanceof TapeAnchorBlock)) return;
        if (ChunkExtras.tapeInMesh(be) || LateBuffers.shadowPass()) return;
        AABB box = getRenderBoundingBox(be);
        if (Occlusion.outsideView(box) || Occlusion.hidden(be, box)) return;
        boolean cable = isCable(be);
        BlockPos self = be.getBlockPos();
        int rgb = be.getColor().getTextureDiffuseColor();
        int signature = (be.getBlockState().hashCode() * 31 + be.links().hashCode()) * 31 + rgb;
        long now = level.getGameTime();
        WireMesh mesh = MESHES.get(be);
        if (mesh == null || mesh.stale(now, signature, self)) {
            WireMesh built = new WireMesh(now, signature);
            int r = FastColor.ARGB32.red(rgb), g = FastColor.ARGB32.green(rgb), b = FastColor.ARGB32.blue(rgb);
            Matrix4f identity = new Matrix4f();
            forEachLink(be, level, (other, a, c) -> {
                emit(built, identity, level, self, cable, a, c, fitSag(level, self, a, c, cable), r, g, b);
                built.endPart();
            });
            MESHES.put(be, mesh = built);
            if (com.sqdecor.client.PerfProbe.on) com.sqdecor.client.PerfProbe.wireBuilds++;
        }
        if (mesh.isEmpty()) return;
        mesh.draw(buffers.getBuffer(RenderType.entityCutoutNoCull(cable ? CABLE_TEXTURE : TEXTURE)), pose.last(), self);
    }

    interface LinkVisitor {
        void link(BlockPos other, Vec3 a, Vec3 c);
    }

    static void forEachLink(TapeAnchorBlockEntity be, Level level, LinkVisitor visitor) {
        BlockPos self = be.getBlockPos();
        Vec3 from = TapeAnchorBlock.tip(self, be.getBlockState());
        for (BlockPos other : be.links()) {
            if (other.asLong() < self.asLong()) continue;
            var state = level.getBlockState(other);
            if (state.getBlock() != be.getBlockState().getBlock()) continue;
            if (!(level.getBlockEntity(other) instanceof TapeAnchorBlockEntity ob) || !ob.links().contains(self)) continue;
            Vec3 to = TapeAnchorBlock.tip(other, state);
            visitor.link(other, from.subtract(Vec3.atLowerCornerOf(self)), to.subtract(Vec3.atLowerCornerOf(self)));
        }
    }

    static boolean isCable(TapeAnchorBlockEntity be) {
        return be.getBlockState().getBlock() instanceof com.sqdecor.block.cable.CableClipBlock;
    }

    static float fitSag(Level level, BlockPos self, Vec3 a, Vec3 c, boolean cable) {
        return cable
                ? fitSag(level, self, a, c, (float) Math.min(CABLE_SAG_MAX, a.distanceTo(c) * CABLE_SAG_FACTOR), CABLE_RADIUS)
                : fitSag(level, self, a, c, (float) Math.min(SAG_MAX, a.distanceTo(c) * SAG_FACTOR), HALF_WIDTH);
    }

    static void emit(VertexConsumer vc, Matrix4f m, Level level, BlockPos self, boolean cable, Vec3 a, Vec3 c, float sag,
                     int r, int g, int b) {
        if (cable) tube(vc, m, level, a, c, self, sag, r, g, b);
        else ribbon(vc, m, level, a, c, self, sag, r, g, b);
    }

    private static void ribbon(VertexConsumer vc, Matrix4f m, Level level, Vec3 a, Vec3 b, BlockPos origin, float sag,
                               int r, int g, int bl) {
        double length = a.distanceTo(b);
        int segments = Math.max(4, (int) Math.ceil(length * 3));
        Vec3 prev = point(a, b, 0, sag);
        int prevLight = lightAt(level, origin, prev);
        for (int i = 1; i <= segments; i++) {
            float t = i / (float) segments;
            Vec3 cur = point(a, b, t, sag);
            int curLight = lightAt(level, origin, cur);
            float u0 = (float) (length * (i - 1) / segments * 2), u1 = (float) (length * i / segments * 2);
            quad(vc, m, prev, cur, u0, u1, prevLight, curLight, r, g, bl);
            prev = cur;
            prevLight = curLight;
        }
    }

    private static void tube(VertexConsumer vc, Matrix4f m, Level level, Vec3 a, Vec3 b, BlockPos origin, float sag,
                             int r, int g, int bl) {
        double length = a.distanceTo(b);
        int segments = Math.max(2, (int) Math.ceil(length * 2));
        Vec3 prev = point(a, b, 0, sag);
        int prevLight = lightAt(level, origin, prev);
        for (int i = 1; i <= segments; i++) {
            Vec3 cur = point(a, b, i / (float) segments, sag);
            int curLight = lightAt(level, origin, cur);
            Vec3 d = cur.subtract(prev).normalize();
            Vec3 side = d.cross(new Vec3(0, 1, 0));
            if (side.lengthSqr() < 1e-6) side = new Vec3(1, 0, 0);
            side = side.normalize();
            Vec3 up = side.cross(d).normalize();
            float u0 = (float) (length * (i - 1) / segments), u1 = (float) (length * i / segments);
            Vec3[] corner = new Vec3[4];
            for (int k = 0; k < 4; k++) {
                double ca = Math.cos(Math.PI / 2 * k + Math.PI / 4), sa = Math.sin(Math.PI / 2 * k + Math.PI / 4);
                corner[k] = up.scale(ca * CABLE_RADIUS * 1.414).add(side.scale(sa * CABLE_RADIUS * 1.414));
            }
            for (int k = 0; k < 4; k++) {
                Vec3 c0 = corner[k], c1 = corner[(k + 1) % 4];
                Vec3 n = c0.add(c1).normalize();
                tv(vc, m, prev.add(c0), u0, 0, prevLight, r, g, bl, n);
                tv(vc, m, prev.add(c1), u0, 1, prevLight, r, g, bl, n);
                tv(vc, m, cur.add(c1), u1, 1, curLight, r, g, bl, n);
                tv(vc, m, cur.add(c0), u1, 0, curLight, r, g, bl, n);
            }
            prev = cur;
            prevLight = curLight;
        }
    }

    private static void tv(VertexConsumer vc, Matrix4f m, Vec3 p, float u, float v, int light, int r, int g, int b, Vec3 n) {
        vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z)
                .setColor(r, g, b, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal((float) n.x, (float) n.y, (float) n.z);
    }

    private static float fitSag(Level level, BlockPos origin, Vec3 a, Vec3 b, float sag, float margin) {
        if (sag <= 0) return sag;
        int samples = Math.max(4, (int) Math.ceil(a.distanceTo(b) * 4));
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 1; i < samples; i++) {
            float t = i / (float) samples;
            double k = 4 * t * (1 - t);
            double x = origin.getX() + a.x + (b.x - a.x) * t;
            double y = origin.getY() + a.y + (b.y - a.y) * t;
            double z = origin.getZ() + a.z + (b.z - a.z) * t;
            double floor = Double.NEGATIVE_INFINITY;
            int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
            double lx = x - bx, lz = z - bz;
            for (int by = (int) Math.floor(y); by >= (int) Math.floor(y - sag - margin) - 1 && floor == Double.NEGATIVE_INFINITY; by--) {
                pos.set(bx, by, bz);
                var state = level.getBlockState(pos);
                if (state.getBlock() instanceof TapeAnchorBlock) continue;
                for (AABB box : state.getCollisionShape(level, pos).toAabbs()) {
                    if (lx < box.minX || lx > box.maxX || lz < box.minZ || lz > box.maxZ) continue;
                    double top = by + box.maxY;
                    if (top <= y + 1e-3) floor = Math.max(floor, top);
                }
            }
            if (floor == Double.NEGATIVE_INFINITY) continue;
            double allowed = (y - floor - margin) / k;
            if (allowed < sag) sag = (float) Math.max(0, allowed);
        }
        return sag;
    }

    private static Vec3 point(Vec3 a, Vec3 b, float t, float sag) {
        return new Vec3(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t - sag * 4 * t * (1 - t), a.z + (b.z - a.z) * t);
    }

    private static int lightAt(Level level, BlockPos origin, Vec3 p) {
        return LevelRenderer.getLightColor(level, origin.offset((int) Math.floor(p.x), (int) Math.floor(p.y), (int) Math.floor(p.z)));
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Vec3 p0, Vec3 p1, float u0, float u1, int l0, int l1, int r, int g, int b) {
        float h = HALF_WIDTH;
        for (int side = 0; side < 2; side++) {
            Vec3 d = p1.subtract(p0).normalize();
            float nx = (float) -d.z * (side == 0 ? 1 : -1);
            float nzz = (float) d.x * (side == 0 ? 1 : -1);
            float len = (float) Math.sqrt(nx * nx + nzz * nzz);
            if (len < 1e-4f) { nx = 1; nzz = 0; } else { nx /= len; nzz /= len; }
            if (side == 0) {
                v(vc, m, p0, -h, u0, 0, l0, r, g, b, nx, nzz);
                v(vc, m, p0, h, u0, 1, l0, r, g, b, nx, nzz);
                v(vc, m, p1, h, u1, 1, l1, r, g, b, nx, nzz);
                v(vc, m, p1, -h, u1, 0, l1, r, g, b, nx, nzz);
            } else {
                v(vc, m, p0, h, u0, 1, l0, r, g, b, nx, nzz);
                v(vc, m, p0, -h, u0, 0, l0, r, g, b, nx, nzz);
                v(vc, m, p1, -h, u1, 0, l1, r, g, b, nx, nzz);
                v(vc, m, p1, h, u1, 1, l1, r, g, b, nx, nzz);
            }
        }
    }

    private static void v(VertexConsumer vc, Matrix4f m, Vec3 p, float dy, float u, float v, int light,
                          int r, int g, int b, float nx, float nz) {
        vc.addVertex(m, (float) p.x, (float) p.y + dy, (float) p.z)
                .setColor(r, g, b, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(nx, 0f, nz);
    }
}
