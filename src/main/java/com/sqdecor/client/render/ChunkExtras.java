package com.sqdecor.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sqdecor.SQDecor;
import com.sqdecor.block.entity.LampBlockEntity;
import com.sqdecor.block.entity.LedBlockEntity;
import com.sqdecor.block.entity.TapeAnchorBlockEntity;
import com.sqdecor.block.lamp.HangLampBlock;
import com.sqdecor.client.ClientConfig;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;

public final class ChunkExtras {
    private ChunkExtras() {}

    private static final long OVERLAP_NANOS = 700_000_000L;
    private static final int CHAIN_CHECK_TICKS = 10;
    private static final int TAPE_CHECK_TICKS = 20;
    private static final BlockState CHAIN = Blocks.CHAIN.defaultBlockState();
    private static final ResourceLocation TAPE_SPRITE = SQDecor.id("block/tape/caution_tape");
    private static final ResourceLocation CABLE_SPRITE = SQDecor.id("block/tape/thin_cable");

    private abstract static class Placement {
        final int minX, minY, minZ, maxX, maxY, maxZ;
        long[] sections = new long[0];
        volatile long delivered;

        Placement(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        boolean touches(BlockPos o) {
            return Arrays.binarySearch(sections, SectionPos.asLong(o.getX() >> 4, o.getY() >> 4, o.getZ() >> 4)) >= 0;
        }

        void hosts(long[] perPart) {
            sections = Arrays.stream(perPart).distinct().sorted().toArray();
        }

        boolean inMesh() {
            long d = delivered;
            return d != 0 && System.nanoTime() - d > OVERLAP_NANOS;
        }

        boolean empty() {
            return false;
        }

        abstract void paint(AddSectionGeometryEvent.SectionRenderingContext ctx, BlockPos origin, RandomSource random);
    }

    private static final class Chain extends Placement {
        final int x, z, bottom, top;
        final long[] host;

        Chain(int x, int bottom, int top, int z, long[] host) {
            super(x, bottom, z, x, top, z);
            this.x = x;
            this.z = z;
            this.bottom = bottom;
            this.top = top;
            this.host = host;
            hosts(host);
        }

        boolean same(Chain o) {
            return o.x == x && o.z == z && o.bottom == bottom && o.top == top && Arrays.equals(o.host, host);
        }

        @Override
        void paint(AddSectionGeometryEvent.SectionRenderingContext ctx, BlockPos origin, RandomSource random) {
            BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
            BakedModel model = blocks.getBlockModel(CHAIN);
            PoseStack pose = ctx.getPoseStack();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            long sec = SectionPos.asLong(origin.getX() >> 4, origin.getY() >> 4, origin.getZ() >> 4);
            for (int y = bottom; y <= top; y++) {
                if (host[y - bottom] != sec) continue;
                pos.set(x, y, z);
                pose.pushPose();
                pose.translate(x - origin.getX(), y - origin.getY(), z - origin.getZ());
                for (RenderType type : model.getRenderTypes(CHAIN, random, ModelData.EMPTY)) {
                    blocks.renderBatched(CHAIN, pos, ctx.getRegion(), pose, ctx.getOrCreateChunkBuffer(type), false, random,
                            ModelData.EMPTY, type);
                }
                pose.popPose();
            }
        }
    }

    private static final class Tape extends Placement {
        final int ax, ay, az;
        final float[] data;
        final int rgb;
        final boolean cable;
        final TextureAtlasSprite sprite;
        final long[] host;

        Tape(BlockPos anchor, float[] data, int[] box, int rgb, boolean cable, TextureAtlasSprite sprite, long[] host) {
            super(box[0], box[1], box[2], box[3], box[4], box[5]);
            this.ax = anchor.getX();
            this.ay = anchor.getY();
            this.az = anchor.getZ();
            this.data = data;
            this.rgb = rgb;
            this.cable = cable;
            this.sprite = sprite;
            this.host = host;
            hosts(host);
        }

        boolean same(Tape o) {
            return o.rgb == rgb && o.cable == cable && o.sprite == sprite && Arrays.equals(o.data, data)
                    && Arrays.equals(o.host, host);
        }

        @Override
        boolean empty() {
            return data.length == 0;
        }

        @Override
        boolean inMesh() {
            return empty() || super.inMesh();
        }

        @Override
        void paint(AddSectionGeometryEvent.SectionRenderingContext ctx, BlockPos origin, RandomSource random) {
            VertexConsumer vc = ctx.getOrCreateChunkBuffer(RenderType.cutout());
            BlockAndTintGetter region = ctx.getRegion();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            long sec = SectionPos.asLong(origin.getX() >> 4, origin.getY() >> 4, origin.getZ() >> 4);
            float ox = ax - origin.getX(), oy = ay - origin.getY(), oz = az - origin.getZ();
            for (int q = 0; q + QUAD <= data.length; q += QUAD) {
                if (host[q / QUAD] != sec) continue;
                double cx = ax + (data[q] + data[q + 8] + data[q + 16] + data[q + 24]) * 0.25;
                double cy = ay + (data[q + 1] + data[q + 9] + data[q + 17] + data[q + 25]) * 0.25;
                double cz = az + (data[q + 2] + data[q + 10] + data[q + 18] + data[q + 26]) * 0.25;
                int bx = (int) Math.floor(cx), by = (int) Math.floor(cy), bz = (int) Math.floor(cz);
                int light = LevelRenderer.getLightColor(region, pos.set(bx, by, bz));
                emit(vc, q, light, ox, oy, oz);
            }
        }

        private void emit(VertexConsumer vc, int q, int light, float ox, float oy, float oz) {
            float u0 = data[q + 3], u1 = data[q + 19];
            List<Float> cuts = new ArrayList<>(4);
            cuts.add(0f);
            if (Math.abs(u1 - u0) > 1e-6f) {
                float lo = Math.min(u0, u1), hi = Math.max(u0, u1);
                for (int k = (int) Math.floor(lo) + 1; k < hi - 1e-4f; k++) {
                    float t = (k - u0) / (u1 - u0);
                    if (t > 1e-4f && t < 1 - 1e-4f) cuts.add(t);
                }
                cuts.sort(null);
            }
            cuts.add(1f);
            for (int i = 0; i + 1 < cuts.size(); i++) {
                float ta = cuts.get(i), tb = cuts.get(i + 1);
                float cell = (float) Math.floor(u0 + (u1 - u0) * (ta + tb) * 0.5f);
                float[] a0 = lerp(q, q + 24, ta), a1 = lerp(q + 8, q + 16, ta);
                float[] b1 = lerp(q + 8, q + 16, tb), b0 = lerp(q, q + 24, tb);
                vertex(vc, a0, cell, light, ox, oy, oz);
                vertex(vc, a1, cell, light, ox, oy, oz);
                vertex(vc, b1, cell, light, ox, oy, oz);
                vertex(vc, b0, cell, light, ox, oy, oz);
                if (cable) {
                    vertex(vc, b0, cell, light, ox, oy, oz);
                    vertex(vc, b1, cell, light, ox, oy, oz);
                    vertex(vc, a1, cell, light, ox, oy, oz);
                    vertex(vc, a0, cell, light, ox, oy, oz);
                }
            }
        }

        private float[] lerp(int from, int to, float t) {
            float[] out = new float[8];
            for (int k = 0; k < 8; k++) out[k] = data[from + k] + (data[to + k] - data[from + k]) * t;
            return out;
        }

        private void vertex(VertexConsumer vc, float[] p, float cell, int light, float ox, float oy, float oz) {
            float u = Math.max(0f, Math.min(1f, p[3] - cell));
            float v = Math.max(0f, Math.min(1f, p[4]));
            vc.addVertex(p[0] + ox, p[1] + oy, p[2] + oz)
                    .setColor(FastColor.ARGB32.red(rgb), FastColor.ARGB32.green(rgb), FastColor.ARGB32.blue(rgb), 255)
                    .setUv(sprite.getU(u), sprite.getV(v))
                    .setLight(light)
                    .setNormal(p[5], p[6], p[7]);
        }
    }

    private static final int VERTEX = 8, QUAD = 4 * VERTEX;
    private static final int RANGE_MIN = -7, RANGE_MAX = 23;

    private static final Map<Long, Chain> CHAINS = new ConcurrentHashMap<>();
    private static final Map<Long, Tape> TAPES = new ConcurrentHashMap<>();
    private static final Map<Long, LampBlockEntity> LAMPS = new HashMap<>();
    private static final Map<Long, TapeAnchorBlockEntity> ANCHORS = new HashMap<>();
    private static final LongOpenHashSet DUE = new LongOpenHashSet();
    private static Level cacheLevel;
    private static boolean wasOn;

    public static boolean enabled() {
        return ClientConfig.CHUNK_MESHES.get();
    }

    public static void lampLoaded(LedBlockEntity be) {
        if (be instanceof LampBlockEntity lamp && be.getBlockState().getBlock() instanceof HangLampBlock) {
            LAMPS.put(be.getBlockPos().asLong(), lamp);
        }
    }

    public static void anchorChanged(TapeAnchorBlockEntity be) {
        long key = be.getBlockPos().asLong();
        ANCHORS.put(key, be);
        DUE.add(key);
        for (BlockPos other : be.links()) DUE.add(other.asLong());
    }

    public static boolean chainInMesh(LampBlockEntity lamp) {
        Chain chain = CHAINS.get(lamp.getBlockPos().asLong());
        return chain != null && chain.inMesh();
    }

    public static boolean tapeInMesh(TapeAnchorBlockEntity be) {
        Tape tape = TAPES.get(be.getBlockPos().asLong());
        return tape != null && tape.inMesh();
    }

    public static int chainsInMesh() {
        return (int) CHAINS.values().stream().filter(Placement::inMesh).count();
    }

    public static int tapesInMesh() {
        return (int) TAPES.values().stream().filter(t -> !t.empty() && t.inMesh()).count();
    }

    public static int notInMesh() {
        int lamps = (int) LAMPS.values().stream().filter(l -> l.chainLength > 0 && !chainInMesh(l)).count();
        int anchors = (int) ANCHORS.values().stream().filter(a -> !a.links().isEmpty() && !tapeInMesh(a)).count();
        return lamps + anchors;
    }

    public static void reload() {
        TAPES.clear();
        DUE.addAll(ANCHORS.keySet());
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Level level = Minecraft.getInstance().level;
        if (level != cacheLevel) {
            CHAINS.clear();
            TAPES.clear();
            LAMPS.clear();
            ANCHORS.clear();
            DUE.clear();
            cacheLevel = level;
        }
        if (level == null) return;
        boolean on = enabled();
        if (on != wasOn) {
            wasOn = on;
            for (Placement p : CHAINS.values()) dirty(p);
            for (Placement p : TAPES.values()) dirty(p);
            CHAINS.clear();
            TAPES.clear();
            if (on) DUE.addAll(ANCHORS.keySet());
        }
        long now = level.getGameTime();

        Iterator<Map.Entry<Long, LampBlockEntity>> lamps = LAMPS.entrySet().iterator();
        while (lamps.hasNext()) {
            Map.Entry<Long, LampBlockEntity> e = lamps.next();
            long key = e.getKey();
            LampBlockEntity lamp = e.getValue();
            BlockPos pos = lamp.getBlockPos();
            if (lamp.isRemoved() || level.getBlockEntity(pos) != lamp || !(lamp.getBlockState().getBlock() instanceof HangLampBlock)) {
                lamps.remove();
                remove(CHAINS.remove(key));
                continue;
            }
            boolean fresh = lamp.chainLength < 0 || (on && lamp.chainLength > 0 && !CHAINS.containsKey(key));
            if (!fresh && Math.floorMod(now + key, CHAIN_CHECK_TICKS) != 0) continue;
            int length = HangLampBlock.chainLength(level, pos);
            lamp.chainLength = length;
            lamp.chainCheckedAt = now;
            if (on) updateChain(key, pos, length, level);
        }

        Iterator<Map.Entry<Long, TapeAnchorBlockEntity>> anchors = ANCHORS.entrySet().iterator();
        while (anchors.hasNext()) {
            Map.Entry<Long, TapeAnchorBlockEntity> e = anchors.next();
            long key = e.getKey();
            TapeAnchorBlockEntity be = e.getValue();
            if (be.isRemoved() || level.getBlockEntity(be.getBlockPos()) != be) {
                anchors.remove();
                remove(TAPES.remove(key));
                continue;
            }
            if (!on) continue;
            if (!DUE.contains(key) && Math.floorMod(now + key, TAPE_CHECK_TICKS) != 0) continue;
            updateTape(key, be, level);
        }
        DUE.clear();
    }

    private static void updateChain(long key, BlockPos pos, int length, Level level) {
        Chain old = CHAINS.get(key);
        if (length <= 0) {
            if (old != null) remove(CHAINS.remove(key));
            return;
        }
        Map<Long, Boolean> air = new HashMap<>();
        long[] host = new long[length];
        int x = pos.getX(), z = pos.getZ();
        for (int i = 0; i < length; i++) {
            int y = pos.getY() + 1 + i;
            host[i] = host(level, air, x, y, z, x + 1, y + 1, z + 1, x + 0.5, y + 0.5, z + 0.5);
        }
        Chain chain = new Chain(x, pos.getY() + 1, pos.getY() + length, z, host);
        if (old != null && old.same(chain)) return;
        if (old != null) {
            chain.delivered = old.delivered;
            dirty(old);
        }
        CHAINS.put(key, chain);
        dirty(chain);
    }

    private static void updateTape(long key, TapeAnchorBlockEntity be, Level level) {
        Recorder rec = new Recorder();
        BlockPos self = be.getBlockPos();
        boolean cable = TapeRenderer.isCable(be);
        int rgb = be.getColor().getTextureDiffuseColor();
        int r = FastColor.ARGB32.red(rgb), g = FastColor.ARGB32.green(rgb), b = FastColor.ARGB32.blue(rgb);
        Matrix4f identity = new Matrix4f();
        TapeRenderer.forEachLink(be, level, (other, a, c) ->
                TapeRenderer.emit(rec, identity, level, self, cable, a, c, TapeRenderer.fitSag(level, self, a, c, cable), r, g, b));
        float[] data = Arrays.copyOf(rec.data, rec.count * VERTEX);
        TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS)
                .getSprite(cable ? CABLE_SPRITE : TAPE_SPRITE);
        Tape tape = new Tape(self, data, box(self, data), 0xFF000000 | rgb, cable, sprite, hosts(level, self, data));
        Tape old = TAPES.get(key);
        if (old != null && old.same(tape)) return;
        if (old != null) {
            if (!old.empty()) tape.delivered = old.delivered;
            dirty(old);
        }
        TAPES.put(key, tape);
        dirty(tape);
    }

    private static int[] box(BlockPos anchor, float[] data) {
        if (data.length == 0) return new int[] {anchor.getX(), anchor.getY(), anchor.getZ(), anchor.getX(), anchor.getY(), anchor.getZ()};
        float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE}, max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (int i = 0; i + VERTEX <= data.length; i += VERTEX) {
            for (int k = 0; k < 3; k++) {
                min[k] = Math.min(min[k], data[i + k]);
                max[k] = Math.max(max[k], data[i + k]);
            }
        }
        return new int[] {anchor.getX() + (int) Math.floor(min[0]), anchor.getY() + (int) Math.floor(min[1]),
                anchor.getZ() + (int) Math.floor(min[2]), anchor.getX() + (int) Math.floor(max[0]),
                anchor.getY() + (int) Math.floor(max[1]), anchor.getZ() + (int) Math.floor(max[2])};
    }

    private static long[] hosts(Level level, BlockPos anchor, float[] data) {
        Map<Long, Boolean> air = new HashMap<>();
        long[] host = new long[data.length / QUAD];
        float ax = anchor.getX(), ay = anchor.getY(), az = anchor.getZ();
        for (int q = 0, i = 0; q + QUAD <= data.length; q += QUAD, i++) {
            float x0 = Float.MAX_VALUE, y0 = x0, z0 = x0, x1 = -Float.MAX_VALUE, y1 = x1, z1 = x1;
            for (int k = 0; k < 4; k++) {
                int v = q + k * VERTEX;
                x0 = Math.min(x0, data[v]);
                y0 = Math.min(y0, data[v + 1]);
                z0 = Math.min(z0, data[v + 2]);
                x1 = Math.max(x1, data[v]);
                y1 = Math.max(y1, data[v + 1]);
                z1 = Math.max(z1, data[v + 2]);
            }
            double cx = ax + (data[q] + data[q + 8] + data[q + 16] + data[q + 24]) * 0.25;
            double cy = ay + (data[q + 1] + data[q + 9] + data[q + 17] + data[q + 25]) * 0.25;
            double cz = az + (data[q + 2] + data[q + 10] + data[q + 18] + data[q + 26]) * 0.25;
            host[i] = host(level, air, ax + x0, ay + y0, az + z0, ax + x1, ay + y1, az + z1, cx, cy, cz);
        }
        return host;
    }

    private static long host(Level level, Map<Long, Boolean> air, double minX, double minY, double minZ,
                             double maxX, double maxY, double maxZ, double cx, double cy, double cz) {
        int sx = (int) Math.floor(cx) >> 4, sy = (int) Math.floor(cy) >> 4, sz = (int) Math.floor(cz) >> 4;
        long own = SectionPos.asLong(sx, sy, sz);
        if (!onlyAir(level, own, air)) return own;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if ((dx | dy | dz) == 0) continue;
                    long nb = SectionPos.asLong(sx + dx, sy + dy, sz + dz);
                    if (onlyAir(level, nb, air)) continue;
                    double ox = (sx + dx) << 4, oy = (sy + dy) << 4, oz = (sz + dz) << 4;
                    if (minX - ox >= RANGE_MIN && maxX - ox <= RANGE_MAX && minY - oy >= RANGE_MIN && maxY - oy <= RANGE_MAX
                            && minZ - oz >= RANGE_MIN && maxZ - oz <= RANGE_MAX) {
                        return nb;
                    }
                }
            }
        }
        return own;
    }

    private static boolean onlyAir(Level level, long section, Map<Long, Boolean> cache) {
        return cache.computeIfAbsent(section, k -> {
            int index = level.getSectionIndexFromSectionY(SectionPos.y(k));
            if (index < 0 || index >= level.getSectionsCount()) return true;
            LevelChunkSection s = level.getChunk(SectionPos.x(k), SectionPos.z(k)).getSection(index);
            return s == null || s.hasOnlyAir();
        });
    }

    private static void remove(Placement p) {
        if (p != null) dirty(p);
    }

    private static void dirty(Placement p) {
        if (p.empty()) return;
        var renderer = Minecraft.getInstance().levelRenderer;
        renderer.setBlocksDirty(p.minX, p.minY, p.minZ, p.maxX, p.maxY, p.maxZ);
        for (long s : p.sections) renderer.setSectionDirty(SectionPos.x(s), SectionPos.y(s), SectionPos.z(s));
    }

    public static void onSection(AddSectionGeometryEvent event) {
        if ((CHAINS.isEmpty() && TAPES.isEmpty()) || !enabled()) return;
        BlockPos origin = event.getSectionOrigin();
        List<Placement> hits = null;
        for (Chain c : CHAINS.values()) {
            if (!c.touches(origin)) continue;
            if (hits == null) hits = new ArrayList<>();
            hits.add(c);
        }
        for (Tape t : TAPES.values()) {
            if (t.empty() || !t.touches(origin)) continue;
            if (hits == null) hits = new ArrayList<>();
            hits.add(t);
        }
        if (hits == null) return;
        List<Placement> list = hits;
        event.addRenderer(ctx -> {
            RandomSource random = RandomSource.create(42L);
            for (Placement p : list) {
                p.paint(ctx, origin, random);
                if (p.delivered == 0) p.delivered = System.nanoTime();
            }
        });
    }

    private static final class Recorder implements VertexConsumer {
        float[] data = new float[VERTEX * 64];
        int count;
        private int base = -VERTEX;

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            if ((count + 1) * VERTEX > data.length) data = Arrays.copyOf(data, data.length * 2);
            base = count * VERTEX;
            count++;
            data[base] = x;
            data[base + 1] = y;
            data[base + 2] = z;
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            data[base + 3] = u;
            data[base + 4] = v;
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
            data[base + 5] = normalX;
            data[base + 6] = normalY;
            data[base + 7] = normalZ;
            return this;
        }
    }
}
