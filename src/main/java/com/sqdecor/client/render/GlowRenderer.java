package com.sqdecor.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.sqdecor.block.Attached;
import com.sqdecor.block.entity.LampBlockEntity;
import com.sqdecor.block.entity.LedBlockEntity;
import com.sqdecor.client.ClientConfig;
import com.sqdecor.lamp.LampPart;
import com.sqdecor.led.LedLogic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.data.ModelData;

public final class GlowRenderer {
    private GlowRenderer() {}

    private static final double RADIUS = 3 / 16.0;
    private static final double FORWARD = 1.5;
    private static final double BACKWARD = 0.5;
    private static final float MAX_ALPHA = 0.6f;
    private static final int MAX_SAMPLES = 24;
    private static final double CELL = 1 / 16.0;
    private static final int MAX_CELLS = 24;
    private static final int SOLID_CHECK_TICKS = 20;
    private static final double CAMERA_TOLERANCE = 0.02;
    private static final double CAMERA_TOLERANCE_PER_BLOCK = 0.02;
    private static final int RECOMPUTE_BUDGET = 8;
    private static final int MAX_STALE_FRAMES = 6;

    private static long frame;
    private static int recomputed;

    public static void endFrame() {
        frame++;
        recomputed = 0;
    }

    private static final RenderType GLOW = RenderType.create("sqdecor_glow", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 8192, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .createCompositeState(false));

    record Info(AABB bulb, List<AABB> parts, @Nullable Direction light) {}

    private static final Map<BlockState, Info> INFO = new ConcurrentHashMap<>();
    private static final Map<LedBlockEntity, Solids> SOLIDS = new WeakHashMap<>();
    private static final Map<LedBlockEntity, Cached> CACHE = new WeakHashMap<>();

    public static void clearCache() {
        INFO.clear();
        CACHE.clear();
    }

    public static AABB bounds(BlockState state) {
        return info(state).bulb().inflate(RADIUS * FORWARD + 0.1);
    }

    public static void render(LedBlockEntity be, BlockState state, Level level, Vec3 camera, PoseStack pose,
                              MultiBufferSource buffers) {
        if (!state.hasProperty(LedLogic.LIGHT_LEVEL)) return;
        int lightLevel = state.getValue(LedLogic.LIGHT_LEVEL);
        if (lightLevel == 0) return;
        float strength = (float) (lightLevel / 15.0 * ClientConfig.HALO_STRENGTH.get()) * MAX_ALPHA;
        if (strength <= 0) return;

        Info info = info(state);
        Solids solids = solids(be, level, info);
        BlockPos pos = be.getBlockPos();
        double[] cam = {camera.x - pos.getX(), camera.y - pos.getY(), camera.z - pos.getZ()};
        Cached cached = CACHE.get(be);
        if (cached == null || needsRecompute(cached, state, lightLevel, strength, solids, cam)) {
            if (cached == null) CACHE.put(be, cached = new Cached());
            recomputed++;
            cached.frame = frame;
            Fog fog = new Fog(info, radius(lightLevel), solids);
            boolean inside = fog.contains(cam);
            double distance = Math.sqrt(fog.distanceSquared(cam));
            double cell = CELL * Math.max(1, distance / 4);
            cached.reset(state, lightLevel, strength, solids, cam, CAMERA_TOLERANCE + CAMERA_TOLERANCE_PER_BLOCK * distance);
            for (Face face : Face.values()) {
                if (!inside && !face.facesCamera(fog, cam)) continue;
                drawFace(face, fog, cam, inside, cell, strength, cached);
            }
        }

        int rgb = be instanceof LampBlockEntity lamp ? lamp.color(LampPart.BULB) & 0xFFFFFF : be.getRgb();
        if (!be.isLightTinted()) rgb = 0xFFFFFF;
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        if (cached.count == 0) return;
        VertexConsumer buffer = LateBuffers.get(GLOW, buffers);
        Matrix4f matrix = pose.last().pose();
        float[] xyz = cached.xyz;
        byte[] alpha = cached.alpha;
        for (int i = 0; i < cached.count; i++) {
            buffer.addVertex(matrix, xyz[i * 3], xyz[i * 3 + 1], xyz[i * 3 + 2]).setColor(r, g, b, alpha[i] & 0xFF);
        }
    }

    private static boolean needsRecompute(Cached cached, BlockState state, int lightLevel, float strength, Solids solids, double[] cam) {
        if (!cached.sameState(state, lightLevel, strength, solids)) return true;
        if (cached.cameraFits(cam)) return false;
        return recomputed < RECOMPUTE_BUDGET || frame - cached.frame >= MAX_STALE_FRAMES || frame < cached.frame;
    }

    private static final class Cached {
        BlockState state;
        int lightLevel;
        float strength;
        Solids solids;
        int solidsVersion;
        final double[] cam = new double[3];
        double tolerance;
        float[] xyz = new float[96];
        byte[] alpha = new byte[32];
        int count;
        long frame;

        boolean sameState(BlockState state, int lightLevel, float strength, Solids solids) {
            return this.state == state && this.lightLevel == lightLevel && this.strength == strength
                    && this.solids == solids && this.solidsVersion == solids.version;
        }

        boolean cameraFits(double[] cam) {
            double dx = cam[0] - this.cam[0];
            double dy = cam[1] - this.cam[1];
            double dz = cam[2] - this.cam[2];
            return dx * dx + dy * dy + dz * dz <= tolerance * tolerance;
        }

        void reset(BlockState state, int lightLevel, float strength, Solids solids, double[] cam, double tolerance) {
            this.state = state;
            this.lightLevel = lightLevel;
            this.strength = strength;
            this.solids = solids;
            this.solidsVersion = solids.version;
            System.arraycopy(cam, 0, this.cam, 0, 3);
            this.tolerance = tolerance;
            count = 0;
        }

        void add(float x, float y, float z, int a) {
            if (count == alpha.length) {
                alpha = java.util.Arrays.copyOf(alpha, count * 2);
                xyz = java.util.Arrays.copyOf(xyz, count * 6);
            }
            xyz[count * 3] = x;
            xyz[count * 3 + 1] = y;
            xyz[count * 3 + 2] = z;
            alpha[count++] = (byte) a;
        }
    }

    private static double radius(int lightLevel) {
        return RADIUS * (0.6 + 0.4 * lightLevel / 15.0);
    }

    private static void drawFace(Face face, Fog fog, double[] cam, boolean inside, double cell, float strength,
                                 Cached out) {
        int a = face.axis;
        double plane = face.max ? fog.max[a] : fog.min[a];
        double lenU = fog.max[face.u] - fog.min[face.u];
        double lenV = fog.max[face.v] - fog.min[face.v];
        int nu = Mth.clamp((int) Math.ceil(lenU / cell), 2, MAX_CELLS);
        int nv = Mth.clamp((int) Math.ceil(lenV / cell), 2, MAX_CELLS);

        float[] points = POINTS;
        int[] alpha = ALPHA;
        double[] p = P;
        for (int j = 0; j <= nv; j++) {
            for (int i = 0; i <= nu; i++) {
                p[a] = plane;
                p[face.u] = fog.min[face.u] + lenU * i / nu;
                p[face.v] = fog.min[face.v] + lenV * j / nv;
                int k = j * (nu + 1) + i;
                points[k * 3] = (float) p[0];
                points[k * 3 + 1] = (float) p[1];
                points[k * 3 + 2] = (float) p[2];
                alpha[k] = Math.round(Math.min(1f, fog.along(cam, p, inside)) * strength * 255);
            }
        }

        for (int j = 0; j < nv; j++) {
            for (int i = 0; i < nu; i++) {
                int k00 = j * (nu + 1) + i;
                int k10 = k00 + 1;
                int k01 = k00 + nu + 1;
                int k11 = k01 + 1;
                if (alpha[k00] == 0 && alpha[k10] == 0 && alpha[k01] == 0 && alpha[k11] == 0) continue;
                if (inside) {
                    corner(out, points, alpha, k00);
                    corner(out, points, alpha, k01);
                    corner(out, points, alpha, k11);
                    corner(out, points, alpha, k10);
                } else {
                    corner(out, points, alpha, k00);
                    corner(out, points, alpha, k10);
                    corner(out, points, alpha, k11);
                    corner(out, points, alpha, k01);
                }
            }
        }
    }

    private static final float[] POINTS = new float[(MAX_CELLS + 1) * (MAX_CELLS + 1) * 3];
    private static final int[] ALPHA = new int[(MAX_CELLS + 1) * (MAX_CELLS + 1)];
    private static final double[] P = new double[3];

    private static void corner(Cached out, float[] points, int[] alpha, int k) {
        out.add(points[k * 3], points[k * 3 + 1], points[k * 3 + 2], alpha[k]);
    }

    private enum Face {
        DOWN(1, false, 0, 2), UP(1, true, 2, 0),
        NORTH(2, false, 1, 0), SOUTH(2, true, 0, 1),
        WEST(0, false, 2, 1), EAST(0, true, 1, 2);

        final int axis;
        final boolean max;
        final int u;
        final int v;

        Face(int axis, boolean max, int u, int v) {
            this.axis = axis;
            this.max = max;
            this.u = u;
            this.v = v;
        }

        boolean facesCamera(Fog fog, double[] cam) {
            return max ? cam[axis] > fog.max[axis] : cam[axis] < fog.min[axis];
        }
    }

    private static final class Fog {
        final double[] bulbMin;
        final double[] bulbMax;
        final double[][] parts;
        final double[] reachMin = new double[3];
        final double[] reachMax = new double[3];
        final double[] min = new double[3];
        final double[] max = new double[3];
        final double norm;
        final double sampleStep;
        final Solids solids;

        Fog(Info info, double radius, Solids solids) {
            AABB bulb = info.bulb();
            bulbMin = new double[]{bulb.minX, bulb.minY, bulb.minZ};
            bulbMax = new double[]{bulb.maxX, bulb.maxY, bulb.maxZ};
            parts = new double[info.parts().size()][];
            for (int i = 0; i < parts.length; i++) {
                AABB part = info.parts().get(i);
                parts[i] = new double[]{part.minX, part.minY, part.minZ, part.maxX, part.maxY, part.maxZ};
            }
            for (int a = 0; a < 3; a++) {
                reachMin[a] = radius;
                reachMax[a] = radius;
            }
            if (info.light() != null) {
                int a = info.light().getAxis().ordinal();
                boolean positive = info.light().getAxisDirection() == Direction.AxisDirection.POSITIVE;
                (positive ? reachMax : reachMin)[a] = radius * FORWARD;
                (positive ? reachMin : reachMax)[a] = radius * BACKWARD;
            }
            for (int a = 0; a < 3; a++) {
                min[a] = bulbMin[a] - reachMin[a];
                max[a] = bulbMax[a] + reachMax[a];
            }
            norm = 1 / (2.0 / 3.0 * radius);
            sampleStep = radius * BACKWARD / 2;
            this.solids = solids;
        }

        boolean contains(double[] p) {
            for (int a = 0; a < 3; a++) {
                if (p[a] <= min[a] || p[a] >= max[a]) return false;
            }
            return true;
        }

        double distanceSquared(double[] p) {
            double sum = 0;
            for (int a = 0; a < 3; a++) {
                double d = Math.max(0, Math.max(min[a] - p[a], p[a] - max[a]));
                sum += d * d;
            }
            return sum;
        }

        double density(double x, double y, double z) {
            double best = 0;
            for (double[] part : parts) {
                double t = sq(offset(x, 0, part)) + sq(offset(y, 1, part)) + sq(offset(z, 2, part));
                if (t == 0) return -1;
                if (t >= 1) continue;
                double f = 1 - Math.sqrt(t);
                best = Math.max(best, f * f);
            }
            return best;
        }

        private double offset(double value, int a, double[] part) {
            if (value < part[a]) return (part[a] - value) / reachMin[a];
            if (value > part[a + 3]) return (value - part[a + 3]) / reachMax[a];
            return 0;
        }

        float along(double[] cam, double[] point, boolean inside) {
            double dx = point[0] - cam[0];
            double dy = point[1] - cam[1];
            double dz = point[2] - cam[2];
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length < 1e-6) return 0;
            double[] dir = {dx / length, dy / length, dz / length};
            double start;
            double end;
            if (inside) {
                start = 0;
                end = length;
            } else {
                start = length;
                end = exit(cam, dir);
                if (end <= start) return 0;
            }
            end = solids.firstHit(cam, dir, start, end);
            if (end <= start) return 0;

            int samples = Mth.clamp((int) Math.ceil((end - start) / sampleStep), 4, MAX_SAMPLES);
            double step = (end - start) / samples;
            double sum = 0;
            double peak = 0;
            for (int k = 0; k < samples; k++) {
                double t = start + (k + 0.5) * step;
                double x = cam[0] + dir[0] * t;
                double y = cam[1] + dir[1] * t;
                double z = cam[2] + dir[2] * t;
                double d = density(x, y, z);
                if (d < 0) break;
                int bx = Mth.floor(x);
                int by = Mth.floor(y);
                int bz = Mth.floor(z);
                Fog other = (bx | by | bz) == 0 ? null : solids.glowAt(bx, by, bz);
                if (other != null) {
                    double o = other.density(x - bx, y - by, z - bz);
                    if (o < 0) break;
                    d = Math.max(0, d - o);
                }
                sum += d;
                peak = Math.max(peak, d);
            }
            return (float) Math.min(sum * step * norm, peak);
        }

        private double exit(double[] cam, double[] dir) {
            double t = Double.MAX_VALUE;
            for (int a = 0; a < 3; a++) {
                if (dir[a] > 1e-9) t = Math.min(t, (max[a] - cam[a]) / dir[a]);
                else if (dir[a] < -1e-9) t = Math.min(t, (min[a] - cam[a]) / dir[a]);
            }
            return t;
        }

        private static double sq(double v) {
            return v * v;
        }
    }

    private static final class Solids {
        final int x0;
        final int y0;
        final int z0;
        final int sx;
        final int sy;
        final int sz;
        final boolean[] solid;
        final Fog[] glow;
        long checkedAt;
        int version;
        private long signature;

        Solids(int x0, int y0, int z0, int sx, int sy, int sz) {
            this.x0 = x0;
            this.y0 = y0;
            this.z0 = z0;
            this.sx = sx;
            this.sy = sy;
            this.sz = sz;
            this.solid = new boolean[sx * sy * sz];
            this.glow = new Fog[sx * sy * sz];
        }

        void fill(Level level, BlockPos pos) {
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            long sig = 17;
            for (int x = 0; x < sx; x++) {
                for (int y = 0; y < sy; y++) {
                    for (int z = 0; z < sz; z++) {
                        int bx = x0 + x;
                        int by = y0 + y;
                        int bz = z0 + z;
                        p.set(pos.getX() + bx, pos.getY() + by, pos.getZ() + bz);
                        boolean own = bx == 0 && by == 0 && bz == 0;
                        int index = (x * sy + y) * sz + z;
                        BlockState state = level.getBlockState(p);
                        solid[index] = !own && state.isSolidRender(level, p);
                        glow[index] = null;
                        if (!own && level.getBlockEntity(p) instanceof LedBlockEntity
                                && state.hasProperty(LedLogic.LIGHT_LEVEL)) {
                            int lightLevel = state.getValue(LedLogic.LIGHT_LEVEL);
                            if (lightLevel > 0) {
                                glow[index] = new Fog(info(state), radius(lightLevel), null);
                                sig = sig * 31 + System.identityHashCode(state);
                            }
                        }
                        sig = sig * 31 + (solid[index] ? 1 : 0);
                    }
                }
            }
            checkedAt = level.getGameTime();
            if (sig != signature || version == 0) {
                signature = sig;
                version++;
            }
        }

        boolean solidAt(int x, int y, int z) {
            int ix = x - x0;
            int iy = y - y0;
            int iz = z - z0;
            if (ix < 0 || iy < 0 || iz < 0 || ix >= sx || iy >= sy || iz >= sz) return false;
            return solid[(ix * sy + iy) * sz + iz];
        }

        @Nullable
        Fog glowAt(int x, int y, int z) {
            int ix = x - x0;
            int iy = y - y0;
            int iz = z - z0;
            if (ix < 0 || iy < 0 || iz < 0 || ix >= sx || iy >= sy || iz >= sz) return null;
            return glow[(ix * sy + iy) * sz + iz];
        }

        boolean inGrid(int x, int y, int z) {
            return x >= x0 && y >= y0 && z >= z0 && x < x0 + sx && y < y0 + sy && z < z0 + sz;
        }

        double firstHit(double[] o, double[] d, double start, double end) {
            double px = o[0] + d[0] * start;
            double py = o[1] + d[1] * start;
            double pz = o[2] + d[2] * start;
            int x = Mth.floor(px);
            int y = Mth.floor(py);
            int z = Mth.floor(pz);
            if (solidAt(x, y, z)) return start;
            int stepX = d[0] > 0 ? 1 : -1;
            int stepY = d[1] > 0 ? 1 : -1;
            int stepZ = d[2] > 0 ? 1 : -1;
            double deltaX = Math.abs(d[0]) < 1e-9 ? Double.MAX_VALUE : 1 / Math.abs(d[0]);
            double deltaY = Math.abs(d[1]) < 1e-9 ? Double.MAX_VALUE : 1 / Math.abs(d[1]);
            double deltaZ = Math.abs(d[2]) < 1e-9 ? Double.MAX_VALUE : 1 / Math.abs(d[2]);
            double nextX = deltaX == Double.MAX_VALUE ? Double.MAX_VALUE : start + (d[0] > 0 ? x + 1 - px : px - x) * deltaX;
            double nextY = deltaY == Double.MAX_VALUE ? Double.MAX_VALUE : start + (d[1] > 0 ? y + 1 - py : py - y) * deltaY;
            double nextZ = deltaZ == Double.MAX_VALUE ? Double.MAX_VALUE : start + (d[2] > 0 ? z + 1 - pz : pz - z) * deltaZ;
            while (true) {
                double t = Math.min(nextX, Math.min(nextY, nextZ));
                if (t >= end) return end;
                if (t == nextX) {
                    x += stepX;
                    nextX += deltaX;
                } else if (t == nextY) {
                    y += stepY;
                    nextY += deltaY;
                } else {
                    z += stepZ;
                    nextZ += deltaZ;
                }
                if (!inGrid(x, y, z)) return end;
                if (solidAt(x, y, z)) return t;
            }
        }
    }

    private static Solids solids(LedBlockEntity be, Level level, Info info) {
        Solids solids = SOLIDS.get(be);
        long now = level.getGameTime();
        if (solids == null) {
            AABB area = info.bulb().inflate(RADIUS * FORWARD);
            int x0 = Mth.floor(area.minX);
            int y0 = Mth.floor(area.minY);
            int z0 = Mth.floor(area.minZ);
            solids = new Solids(x0, y0, z0, Mth.floor(area.maxX) - x0 + 1, Mth.floor(area.maxY) - y0 + 1,
                    Mth.floor(area.maxZ) - z0 + 1);
            solids.fill(level, be.getBlockPos());
            SOLIDS.put(be, solids);
        } else if (now != solids.checkedAt && (Math.floorMod(now + be.getBlockPos().hashCode(), SOLID_CHECK_TICKS) == 0
                || now - solids.checkedAt > SOLID_CHECK_TICKS || now < solids.checkedAt)) {
            solids.fill(level, be.getBlockPos());
        }
        return solids;
    }

    static Info info(BlockState state) {
        return INFO.computeIfAbsent(state, GlowRenderer::readInfo);
    }

    private static Info readInfo(BlockState state) {
        Direction support = state.getBlock() instanceof Attached attached ? attached.supportDirection(state) : null;
        Direction light = support == null ? null : support.getOpposite();

        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        List<BakedQuad> quads = new ArrayList<>();
        for (Direction side : Direction.values()) {
            quads.addAll(model.getQuads(state, side, RandomSource.create(42L), ModelData.EMPTY, null));
        }
        quads.addAll(model.getQuads(state, null, RandomSource.create(42L), ModelData.EMPTY, null));

        List<AABB> parts = new ArrayList<>();
        for (BakedQuad quad : quads) {
            if (quad.getTintIndex() != 0) continue;
            double[] lo = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
            double[] hi = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
            int[] data = quad.getVertices();
            int stride = data.length / 4;
            for (int i = 0; i < 4; i++) {
                for (int a = 0; a < 3; a++) {
                    double value = Float.intBitsToFloat(data[i * stride + a]);
                    lo[a] = Math.min(lo[a], value);
                    hi[a] = Math.max(hi[a], value);
                }
            }
            parts.add(new AABB(lo[0], lo[1], lo[2], hi[0], hi[1], hi[2]));
        }
        if (parts.isEmpty()) {
            VoxelShape shape = state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            parts.add(shape.isEmpty() ? new AABB(0.4, 0.4, 0.4, 0.6, 0.6, 0.6) : shape.bounds());
        }
        parts = merge(parts);
        AABB bulb = parts.getFirst();
        for (AABB part : parts) bulb = bulb.minmax(part);
        return new Info(bulb, List.copyOf(parts), light);
    }

    private static List<AABB> merge(List<AABB> boxes) {
        List<AABB> list = new ArrayList<>(boxes);
        boolean merged = true;
        while (merged && list.size() > 1) {
            merged = false;
            outer:
            for (int i = 0; i < list.size(); i++) {
                for (int j = i + 1; j < list.size(); j++) {
                    AABB a = list.get(i);
                    AABB b = list.get(j);
                    AABB both = a.minmax(b);
                    if (volume(both) <= 1.1 * (volume(a) + volume(b))) {
                        list.set(i, both);
                        list.remove(j);
                        merged = true;
                        break outer;
                    }
                }
            }
        }
        return list;
    }

    private static double volume(AABB box) {
        return (box.getXsize() + RADIUS) * (box.getYsize() + RADIUS) * (box.getZsize() + RADIUS);
    }
}
